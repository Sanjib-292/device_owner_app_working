package com.example.device_locker

import android.app.KeyguardManager
import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.util.Base64
import android.util.Log
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKeys
import io.flutter.embedding.engine.plugins.FlutterPlugin
import io.flutter.embedding.engine.plugins.activity.ActivityAware
import io.flutter.embedding.engine.plugins.activity.ActivityPluginBinding
import io.flutter.plugin.common.MethodCall
import io.flutter.plugin.common.MethodChannel
import io.flutter.plugin.common.MethodChannel.MethodCallHandler
import java.security.SecureRandom

class DeviceLockerPlugin : FlutterPlugin, MethodCallHandler, ActivityAware {
    private lateinit var channel: MethodChannel
    private lateinit var context: Context
    private lateinit var devicePolicyManager: DevicePolicyManager
    private lateinit var compName: ComponentName
    private lateinit var keyguardManager: KeyguardManager

    override fun onAttachedToEngine(flutterPluginBinding: FlutterPlugin.FlutterPluginBinding) {
        context = flutterPluginBinding.applicationContext
        channel = MethodChannel(flutterPluginBinding.binaryMessenger, "device_locker")
        channel.setMethodCallHandler(this)
        initializeDevicePolicyManager()
        Log.d("DeviceLockerPlugin", "onAttachedToEngine called")
    }

    private fun initializeDevicePolicyManager() {
        devicePolicyManager = context.getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
        compName = ComponentName(context, MyDeviceAdminReceiver::class.java)
        keyguardManager = context.getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager
    }

    override fun onDetachedFromEngine(binding: FlutterPlugin.FlutterPluginBinding) {
        channel.setMethodCallHandler(null)
    }

    override fun onAttachedToActivity(binding: ActivityPluginBinding) {
        Log.d("DeviceLockerPlugin", "onAttachedToActivity called")
        devicePolicyManager = context.getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
        compName = ComponentName(context, MyDeviceAdminReceiver::class.java)
        keyguardManager = context.getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager
    }

    override fun onDetachedFromActivityForConfigChanges() {
        Log.d("DeviceLockerPlugin", "onDetachedFromActivityForConfigChanges called")
    }

    override fun onReattachedToActivityForConfigChanges(binding: ActivityPluginBinding) {
        Log.d("DeviceLockerPlugin", "onReattachedToActivityForConfigChanges called")
        onAttachedToActivity(binding)
    }

    override fun onDetachedFromActivity() {
        Log.d("DeviceLockerPlugin", "onDetachedFromActivity called")
    }

    override fun onMethodCall(call: MethodCall, result: MethodChannel.Result) {
        when (call.method) {
            "activateDeviceAdmin" -> {
                if (::compName.isInitialized) {
                    activateDeviceAdmin()
                    result.success(null)
                } else {
                    activateDeviceAdmin()
                    result.error("ERROR", "compName has not been initialized", null)
                }
            }
            "lockDevice" -> {
                val pin = call.argument<String>("pin")
                if (pin != null) {
                    lockDevice(pin)
                    result.success(null)
                } else {
                    result.error("ERROR", "PIN is required", null)
                }
            }
            "unlockDevice" -> {
                unlockDevice()
                result.success(null)
            }
            else -> result.notImplemented()
        }
    }

    private fun activateDeviceAdmin() {
        val intent = Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN)
        intent.putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, compName)
        intent.putExtra(DevicePolicyManager.EXTRA_ADD_EXPLANATION, "Please enable device admin to use lock/unlock features.")
        context.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }

    private fun lockDevice(pin: String) {
        Log.d("DeviceLockerPlugin", "Locking device with pin: $pin")
        if (isDeviceOwner()) {
            try {
                var token = getToken()
                if (token.isEmpty()) {
                    token = generateAndSaveToken()
                }
                if (!devicePolicyManager.isResetPasswordTokenActive(compName)) {
                    activateToken(token)
                }
                if (devicePolicyManager.isResetPasswordTokenActive(compName)) {
                    resetPasswordWithToken(pin, token)
                    devicePolicyManager.lockNow()
                    Log.d("DeviceLockerPlugin", "Device locked successfully")
                } else {
                    Log.e("DeviceLockerPlugin", "Token activation failed")
                }
            } catch (e: SecurityException) {
                Log.e("DeviceLockerPlugin", "Failed to lock device: ${e.message}")
            }
        } else {
            Log.e("DeviceLockerPlugin", "This app is not the device owner")
        }
    }

    private fun unlockDevice() {
        Log.d("DeviceLockerPlugin", "Unlocking device")
        if (devicePolicyManager.isAdminActive(compName)) {
            try {
                val token = getToken()
                if (token.isNotEmpty()) {
                    resetPasswordWithToken("", token)
                    Log.d("DeviceLockerPlugin", "Device unlocked successfully")
                } else {
                    Log.e("DeviceLockerPlugin", "No token available to reset password")
                }
            } catch (e: SecurityException) {
                Log.e("DeviceLockerPlugin", "Failed to unlock device: ${e.message}")
            }
        } else {
            Log.e("DeviceLockerPlugin", "This app is not a device admin")
        }
    }

    private fun activateToken(token: ByteArray) {
        Log.d("DeviceLockerPlugin", "Activating token")
        if (isLockScreenPasswordSet()) {
            val confirmIntent = keyguardManager.createConfirmDeviceCredentialIntent(null, null)
            confirmIntent?.let {
                context.startActivity(it.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            }
        }
    }

    private fun resetPasswordWithToken(newPassword: String, token: ByteArray) {
        try {
            val result = devicePolicyManager.resetPasswordWithToken(compName, newPassword, token, 0)
            if (result) {
                Log.d("DeviceLockerPlugin", "Password reset successfully")
            } else {
                Log.e("DeviceLockerPlugin", "Password reset failed")
            }
        } catch (e: IllegalStateException) {
            Log.e("DeviceLockerPlugin", "Token mismatch: ${e.message}")
        } catch (e: SecurityException) {
            Log.e("DeviceLockerPlugin", "Security exception: ${e.message}")
        }
    }

    private fun generateAndSaveToken(): ByteArray {
        val token = ByteArray(32)
        SecureRandom().nextBytes(token)
        val success = devicePolicyManager.setResetPasswordToken(compName, token)
        if (success) {
            saveToken(Base64.encodeToString(token, Base64.DEFAULT))
        }
        return token
    }

    private fun saveToken(token: String) {
        val masterKeyAlias = MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC)
        val sharedPreferences = EncryptedSharedPreferences.create(
            "secret_shared_prefs",
            masterKeyAlias,
            context,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
        with(sharedPreferences.edit()) {
            putString("password_token", token)
            apply()
        }
    }

    private fun getToken(): ByteArray {
        val masterKeyAlias = MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC)
        val sharedPreferences = EncryptedSharedPreferences.create(
            "secret_shared_prefs",
            masterKeyAlias,
            context,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
        val tokenString = sharedPreferences.getString("password_token", null)
        return if (tokenString != null) {
            Base64.decode(tokenString, Base64.DEFAULT)
        } else {
            ByteArray(0)
        }
    }

    private fun isDeviceOwner(): Boolean {
        return devicePolicyManager.isDeviceOwnerApp(context.packageName)
    }

    private fun isLockScreenPasswordSet(): Boolean {
        return keyguardManager.isKeyguardSecure
    }
}
