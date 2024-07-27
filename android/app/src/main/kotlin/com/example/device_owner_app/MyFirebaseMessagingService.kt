package com.example.device_owner_app

import android.util.Log
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import io.flutter.plugin.common.MethodChannel

class MyFirebaseMessagingService : FirebaseMessagingService() {
    private val CHANNEL = "com.example.device_owner_app/device_locker"

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        val action = remoteMessage.data["action"]
        val pin = remoteMessage.data["pin"]

        val flutterEngine = MyApp.flutterEngine
        val methodChannel = MethodChannel(flutterEngine.dartExecutor.binaryMessenger, CHANNEL)

        when (action) {
            "lockDevice" -> {
                methodChannel.invokeMethod("lockDevice", mapOf("pin" to pin))
                Log.d("MyFirebaseMessagingService", "Invoked lockDevice method on MethodChannel")
            }
            "unlockDevice" -> {
                methodChannel.invokeMethod("unlockDevice", null)
                Log.d("MyFirebaseMessagingService", "Invoked unlockDevice method on MethodChannel")
            }
        }
    }
}
