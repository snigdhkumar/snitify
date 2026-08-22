package com.snitrix.snitify.playback

import android.content.Context
import android.net.ConnectivityManager
import android.net.Uri
import androidx.media3.database.StandaloneDatabaseProvider
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.datasource.cache.CacheDataSource
import androidx.media3.datasource.cache.CacheWriter
import androidx.media3.datasource.cache.LeastRecentlyUsedCacheEvictor
import androidx.media3.datasource.cache.SimpleCache
import com.metrolist.innertube.strategy.ContentHints
import com.snitrix.snitify.constants.AudioQuality
import com.snitrix.snitify.data.model.Song
import com.snitrix.snitify.utils.YTPlayerUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import timber.log.Timber
import java.io.File

object CacheManager {
    private const val TAG = "CacheManager"
    private const val CACHE_SIZE = 300 * 1024 * 1024L // 300MB LRU Cache limit

    @Volatile
    private var simpleCache: SimpleCache? = null

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    @Synchronized
    fun getCache(context: Context): SimpleCache {
        return simpleCache ?: run {
            val cacheDir = File(context.cacheDir, "exoplayer_cache").also { it.mkdirs() }
            val evictor = LeastRecentlyUsedCacheEvictor(CACHE_SIZE)
            val databaseProvider = StandaloneDatabaseProvider(context)
            SimpleCache(cacheDir, evictor, databaseProvider).also {
                simpleCache = it
            }
        }
    }

    fun getCacheDataSourceFactory(context: Context): CacheDataSource.Factory {
        val cache = getCache(context)
        val httpFactory = DefaultHttpDataSource.Factory()
            .setAllowCrossProtocolRedirects(true)
            .setUserAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64)")
        val defaultDataSourceFactory = androidx.media3.datasource.DefaultDataSource.Factory(context, httpFactory)
        return CacheDataSource.Factory()
            .setCache(cache)
            .setUpstreamDataSourceFactory(defaultDataSourceFactory)
            .setFlags(CacheDataSource.FLAG_IGNORE_CACHE_ON_ERROR)
    }

    fun preCacheQueue(context: Context, queue: List<Song>, currentIndex: Int) {
        if (queue.isEmpty()) return
        scope.launch {
            val nextIndex1 = (currentIndex + 1) % queue.size
            val nextIndex2 = (currentIndex + 2) % queue.size

            val indicesToPreCache = listOf(nextIndex1, nextIndex2).distinct()

            indicesToPreCache.forEach { index ->
                val song = queue.getOrNull(index) ?: return@forEach
                preCacheTrack(context, song)
            }
        }
    }

    private suspend fun preCacheTrack(context: Context, song: Song) {
        // If track is a local device file or content URI or downloaded, it's already local!
        val isLocal = song.id.startsWith("content://") ||
                song.id.startsWith("/") ||
                song.id.toLongOrNull() != null ||
                (try { File(song.id).exists() } catch (e: Exception) { false })

        if (isLocal) return

        try {
            val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            val playbackData = YTPlayerUtils.playerResponseForPlayback(
                videoId = song.id,
                audioQuality = AudioQuality.HIGH,
                connectivityManager = connectivityManager,
                contentHints = ContentHints()
            ).getOrNull()

            val streamUrl = playbackData?.streamUrl ?: return
            val cache = getCache(context)
            val dataSource = CacheDataSource.Factory()
                .setCache(cache)
                .setUpstreamDataSourceFactory(DefaultHttpDataSource.Factory().setAllowCrossProtocolRedirects(true))
                .createDataSource()

            val dataSpec = androidx.media3.datasource.DataSpec(Uri.parse(streamUrl), 0, 1024 * 1024L) // Pre-buffer 1MB head
            val writer = CacheWriter(dataSource, dataSpec, null, null)
            writer.cache()
            Timber.tag(TAG).d("Pre-cached head bytes for song: ${song.title}")
        } catch (e: Exception) {
            Timber.tag(TAG).w(e, "Pre-cache failed for ${song.title} (non-fatal)")
        }
    }
}
