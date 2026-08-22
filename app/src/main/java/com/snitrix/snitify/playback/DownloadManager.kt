package com.snitrix.snitify.playback

import android.content.Context
import android.net.ConnectivityManager
import com.metrolist.innertube.YouTube
import com.metrolist.innertube.strategy.ContentHints
import com.snitrix.snitify.constants.AudioQuality
import com.snitrix.snitify.data.db.DatabaseManager
import com.snitrix.snitify.data.model.Song
import com.snitrix.snitify.utils.YTPlayerUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import timber.log.Timber
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.TimeUnit

// ─── Public State Model ───────────────────────────────────────────────────────

enum class DownloadStatus { IDLE, PENDING, DOWNLOADING, COMPLETED, FAILED }

data class DownloadProgress(
    val status: DownloadStatus = DownloadStatus.IDLE,
    val percent: Int = 0,          // 0-100, only meaningful during DOWNLOADING
    val song: Song? = null          // the song being tracked
)

// ─── Manager ──────────────────────────────────────────────────────────────────

object DownloadManager {
    private const val TAG = "DownloadManager"

    // Dedicated scope so downloads survive ViewModel re-creation
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    // Direct high-speed OkHttpClient without proxy overhead for raw googlevideo audio streams
    private val downloadHttp = OkHttpClient.Builder()
        .connectionPool(okhttp3.ConnectionPool(8, 5, java.util.concurrent.TimeUnit.MINUTES))
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(120, TimeUnit.SECONDS)
        .build()

    private val activeJobs = java.util.concurrent.ConcurrentHashMap<String, kotlinx.coroutines.Job>()
    private val activeCalls = java.util.concurrent.ConcurrentHashMap<String, okhttp3.Call>()

    /** Map of songId → DownloadProgress, emitted to the UI */
    private val _status = MutableStateFlow<Map<String, DownloadProgress>>(emptyMap())
    val downloadStatus: StateFlow<Map<String, DownloadProgress>> = _status.asStateFlow()

    // ── Public API ────────────────────────────────────────────────────────────

    fun resumeInterruptedDownloads(context: Context) {
        scope.launch {
            try {
                DatabaseManager.init(context)
                val allDownloads = DatabaseManager.getAllDownloads()
                val dir = File(context.filesDir, "downloads")

                allDownloads.forEach { song ->
                    val trackFile = File(dir, "${song.id}.m4a")
                    val tmpFile = File(dir, "${song.id}.m4a.tmp")

                    if (!trackFile.exists() || trackFile.length() == 0L || tmpFile.exists()) {
                        Timber.tag(TAG).i("Resuming interrupted download for ${song.title}")
                        downloadTrack(context, song)
                    }
                }
            } catch (e: Exception) {
                Timber.tag(TAG).e(e, "Error resuming interrupted downloads")
            }
        }
    }

    fun downloadTrack(context: Context, song: Song) {
        if (_status.value[song.id]?.status == DownloadStatus.DOWNLOADING) return

        // 0. Start Foreground Service so download survives backgrounding
        DownloadService.startService(context)

        // 1. Mark as PENDING immediately → appears in Downloads list at 0%
        updateStatus(song.id, DownloadProgress(DownloadStatus.PENDING, 0, song))

        // 2. Insert a placeholder DB row right now — no file paths yet,
        //    using an empty string so the row exists for the UI to find.
        DatabaseManager.init(context)
        DatabaseManager.insertDownload(
            id = song.id,
            title = song.title,
            artist = song.artist,
            album = song.album,
            duration = song.duration,
            coverUrl = song.coverUrl,
            localPath = "",            // filled in after download completes
            coverLocalPath = song.coverUrl
        )

        val job = scope.launch {
            try {
                doDownload(context, song)
            } finally {
                activeJobs.remove(song.id)
                activeCalls.remove(song.id)
            }
        }
        activeJobs[song.id] = job
    }

    fun cancelDownload(context: Context, songId: String) {
        activeCalls.remove(songId)?.cancel()
        activeJobs.remove(songId)?.cancel()

        DatabaseManager.init(context)
        val dir = File(context.filesDir, "downloads")
        File(dir, "${songId}.m4a.tmp").delete()
        File(dir, "${songId}.m4a").delete()
        File(dir, "${songId}_cover.jpg").delete()
        DatabaseManager.deleteDownload(songId)

        val updated = _status.value.toMutableMap()
        updated.remove(songId)
        _status.value = updated
        Timber.tag(TAG).i("Download cancelled and evicted for $songId")
    }

    fun deleteDownloadedTrack(context: Context, songId: String) {
        cancelDownload(context, songId)
    }

    // ── Private Download Logic ────────────────────────────────────────────────

    private suspend fun doDownload(context: Context, song: Song) {
        val startTime = System.currentTimeMillis()
        try {
            // ── Step 1: Resolve stream URL ──────────────────────────────────
            updateStatus(song.id, DownloadProgress(DownloadStatus.DOWNLOADING, 0, song))

            val connectivityManager =
                context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

            val playbackData = YTPlayerUtils.playerResponseForPlayback(
                videoId = song.id,
                audioQuality = AudioQuality.HIGH,
                connectivityManager = connectivityManager,
                contentHints = ContentHints()
            ).getOrNull()

            if (playbackData == null) {
                Timber.tag(TAG).e("Could not resolve stream URL for ${song.id}")
                markFailed(song)
                return
            }

            val streamUrl = playbackData.streamUrl
            Timber.tag(TAG).d("Stream URL resolved, starting download for ${song.id}")

            // ── Step 2: Prepare output files ────────────────────────────────
            val dir = File(context.filesDir, "downloads").also { it.mkdirs() }
            val trackFile = File(dir, "${song.id}.m4a")
            val coverFile = File(dir, "${song.id}_cover.jpg")

            // ── Step 3: Download audio with multi-threaded parallel Range requests ───────
            val audioOk = downloadParallelChunked(
                songId = song.id,
                url = streamUrl,
                destFile = trackFile,
                cookie = YouTube.cookie,
                onProgress = { pct ->
                    // Audio is 0-95% of UX progress
                    updateStatus(song.id, DownloadProgress(DownloadStatus.DOWNLOADING, (pct * 0.95).toInt(), song))
                }
            )
            if (!audioOk) {
                Timber.tag(TAG).e("Audio download failed for ${song.id}")
                markFailed(song)
                return
            }

            // ── Step 4: Download cover art ──────────────────────────────────
            updateStatus(song.id, DownloadProgress(DownloadStatus.DOWNLOADING, 96, song))
            downloadSimple(song.coverUrl, coverFile)

            // ── Step 5: Update DB with real file paths ──────────────────────
            updateStatus(song.id, DownloadProgress(DownloadStatus.DOWNLOADING, 99, song))
            DatabaseManager.insertDownload(
                id = song.id,
                title = song.title,
                artist = song.artist,
                album = song.album,
                duration = song.duration,
                coverUrl = song.coverUrl,
                localPath = trackFile.absolutePath,
                coverLocalPath = if (coverFile.exists()) coverFile.absolutePath else song.coverUrl
            )

            // ── Done ────────────────────────────────────────────────────────
            updateStatus(song.id, DownloadProgress(DownloadStatus.COMPLETED, 100, song))
            Timber.tag(TAG).i("Download complete: ${song.title}")

        } catch (e: Exception) {
            if (e is kotlinx.coroutines.CancellationException) {
                Timber.tag(TAG).i("Download job cancelled for ${song.id}")
                throw e
            }
            Timber.tag(TAG).e(e, "Download exception for ${song.id}")
            markFailed(song)
        }
    }

    /**
     * High-speed parallel chunked download using 12 concurrent HTTP Range requests.
     * Bypasses YouTube googlevideo CDN single-connection bandwidth throttling.
     */
    private suspend fun downloadParallelChunked(
        songId: String,
        url: String,
        destFile: File,
        cookie: String?,
        onProgress: (Int) -> Unit
    ): Boolean = kotlinx.coroutines.coroutineScope {
        val tmpFile = File(destFile.parentFile, "${destFile.name}.tmp")
        try {
            // Step A: Determine total file size via lightweight Range 0-1 request
            val testReq = Request.Builder()
                .url(url)
                .addHeader("Range", "bytes=0-1")
                .apply { cookie?.let { addHeader("Cookie", it) } }
                .addHeader("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/125.0.0.0 Safari/537.36")
                .build()

            val testCall = downloadHttp.newCall(testReq)
            activeCalls[songId] = testCall
            val testResp = testCall.execute()

            var totalBytes = -1L
            if (testResp.code == 206) {
                val cr = testResp.header("Content-Range")
                if (cr != null && cr.contains("/")) {
                    totalBytes = cr.substringAfter("/").trim().toLongOrNull() ?: -1L
                }
            }
            testResp.close()

            if (totalBytes <= 0L || totalBytes < 500_000L) {
                // Fallback to single-connection download if server doesn't report range size or file is small
                return@coroutineScope downloadWithProgress(songId, url, destFile, cookie, onProgress)
            }

            // Step B: Divide into 12 parallel byte ranges
            val chunkCount = 12
            val chunkSize = totalBytes / chunkCount

            val raf = java.io.RandomAccessFile(tmpFile, "rw")
            raf.setLength(totalBytes)

            val downloadedBytes = java.util.concurrent.atomic.AtomicLong(0L)
            var lastPct = -1

            val jobs = (0 until chunkCount).map { i ->
                async(Dispatchers.IO) {
                    val start = i * chunkSize
                    val end = if (i == chunkCount - 1) totalBytes - 1 else (i + 1) * chunkSize - 1

                    val req = Request.Builder()
                        .url(url)
                        .addHeader("Range", "bytes=$start-$end")
                        .apply { cookie?.let { addHeader("Cookie", it) } }
                        .addHeader("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/125.0.0.0 Safari/537.36")
                        .build()

                    val call = downloadHttp.newCall(req)
                    val resp = call.execute()

                    if (!resp.isSuccessful && resp.code != 206) {
                        resp.close()
                        return@async false
                    }

                    val stream = resp.body?.byteStream() ?: run {
                        resp.close()
                        return@async false
                    }

                    val buf = ByteArray(64 * 1024)
                    var written = 0L
                    stream.use { input ->
                        var read: Int
                        while (input.read(buf).also { read = it } != -1) {
                            synchronized(raf) {
                                raf.seek(start + written)
                                raf.write(buf, 0, read)
                            }
                            written += read
                            val currentTotal = downloadedBytes.addAndGet(read.toLong())
                            val pct = ((currentTotal.toFloat() / totalBytes) * 100).toInt().coerceIn(0, 100)
                            if (pct != lastPct) {
                                lastPct = pct
                                onProgress(pct)
                            }
                        }
                    }
                    resp.close()
                    true
                }
            }

            val results = kotlinx.coroutines.awaitAll(*jobs.toTypedArray())
            raf.close()

            val allOk = results.all { it == true }
            if (allOk) {
                if (destFile.exists()) destFile.delete()
                tmpFile.renameTo(destFile)
                true
            } else {
                tmpFile.delete()
                false
            }
        } catch (e: Exception) {
            Timber.tag(TAG).e(e, "Parallel chunked download failed for $songId, falling back...")
            if (tmpFile.exists()) tmpFile.delete()
            downloadWithProgress(songId, url, destFile, cookie, onProgress)
        }
    }

    /**
     * Download [url] to [destFile] with resume support and live % progress reporting.
     */
    private fun downloadWithProgress(
        songId: String,
        url: String,
        destFile: File,
        cookie: String?,
        onProgress: (Int) -> Unit
    ): Boolean {
        return try {
            val tmpFile = File(destFile.parentFile, "${destFile.name}.tmp")
            val existingLength = if (tmpFile.exists()) tmpFile.length() else 0L

            val reqBuilder = Request.Builder().url(url)
            cookie?.let { reqBuilder.addHeader("Cookie", it) }
            reqBuilder.addHeader("User-Agent",
                "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 " +
                "(KHTML, like Gecko) Chrome/125.0.0.0 Safari/537.36")
            reqBuilder.addHeader("Accept", "*/*")
            reqBuilder.addHeader("Accept-Encoding", "identity")
            reqBuilder.addHeader("Connection", "keep-alive")

            if (existingLength > 0) {
                reqBuilder.addHeader("Range", "bytes=$existingLength-")
            }

            val call = downloadHttp.newCall(reqBuilder.build())
            activeCalls[songId] = call
            val response = call.execute()
            if (!response.isSuccessful && response.code != 416) {
                Timber.tag(TAG).e("HTTP ${response.code} downloading $url")
                return false
            }

            val isPartial = response.code == 206
            val appendMode = isPartial && existingLength > 0
            val body = response.body ?: return false

            val contentLength = body.contentLength()
            val totalBytes = if (appendMode) existingLength + contentLength else contentLength

            var downloadedBytes = if (appendMode) existingLength else 0L
            var lastPct = -1

            body.byteStream().use { input ->
                FileOutputStream(tmpFile, appendMode).use { output ->
                    val buf = ByteArray(128 * 1024)
                    var bytesRead: Int
                    while (input.read(buf).also { bytesRead = it } != -1) {
                        output.write(buf, 0, bytesRead)
                        downloadedBytes += bytesRead
                        if (totalBytes > 0) {
                            val pct = ((downloadedBytes.toFloat() / totalBytes) * 100).toInt().coerceIn(0, 100)
                            if (pct != lastPct) {
                                lastPct = pct
                                onProgress(pct)
                            }
                        }
                    }
                    output.flush()
                }
            }

            if (destFile.exists()) destFile.delete()
            tmpFile.renameTo(destFile)
            true
        } catch (e: Exception) {
            Timber.tag(TAG).e(e, "downloadWithProgress failed")
            false
        }
    }

    /** Simple best-effort cover art download without progress reporting */
    private fun downloadSimple(url: String, dest: File) {
        try {
            val req = Request.Builder().url(url).build()
            val resp = downloadHttp.newCall(req).execute()
            resp.body?.byteStream()?.use { input ->
                FileOutputStream(dest).use { it.write(input.readBytes()) }
            }
        } catch (e: Exception) {
            Timber.tag(TAG).w(e, "Cover art download failed (non-fatal)")
        }
    }

    fun saveToPublicDownloads(context: Context, song: Song): Boolean {
        DatabaseManager.init(context)
        val localPath = DatabaseManager.getDownloadPath(song.id) ?: return false
        val sourceFile = File(localPath)
        if (!sourceFile.exists()) return false

        return try {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                val resolver = context.contentResolver
                val contentValues = android.content.ContentValues().apply {
                    put(android.provider.MediaStore.Downloads.DISPLAY_NAME, "${song.title} - ${song.artist}.m4a")
                    put(android.provider.MediaStore.Downloads.MIME_TYPE, "audio/mp4")
                    put(android.provider.MediaStore.Downloads.RELATIVE_PATH, android.os.Environment.DIRECTORY_DOWNLOADS)
                }

                val uri = resolver.insert(android.provider.MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues) ?: return false
                resolver.openOutputStream(uri)?.use { outputStream ->
                    java.io.FileInputStream(sourceFile).use { inputStream ->
                        inputStream.copyTo(outputStream)
                    }
                }
                true
            } else {
                val downloadsDir = android.os.Environment.getExternalStoragePublicDirectory(android.os.Environment.DIRECTORY_DOWNLOADS)
                if (!downloadsDir.exists()) {
                    downloadsDir.mkdirs()
                }
                val destFile = File(downloadsDir, "${song.title} - ${song.artist}.m4a")
                java.io.FileInputStream(sourceFile).use { inputStream ->
                    java.io.FileOutputStream(destFile).use { outputStream ->
                        inputStream.copyTo(outputStream)
                    }
                }
                true
            }
        } catch (e: Exception) {
            Timber.tag(TAG).e(e, "Failed to save to public downloads")
            false
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private fun updateStatus(id: String, progress: DownloadProgress) {
        _status.value = _status.value + (id to progress)
    }

    private fun markFailed(song: Song) {
        // Remove placeholder DB row so the song doesn't appear as broken
        DatabaseManager.deleteDownload(song.id)
        updateStatus(song.id, DownloadProgress(DownloadStatus.FAILED, 0, song))
    }
}
