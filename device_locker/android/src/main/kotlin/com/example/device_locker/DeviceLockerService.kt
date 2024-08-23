package com.example.device_locker

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.IBinder
import androidx.core.app.NotificationCompat

class DeviceLockerService : Service() {

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        createNotificationChannel()
        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Device Locker")
            .setContentText("Performing device management tasks")
            .setSmallIcon(R.drawable.ic_lock) // Replace with your app's icon
            .build()

        startForeground(NOTIFICATION_ID, notification)

        // Perform your background work here

        stopSelf() // Stop the service once the work is done
        return START_NOT_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    private fun createNotificationChannel() {
        val serviceChannel = NotificationChannel(
            CHANNEL_ID,
            "Device Locker Service Channel",
            NotificationManager.IMPORTANCE_DEFAULT
        )

        val manager = getSystemService(NotificationManager::class.java)
        manager?.createNotificationChannel(serviceChannel)
    }

    companion object {
        const val CHANNEL_ID = "DeviceLockerServiceChannel"
        const val NOTIFICATION_ID = 1
    }
}
