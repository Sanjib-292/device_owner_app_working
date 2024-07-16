package com.example.device_owner_app

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat

class BackgroundService : Service() {

    private val TAG = "BackgroundService"
    private var handler: Handler? = null
    private var runnable: Runnable = object : Runnable {
        override fun run() {
            if (isServiceRunning) {
                Log.d(TAG, "Service is still running in the background")
                // Schedule the next execution of the runnable after 10 seconds
                handler?.postDelayed(this, 10000) // 10 seconds delay
            }
        }
    }
    
    private var isServiceRunning = false

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "Service created")
        startForegroundService()

        // Start logging function every 10 seconds
        handler = Handler()
        handler?.postDelayed(runnable, 10000) // Start after 10 seconds initially
        isServiceRunning = true
    }

    private fun startForegroundService() {
        val channelId = "default_channel"
        val channelName = "Default Channel"

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val chan = NotificationChannel(channelId, channelName, NotificationManager.IMPORTANCE_DEFAULT)
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(chan)
        }

        val notification: Notification = NotificationCompat.Builder(this, channelId)
            .setContentTitle("App is running in the background")
            .setContentText("Your app is always running")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()

        startForeground(1, notification)
    }

    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {
        Log.d(TAG, "Service started")
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? {
        Log.d(TAG, "Service bound")
        return null
    }

    override fun onDestroy() {
        super.onDestroy()
        isServiceRunning = false
        handler?.removeCallbacks(runnable)
        Log.d(TAG, "Service destroyed")

        val restartServiceIntent = Intent(applicationContext, this.javaClass)
        restartServiceIntent.setPackage(packageName)
        startService(restartServiceIntent)
        Log.d(TAG, "Service restarted")
    }
}
