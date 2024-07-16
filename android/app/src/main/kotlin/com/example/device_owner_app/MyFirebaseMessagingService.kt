package com.example.device_owner_app

import android.content.Context
import android.util.Log
import androidx.core.app.NotificationCompat
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import android.app.admin.DevicePolicyManager
import android.content.ComponentName


class MyFirebaseMessagingService : FirebaseMessagingService() {

   override fun onMessageReceived(remoteMessage: RemoteMessage) {
        // Example usage: Lock the device using DevicePolicyManager
        val context = applicationContext
        val devicePolicyManager = context.getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
        val componentName = ComponentName(context, MyDeviceAdminReceiver::class.java)

        if (devicePolicyManager.isAdminActive(componentName)) {
            devicePolicyManager.lockNow()
        } else {
            Log.e(TAG, "Device admin not active")
        }
    }

    override fun onNewToken(token: String) {
        // Handle token refresh logic here
    }


    companion object {
        private const val TAG = "MyFirebaseMessaging"
    }
}
