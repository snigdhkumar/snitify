package com.snitrix.snitify.playback

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.CommandButton
import androidx.media3.session.DefaultMediaNotificationProvider
import androidx.media3.session.MediaNotification
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import com.google.common.collect.ImmutableList
import com.snitrix.snitify.MainActivity
import com.snitrix.snitify.R
import timber.log.Timber

@UnstableApi
class PlaybackService : MediaSessionService() {
    private var controllerFuture: com.google.common.util.concurrent.ListenableFuture<androidx.media3.session.MediaController>? = null

    override fun onCreate() {
        super.onCreate()

        // Metrolist Pattern: Instantly promote service to foreground on onCreate()
        // before any ExoPlayer setup or I/O to avoid 5-second OS ANR/kill on OEM ROMs (Realme, Oppo, Vivo, Xiaomi)
        ensureForegroundChannelExists()
        startForegroundSafely(createFallbackForegroundNotification())

        val defaultProvider = DefaultMediaNotificationProvider.Builder(this)
            .setNotificationId(NOTIFICATION_ID)
            .setChannelId(CHANNEL_ID)
            .build()

        setMediaNotificationProvider(defaultProvider)

        // Self-connected MediaController (Metrolist Pattern) ensures active system notification
        try {
            val sessionToken = androidx.media3.session.SessionToken(
                this,
                android.content.ComponentName(this, PlaybackService::class.java)
            )
            controllerFuture = androidx.media3.session.MediaController.Builder(this, sessionToken).buildAsync()
        } catch (e: Exception) {
            Timber.tag(TAG).e(e, "Failed to initialize internal MediaController")
        }
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? {
        return PlaybackManager.mediaSession
    }

    override fun onDestroy() {
        controllerFuture?.let {
            androidx.media3.session.MediaController.releaseFuture(it)
        }
        super.onDestroy()
    }

    private fun ensureForegroundChannelExists() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Metrolist Playback",
                NotificationManager.IMPORTANCE_LOW
            )
            nm.createNotificationChannel(channel)
        }
    }

    private fun createFallbackForegroundNotification(): Notification {
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Playing Music")
            .setContentText("Snitify playback active")
            .setSmallIcon(R.drawable.ic_download)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    private fun startForegroundSafely(notification: Notification): Boolean {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                startForeground(
                    NOTIFICATION_ID,
                    notification,
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK
                )
            } else {
                startForeground(NOTIFICATION_ID, notification)
            }
            true
        } catch (e: Exception) {
            Timber.tag(TAG).e(e, "Failed to start foreground playback service")
            false
        }
    }

    companion object {
        private const val TAG = "PlaybackService"
        const val CHANNEL_ID = "metrolist_playback_channel"
        const val NOTIFICATION_ID = 1001
    }
}
