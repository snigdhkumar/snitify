package com.snitrix.snitify.playback

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.Uri
import android.os.Build
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaSession
import com.metrolist.innertube.strategy.ContentHints
import com.snitrix.snitify.MainActivity
import com.snitrix.snitify.constants.AudioQuality
import com.snitrix.snitify.data.db.DatabaseManager
import com.snitrix.snitify.data.model.Song
import com.snitrix.snitify.utils.YTPlayerUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber

object PlaybackManager {
    private const val TAG = "PlaybackManager"

    private var exoPlayer: ExoPlayer? = null
    var mediaSession: MediaSession? = null
        private set

    private val scope = CoroutineScope(Dispatchers.Main)
    private var progressJob: Job? = null
    private var playJob: Job? = null
    private var isResolvingUrl = false
    private var appContext: Context? = null

    private var stateChangeListener: ((isPlaying: Boolean, progressMs: Long, durationMs: Long, isLoading: Boolean) -> Unit)? = null

    @Volatile var notifTitle: String = ""
    @Volatile var notifArtist: String = ""
    @Volatile var notifArtUrl: String = ""

    private var trackEndedListener: (() -> Unit)? = null
    private var nextCallback: (() -> Unit)? = null
    private var prevCallback: (() -> Unit)? = null

    fun setOnTrackEndedListener(listener: () -> Unit) {
        trackEndedListener = listener
    }

    fun setOnNextRequestedListener(listener: () -> Unit) {
        nextCallback = listener
    }

    fun setOnPrevRequestedListener(listener: () -> Unit) {
        prevCallback = listener
    }

    private val sessionCallback = object : MediaSession.Callback {
        override fun onConnect(
            session: MediaSession,
            controller: MediaSession.ControllerInfo
        ): MediaSession.ConnectionResult {
            val sessionCommands = MediaSession.ConnectionResult.DEFAULT_SESSION_COMMANDS.buildUpon()
                .build()
            val playerCommands = MediaSession.ConnectionResult.DEFAULT_PLAYER_COMMANDS.buildUpon()
                .add(Player.COMMAND_SEEK_TO_NEXT)
                .add(Player.COMMAND_SEEK_TO_NEXT_MEDIA_ITEM)
                .add(Player.COMMAND_SEEK_TO_PREVIOUS)
                .add(Player.COMMAND_SEEK_TO_PREVIOUS_MEDIA_ITEM)
                .add(Player.COMMAND_PLAY_PAUSE)
                .add(Player.COMMAND_SEEK_IN_CURRENT_MEDIA_ITEM)
                .build()
            return MediaSession.ConnectionResult.AcceptedResultBuilder(session)
                .setAvailableSessionCommands(sessionCommands)
                .setAvailablePlayerCommands(playerCommands)
                .build()
        }

        override fun onPlayerCommandRequest(
            session: MediaSession,
            controller: MediaSession.ControllerInfo,
            playerCommand: Int
        ): Int {
            when (playerCommand) {
                Player.COMMAND_SEEK_TO_NEXT, Player.COMMAND_SEEK_TO_NEXT_MEDIA_ITEM -> {
                    scope.launch(Dispatchers.Main) {
                        nextCallback?.invoke()
                    }
                }
                Player.COMMAND_SEEK_TO_PREVIOUS, Player.COMMAND_SEEK_TO_PREVIOUS_MEDIA_ITEM -> {
                    scope.launch(Dispatchers.Main) {
                        prevCallback?.invoke()
                    }
                }
            }
            return super.onPlayerCommandRequest(session, controller, playerCommand)
        }
    }

    private var wakeLock: android.os.PowerManager.WakeLock? = null
    private val streamUrlCache = java.util.concurrent.ConcurrentHashMap<String, String>()

    private fun acquireWakeLock(context: Context) {
        try {
            if (wakeLock == null) {
                val pm = context.getSystemService(Context.POWER_SERVICE) as android.os.PowerManager
                wakeLock = pm.newWakeLock(android.os.PowerManager.PARTIAL_WAKE_LOCK, "Snitify:PlaybackStreamLock")
            }
            if (wakeLock?.isHeld == false) {
                wakeLock?.acquire(30000L) // 30s timeout
            }
        } catch (e: Exception) {
            Timber.tag(TAG).e(e, "Failed to acquire wake lock")
        }
    }

    private fun releaseWakeLock() {
        try {
            if (wakeLock?.isHeld == true) {
                wakeLock?.release()
            }
        } catch (e: Exception) {
            Timber.tag(TAG).e(e, "Failed to release wake lock")
        }
    }

    fun prefetchNextStreamUrl(context: Context, videoId: String) {
        if (streamUrlCache.containsKey(videoId)) return
        val sandboxPath = DatabaseManager.getDownloadPath(videoId)
        if (sandboxPath != null && java.io.File(sandboxPath).exists()) return

        scope.launch(Dispatchers.IO) {
            try {
                val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
                val playbackData = YTPlayerUtils.playerResponseForPlayback(
                    videoId = videoId,
                    audioQuality = AudioQuality.HIGH,
                    connectivityManager = connectivityManager,
                    contentHints = ContentHints()
                ).getOrNull()
                playbackData?.streamUrl?.let { url ->
                    streamUrlCache[videoId] = url
                }
            } catch (e: Exception) {
                Timber.tag(TAG).e(e, "Prefetch failed for $videoId")
            }
        }
    }

    private fun startPlaybackService() {
        val ctx = appContext ?: return
        val intent = Intent(ctx, PlaybackService::class.java)
        try {
            androidx.core.content.ContextCompat.startForegroundService(ctx, intent)
        } catch (e: Exception) {
            Timber.tag(TAG).e(e, "Failed to start PlaybackService")
        }
    }

    private fun stopPlaybackService() {
        val ctx = appContext ?: return
        val intent = Intent(ctx, PlaybackService::class.java)
        ctx.stopService(intent)
    }

    fun init(context: Context) {
        if (appContext == null) {
            appContext = context.applicationContext
        }
        if (exoPlayer == null) {
            val audioAttributes = AudioAttributes.Builder()
                .setUsage(C.USAGE_MEDIA)
                .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
                .build()

            val cacheFactory = CacheManager.getCacheDataSourceFactory(context.applicationContext)
            val mediaSourceFactory = androidx.media3.exoplayer.source.DefaultMediaSourceFactory(cacheFactory)

            val player = ExoPlayer.Builder(context.applicationContext)
                .setMediaSourceFactory(mediaSourceFactory)
                .build().apply {
                repeatMode = Player.REPEAT_MODE_OFF
                playWhenReady = true
                setAudioAttributes(audioAttributes, true)
                setWakeMode(C.WAKE_MODE_LOCAL)
                addListener(object : Player.Listener {
                    override fun onIsPlayingChanged(isPlaying: Boolean) {
                        notifyStateChange()
                        if (isPlaying) {
                            startProgressPolling()
                            startPlaybackService()
                        } else {
                            stopProgressPolling()
                            if (playbackState == Player.STATE_IDLE) {
                                stopPlaybackService()
                            }
                        }
                    }

                    override fun onPlaybackStateChanged(playbackState: Int) {
                        notifyStateChange()
                        if (playbackState == Player.STATE_ENDED) {
                            stateChangeListener?.invoke(false, duration, duration, false)
                            scope.launch(Dispatchers.Main) {
                                trackEndedListener?.invoke()
                            }
                        }
                    }

                    override fun onPlayerError(error: androidx.media3.common.PlaybackException) {
                        Timber.tag(TAG).e(error, "ExoPlayer error: ${error.message}")
                        scope.launch(Dispatchers.Main) {
                            trackEndedListener?.invoke()
                        }
                    }
                })
            }
            exoPlayer = player

            val intent = Intent(context.applicationContext, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            val pendingIntent = PendingIntent.getActivity(
                context.applicationContext, 0, intent,
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            )

            val forwardingPlayer = object : androidx.media3.common.ForwardingPlayer(player) {
                override fun isCommandAvailable(command: Int): Boolean {
                    if (command == Player.COMMAND_SEEK_TO_NEXT || command == Player.COMMAND_SEEK_TO_PREVIOUS) {
                        return true
                    }
                    return super.isCommandAvailable(command)
                }

                override fun getAvailableCommands(): Player.Commands {
                    return super.getAvailableCommands().buildUpon()
                        .add(Player.COMMAND_SEEK_TO_NEXT)
                        .add(Player.COMMAND_SEEK_TO_PREVIOUS)
                        .build()
                }
            }

            mediaSession = MediaSession.Builder(context.applicationContext, forwardingPlayer)
                .setSessionActivity(pendingIntent)
                .setCallback(sessionCallback)
                .build()
        }
    }

    fun updateNotificationMetadata(title: String, artist: String, artUrl: String) {
        notifTitle = title
        notifArtist = artist
        notifArtUrl = artUrl
    }

    fun setOnStateChangeListener(listener: (isPlaying: Boolean, progressMs: Long, durationMs: Long, isLoading: Boolean) -> Unit) {
        stateChangeListener = listener
    }

    private fun createMediaItem(videoId: String, localPath: String?, title: String?, artist: String?, artUrl: String?, streamUri: Uri? = null): MediaItem {
        val t = title ?: notifTitle.ifEmpty { "Metrolist" }
        val a = artist ?: notifArtist.ifEmpty { "" }
        val u = artUrl ?: notifArtUrl

        val defaultUri = Uri.parse("android.resource://" + (appContext?.packageName ?: "com.snitrix.snitify") + "/" + com.snitrix.snitify.R.drawable.defaultthumbnail)

        val artUri = when {
            u.startsWith("http") || u.startsWith("content://") || u.startsWith("file://") -> Uri.parse(u)
            u.startsWith("/") -> Uri.fromFile(java.io.File(u))
            else -> defaultUri
        }

        val metadataBuilder = MediaMetadata.Builder()
            .setTitle(t)
            .setArtist(a)
            .setArtworkUri(artUri)

        val uri = when {
            streamUri != null -> streamUri
            localPath != null && (localPath.startsWith("/") || localPath.startsWith("content://") || java.io.File(localPath).exists()) -> {
                if (localPath.startsWith("content://")) Uri.parse(localPath) else Uri.fromFile(java.io.File(localPath))
            }
            else -> {
                val sandboxPath = DatabaseManager.getDownloadPath(videoId)
                if (sandboxPath != null && java.io.File(sandboxPath).exists()) {
                    Uri.fromFile(java.io.File(sandboxPath))
                } else {
                    Uri.EMPTY
                }
            }
        }

        return MediaItem.Builder()
            .setMediaId(videoId)
            .setUri(uri)
            .setMediaMetadata(metadataBuilder.build())
            .build()
    }

    fun updateQueue(context: Context, queue: List<Song>, currentIndex: Int) {
        init(context)
    }

    private fun playMediaItem(item: MediaItem) {
        val player = exoPlayer ?: return
        player.setMediaItem(item)
        player.prepare()
        player.play()
    }

    fun play(context: Context, videoId: String, localPath: String? = null, title: String? = null, artist: String? = null, artUrl: String? = null) {
        init(context)
        DatabaseManager.init(context)
        startPlaybackService()

        if (title != null) notifTitle = title
        if (artist != null) notifArtist = artist
        if (artUrl != null) notifArtUrl = artUrl

        // Stop current audio immediately and notify state
        exoPlayer?.stop()

        // Check if explicit local path is provided (for device media)
        if (localPath != null && (localPath.startsWith("/") || localPath.startsWith("content://") || java.io.File(localPath).exists())) {
            isResolvingUrl = false
            notifyStateChange()
            playJob?.cancel()
            val uri = if (localPath.startsWith("content://")) Uri.parse(localPath) else Uri.fromFile(java.io.File(localPath))
            val item = createMediaItem(videoId, localPath, title, artist, artUrl, uri)
            playMediaItem(item)
            return
        }

        // Check if track is downloaded locally in sandbox
        val sandboxPath = DatabaseManager.getDownloadPath(videoId)
        if (sandboxPath != null && java.io.File(sandboxPath).exists()) {
            isResolvingUrl = false
            notifyStateChange()
            playJob?.cancel()
            val uri = Uri.fromFile(java.io.File(sandboxPath))
            val item = createMediaItem(videoId, localPath, title, artist, artUrl, uri)
            playMediaItem(item)
            return
        }

        // Check pre-fetched stream URL cache
        val cachedStreamUrl = streamUrlCache.remove(videoId)
        if (cachedStreamUrl != null) {
            isResolvingUrl = false
            notifyStateChange()
            playJob?.cancel()
            val item = createMediaItem(videoId, localPath, title, artist, artUrl, Uri.parse(cachedStreamUrl))
            playMediaItem(item)
            return
        }

        isResolvingUrl = true
        notifyStateChange()

        // Abort previous loading coroutine
        playJob?.cancel()

        playJob = scope.launch {
            acquireWakeLock(context)
            val streamUrl = withContext(Dispatchers.IO) {
                try {
                    var resolvedId = videoId
                    var resolvedArtUrl = artUrl

                    // If videoId is an imported non-YouTube ID (e.g. spotify:track:xxx or import_xxx), resolve via YouTube search
                    if (resolvedId.contains("spotify") || resolvedId.contains("import") || resolvedId.contains(":") || resolvedId.length != 11) {
                        val searchQuery = "${title ?: ""} ${artist ?: ""}".trim()
                        if (searchQuery.isNotBlank()) {
                            try {
                                val searchRes = com.metrolist.innertube.YouTube.search(searchQuery, com.metrolist.innertube.YouTube.SearchFilter.FILTER_SONG).getOrNull()
                                val matchedSong = searchRes?.items?.filterIsInstance<com.metrolist.innertube.models.SongItem>()?.firstOrNull()
                                if (matchedSong != null) {
                                    val realId = matchedSong.id
                                    val realCover = matchedSong.thumbnail ?: ""
                                    DatabaseManager.updateImportedSongIdAndCover(videoId, realId, realCover)
                                    resolvedId = realId
                                    if (resolvedArtUrl.isNullOrEmpty() && realCover.isNotEmpty()) {
                                        resolvedArtUrl = realCover
                                        notifArtUrl = realCover
                                    }
                                }
                            } catch (e: Exception) {
                                Timber.tag(TAG).e(e, "Error matching imported song on YouTube")
                            }
                        }
                    }

                    val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
                    val playbackData = YTPlayerUtils.playerResponseForPlayback(
                        videoId = resolvedId,
                        audioQuality = AudioQuality.HIGH,
                        connectivityManager = connectivityManager,
                        contentHints = ContentHints()
                    ).getOrThrow()

                    playbackData.streamUrl
                } catch (e: Exception) {
                    Timber.tag(TAG).e(e, "Failed to resolve stream URL for playback")
                    null
                }
            }

            releaseWakeLock()
            isResolvingUrl = false
            if (streamUrl != null) {
                val item = createMediaItem(videoId, localPath, title, artist, notifArtUrl, Uri.parse(streamUrl))
                playMediaItem(item)
            } else {
                exoPlayer?.stop()
                exoPlayer?.clearMediaItems()
                notifyStateChange()
                withContext(Dispatchers.Main) {
                    android.widget.Toast.makeText(context, "Playback failed. Please check your internet connection.", android.widget.Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    fun pause() {
        exoPlayer?.pause()
    }

    fun resume() {
        exoPlayer?.play()
    }

    fun seekTo(positionMs: Long) {
        exoPlayer?.seekTo(positionMs)
    }

    fun isPlaying(): Boolean {
        return exoPlayer?.isPlaying == true
    }

    fun getDuration(): Long {
        return exoPlayer?.duration?.coerceAtLeast(0) ?: 0
    }

    fun getCurrentPosition(): Long {
        return exoPlayer?.currentPosition?.coerceAtLeast(0) ?: 0
    }

    private fun startProgressPolling() {
        progressJob?.cancel()
        progressJob = scope.launch {
            while (true) {
                notifyStateChange()
                delay(500)
            }
        }
    }

    private fun stopProgressPolling() {
        progressJob?.cancel()
        progressJob = null
        notifyStateChange()
    }

    private fun notifyStateChange() {
        val isPlaying = exoPlayer?.isPlaying == true
        val duration = exoPlayer?.duration?.coerceAtLeast(0) ?: 0
        val position = exoPlayer?.currentPosition?.coerceAtLeast(0) ?: 0
        val isLocalFile = exoPlayer?.currentMediaItem?.localConfiguration?.uri?.scheme == "file"
        val isBuffering = exoPlayer?.playbackState == Player.STATE_BUFFERING
        val isLoading = isResolvingUrl || (isBuffering && !isLocalFile)
        stateChangeListener?.invoke(isPlaying, position, duration, isLoading)
    }

    fun fadeAndStop(onComplete: () -> Unit) {
        scope.launch {
            val player = exoPlayer ?: run {
                onComplete()
                return@launch
            }
            val startVolume = player.volume
            val steps = 10
            val delayPerStep = 35L
            for (i in steps downTo 0) {
                player.volume = startVolume * (i.toFloat() / steps)
                delay(delayPerStep)
            }
            stopPlaybackAndService()
            player.volume = startVolume
            onComplete()
        }
    }

    fun stopPlaybackAndService() {
        playJob?.cancel()
        playJob = null
        isResolvingUrl = false
        releaseWakeLock()
        exoPlayer?.stop()
        exoPlayer?.clearMediaItems()
        stopPlaybackService()
        notifyStateChange()
    }

    fun release() {
        progressJob?.cancel()
        stopPlaybackService()
        mediaSession?.release()
        mediaSession = null
        exoPlayer?.release()
        exoPlayer = null
    }
}
