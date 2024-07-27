package com.example.device_owner_app
import android.app.AlarmManager
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.os.SystemClock
import android.util.Log
import androidx.core.app.NotificationCompat
import io.flutter.plugin.common.MethodChannel
import io.flutter.embedding.engine.FlutterEngineCache
import io.flutter.embedding.engine.FlutterEngine



class BackgroundService : Service() {
    private val CHANNEL_ID = "ForegroundServiceChannel"
    private val CHANNEL = "com.example.device_owner_app/device_locker"

    override fun onCreate() {
        super.onCreate()
        startForegroundService()
        setupFlutterEngine()
    }

    private fun setupFlutterEngine() {
        // Get the cached FlutterEngine
        val flutterEngine = FlutterEngineCache
            .getInstance()
            .get("background_engine")

        if (flutterEngine != null) {
            MethodChannel(flutterEngine.dartExecutor.binaryMessenger, CHANNEL).setMethodCallHandler { call, result ->
                when (call.method) {
                    "lockDevice" -> {
                        val pin = call.argument<String>("pin")
                        if (pin != null) {
                            lockDevice(pin)
                            result.success(null)
                        } else {
                            result.error("INVALID_PIN", "Pin is required", null)
                        }
                    }
                    "unlockDevice" -> {
                        unlockDevice()
                        result.success(null)
                    }
                    else -> result.notImplemented()
                }
            }
        } else {
            Log.e("BackgroundService", "FlutterEngine not found in cache")
        }
    }

    private fun lockDevice(pin: String) {
        Log.d("BackgroundService", "Locking device with pin: $pin")
        // Implement lock device logic
    }

    private fun unlockDevice() {
        Log.d("BackgroundService", "Unlocking device")
        // Implement unlock device logic
    }

    private fun startForegroundService() {
        val channelId = CHANNEL_ID
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Foreground Service Channel",
                NotificationManager.IMPORTANCE_DEFAULT
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }

        val notificationIntent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            notificationIntent,
            PendingIntent.FLAG_IMMUTABLE
        )

        val notification: Notification = NotificationCompat.Builder(this, channelId)
            .setContentTitle("LockUnlock Service")
            .setContentText("Running in the background")
            .setSmallIcon(R.drawable.ic_notification)
            .setContentIntent(pendingIntent)
            .build()

        startForeground(1, notification)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.getStringExtra("action")
        val pin = intent?.getStringExtra("pin")

        Log.d("BackgroundService", "onStartCommand called with action: $action and pin: $pin")

        when (action) {
            "lockDevice" -> if (pin != null) lockDevice(pin)
            "unlockDevice" -> unlockDevice()
        }
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        val restartServiceIntent = Intent(applicationContext, this.javaClass).apply {
            setPackage(packageName)
        }
        val restartServicePendingIntent = PendingIntent.getService(
            applicationContext, 1, restartServiceIntent,
            PendingIntent.FLAG_ONE_SHOT or PendingIntent.FLAG_IMMUTABLE
        )
        val alarmManager = getSystemService(Context.ALARM_SERVICE) as? AlarmManager
        alarmManager?.set(
            AlarmManager.ELAPSED_REALTIME,
            SystemClock.elapsedRealtime() + 1000,
            restartServicePendingIntent
        )
        Log.d("BackgroundService", "Service destroyed, restart scheduled")
    }
}
