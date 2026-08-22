package com.snitrix.snitify.playback

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import android.app.PendingIntent
import com.snitrix.snitify.MainActivity
import com.snitrix.snitify.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class DownloadService : Service() {

    private val serviceJob = Job()
    private val serviceScope = CoroutineScope(Dispatchers.Main + serviceJob)

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    private var wakeLock: android.os.PowerManager.WakeLock? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_CANCEL_DOWNLOAD) {
            val songId = intent.getStringExtra(EXTRA_SONG_ID)
            if (songId != null) {
                DownloadManager.cancelDownload(this, songId)
            }
        }

        try {
            val pm = getSystemService(Context.POWER_SERVICE) as android.os.PowerManager
            if (wakeLock == null) {
                wakeLock = pm.newWakeLock(android.os.PowerManager.PARTIAL_WAKE_LOCK, "Snitify:DownloadServiceLock")
            }
            if (wakeLock?.isHeld == false) {
                wakeLock?.acquire(60 * 60 * 1000L) // 1 hour max
            }
        } catch (e: Exception) {
            timber.log.Timber.e(e, "Failed to acquire DownloadService WakeLock")
        }

        val openDownloadsIntent = Intent(this, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            putExtra("navigate_to_downloads", true)
        }
        val contentPendingIntent = PendingIntent.getActivity(
            this,
            0,
            openDownloadsIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Downloading Music")
            .setContentText("Downloading tracks in background...")
            .setSmallIcon(R.drawable.ic_download)
            .setContentIntent(contentPendingIntent)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                startForeground(
                    NOTIFICATION_ID,
                    notification,
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                        android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
                    } else {
                        0
                    }
                )
            } else {
                startForeground(NOTIFICATION_ID, notification)
            }
        } catch (e: Exception) {
            timber.log.Timber.e(e, "Failed to start foreground download service")
        }

        serviceScope.launch {
            DownloadManager.downloadStatus.collectLatest { statusMap ->
                val activeDownloads = statusMap.values.filter {
                    it.status == DownloadStatus.DOWNLOADING || it.status == DownloadStatus.PENDING
                }
                if (activeDownloads.isEmpty()) {
                    try {
                        if (wakeLock?.isHeld == true) {
                            wakeLock?.release()
                        }
                    } catch (e: Exception) {}
                    stopForeground(STOP_FOREGROUND_REMOVE)
                    stopSelf()
                } else {
                    val first = activeDownloads.first()
                    val text = if (activeDownloads.size > 1) {
                        "Downloading '${first.song?.title}' (+${activeDownloads.size - 1} more)"
                    } else {
                        "Downloading '${first.song?.title}' (${first.percent}%)"
                    }

                    val notifBuilder = NotificationCompat.Builder(this@DownloadService, CHANNEL_ID)
                        .setContentTitle("Downloading Music")
                        .setContentText(text)
                        .setSmallIcon(R.drawable.ic_download)
                        .setContentIntent(contentPendingIntent)
                        .setProgress(100, first.percent, first.percent == 0)
                        .setOngoing(true)
                        .setPriority(NotificationCompat.PRIORITY_LOW)

                    first.song?.id?.let { songId ->
                        val cancelIntent = Intent(this@DownloadService, DownloadService::class.java).apply {
                            action = ACTION_CANCEL_DOWNLOAD
                            putExtra(EXTRA_SONG_ID, songId)
                        }
                        val cancelPendingIntent = PendingIntent.getService(
                            this@DownloadService,
                            songId.hashCode(),
                            cancelIntent,
                            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
                        )
                        notifBuilder.addAction(
                            NotificationCompat.Action.Builder(
                                R.drawable.ic_close,
                                "Cancel",
                                cancelPendingIntent
                            ).build()
                        )
                    }

                    val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                    notificationManager.notify(NOTIFICATION_ID, notifBuilder.build())
                }
            }
        }

        return START_STICKY
    }

    override fun onDestroy() {
        try {
            if (wakeLock?.isHeld == true) {
                wakeLock?.release()
            }
        } catch (e: Exception) {}
        serviceJob.cancel()
        super.onDestroy()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Download Service Channel",
                NotificationManager.IMPORTANCE_LOW
            )
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }

    companion object {
        const val CHANNEL_ID = "metrolist_download_channel"
        const val NOTIFICATION_ID = 2001
        const val ACTION_CANCEL_DOWNLOAD = "com.snitrix.snitify.ACTION_CANCEL_DOWNLOAD"
        const val EXTRA_SONG_ID = "extra_song_id"

        fun startService(context: Context) {
            val intent = Intent(context, DownloadService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }
    }
}
