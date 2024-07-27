package com.example.device_owner_app

import android.app.Application
import android.os.Handler
import android.os.Looper
import io.flutter.embedding.engine.FlutterEngine
import io.flutter.embedding.engine.FlutterEngineCache
import io.flutter.embedding.engine.dart.DartExecutor
import io.flutter.plugin.common.MethodChannel
import android.util.Log

class MyApp : Application() {

    companion object {
        private const val FOREGROUND_CHANNEL = "com.example.device_owner_app/device_locker_foreground"
        private const val BACKGROUND_CHANNEL = "com.example.device_owner_app/device_locker_background"
        lateinit var flutterEngine: FlutterEngine
    }

    override fun onCreate() {
        super.onCreate()

        // Instantiate a FlutterEngine.
        flutterEngine = FlutterEngine(this)

        // Start executing Dart code in the FlutterEngine.
        flutterEngine.dartExecutor.executeDartEntrypoint(
            DartExecutor.DartEntrypoint.createDefault()
        )

        // Cache the FlutterEngine to be used by FlutterActivity or other Dart execution.
        FlutterEngineCache
            .getInstance()
            .put("background_engine", flutterEngine)

        // Register MethodChannel for foreground tasks
        MethodChannel(flutterEngine.dartExecutor.binaryMessenger, FOREGROUND_CHANNEL).setMethodCallHandler { call, result ->
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

        // Register MethodChannel for background tasks
        MethodChannel(flutterEngine.dartExecutor.binaryMessenger, BACKGROUND_CHANNEL).setMethodCallHandler { call, result ->
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
    }

    private fun lockDevice(pin: String) {
        Handler(Looper.getMainLooper()).post {
            // Implement lock device logic
            Log.d("MyApp", "Device locked with pin: $pin")
        }
    }

    private fun unlockDevice() {
        Handler(Looper.getMainLooper()).post {
            // Implement unlock device logic
            Log.d("MyApp", "Device unlocked")
        }
    }
}
