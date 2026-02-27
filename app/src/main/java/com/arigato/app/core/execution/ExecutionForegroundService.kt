package com.arigato.app.core.execution

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.arigato.app.R
import com.arigato.app.ui.MainActivity
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class ExecutionForegroundService : Service() {
    @Inject
    lateinit var processManager: ProcessManager

    companion object {
        const val CHANNEL_ID = "execution_channel"
        const val NOTIFICATION_ID = 1001
        const val ACTION_STOP_ALL = "com.arigato.app.STOP_ALL"
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP_ALL) {
            stopSelf()
            return START_NOT_STICKY
        }
        startForeground(NOTIFICATION_ID, buildNotification())
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Tool Execution",
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "Shows when security tools are running"
        }
        getSystemService(NotificationManager::class.java)
            .createNotificationChannel(channel)
    }

    private fun buildNotification(): Notification {
        val mainIntent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this, 0, mainIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val stopIntent = Intent(this, ExecutionForegroundService::class.java).apply {
            action = ACTION_STOP_ALL
        }
        val stopPending = PendingIntent.getService(
            this, 0, stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val activeCount = processManager.getActiveProcesses().size
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Arigato - Running Tools")
            .setContentText("$activeCount tool(s) currently executing")
            .setSmallIcon(android.R.drawable.ic_media_play)
            .setContentIntent(pendingIntent)
            .addAction(android.R.drawable.ic_media_pause, "Stop All", stopPending)
            .setOngoing(true)
            .build()
    }
}
