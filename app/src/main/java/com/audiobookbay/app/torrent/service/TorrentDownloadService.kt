package com.audiobookbay.app.torrent.service

import android.app.*
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import com.audiobookbay.app.R
import com.audiobookbay.app.di.IoDispatcher
import com.audiobookbay.app.domain.model.DownloadStatus
import com.audiobookbay.app.torrent.TorrentDownloadManagerImpl
import com.audiobookbay.app.ui.MainActivity
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.*
import java.util.Locale
import javax.inject.Inject

@AndroidEntryPoint
class TorrentDownloadService : Service() {
    @Inject lateinit var manager: TorrentDownloadManagerImpl
    @Inject @IoDispatcher lateinit var io: CoroutineDispatcher
    private var stoppingBecauseIdle = false
    private var lastStartId = 0
    private var observer: Job? = null
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    override fun onCreate() {
        super.onCreate()
        val notifications = getSystemService(NotificationManager::class.java)
        notifications.createNotificationChannel(NotificationChannel(CHANNEL_ID, getString(R.string.download_channel), NotificationManager.IMPORTANCE_LOW))
        ServiceCompat.startForeground(this, NOTIFICATION_ID, notification(getString(R.string.download_preparing)), ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC)
        observer = serviceScope.launch {
            // Allow the start command and its first database update to settle.
            delay(1500)
            manager.observeDownloads().collect { downloads ->
                val active = downloads.filter { it.status in setOf(DownloadStatus.QUEUED, DownloadStatus.CHECKING, DownloadStatus.DOWNLOADING, DownloadStatus.PUBLISHING) }
                if (active.isEmpty()) {
                    stoppingBecauseIdle = true
                    stopSelfResult(lastStartId)
                } else {
                    val speed = active.sumOf { it.downloadSpeedBytesPerSec } / 1024.0
                    notifications.notify(NOTIFICATION_ID, notification(getString(R.string.download_notification, active.size, String.format(Locale.getDefault(), "%.1f", speed))))
                }
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        lastStartId = startId
        stoppingBecauseIdle = false
        return START_NOT_STICKY
    }
    override fun onBind(intent: Intent?): IBinder? = null

    override fun onTimeout(startId: Int, fgsType: Int) {
        // Stop promptly; the manager owns cleanup independently of this service's lifetime.
        cleanupEngine()
        stopSelf()
    }

    override fun onDestroy() {
        observer?.cancel()
        serviceScope.cancel()
        cleanupEngine(onlyIfIdle = stoppingBecauseIdle)
        ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE)
        super.onDestroy()
    }

    private fun cleanupEngine(onlyIfIdle: Boolean = false) {
        CoroutineScope(SupervisorJob() + io).launch {
            try { if (onlyIfIdle) manager.shutdownIfIdle() else manager.shutdown() }
            catch (e: CancellationException) { throw e }
            catch (e: Exception) { timber.log.Timber.e(e, "Torrent service cleanup failed") }
        }
    }

    private fun notification(message: String): Notification = NotificationCompat.Builder(this, CHANNEL_ID)
        .setContentTitle(getString(R.string.app_name))
        .setContentText(message)
        .setSmallIcon(android.R.drawable.stat_sys_download)
        .setContentIntent(PendingIntent.getActivity(this, 0, Intent(this, MainActivity::class.java), PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT))
        .setOngoing(true)
        .setOnlyAlertOnce(true)
        .setPriority(NotificationCompat.PRIORITY_LOW)
        .build()

    companion object {
        const val CHANNEL_ID = "torrent_download_channel"
        const val NOTIFICATION_ID = 1001
        fun start(context: Context) { context.startForegroundService(Intent(context, TorrentDownloadService::class.java)) }
        fun stop(context: Context) { context.stopService(Intent(context, TorrentDownloadService::class.java)) }
    }
}
