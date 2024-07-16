package com.example.device_owner_app

import android.app.admin.DevicePolicyManager
import android.app.KeyguardManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import android.provider.Settings
import android.util.Base64
import android.util.Log
import androidx.annotation.NonNull
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKeys
import io.flutter.embedding.android.FlutterActivity
import io.flutter.embedding.engine.FlutterEngine
import io.flutter.plugin.common.MethodCall
import io.flutter.plugin.common.MethodChannel
import android.net.Uri

import io.flutter.plugins.GeneratedPluginRegistrant
import java.security.SecureRandom

class MainActivity : FlutterActivity() {

    private val CHANNEL = "com.example.device_owner_app/device_locker"
    private lateinit var devicePolicyManager: DevicePolicyManager
    private lateinit var compName: ComponentName
    private lateinit var keyguardManager: KeyguardManager

    override fun configureFlutterEngine(@NonNull flutterEngine: FlutterEngine) {
        super.configureFlutterEngine(flutterEngine)
        GeneratedPluginRegistrant.registerWith(flutterEngine)

        devicePolicyManager = getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
        compName = ComponentName(this, MyDeviceAdminReceiver::class.java)
        keyguardManager = getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager

        MethodChannel(flutterEngine.dartExecutor.binaryMessenger, CHANNEL).setMethodCallHandler { call, result ->
            when (call.method) {
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
    }

    private fun isDeviceOwner(): Boolean {
        Log.d(TAG, "Checking device owner status...")
        val isOwner = devicePolicyManager.isDeviceOwnerApp(packageName)
        Log.d(TAG, "Is device owner: $isOwner")
        return isOwner
    }

    private fun getToken(): ByteArray {
        val masterKeyAlias = MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC)

        Log.d(TAG, "Retrieving token from encrypted shared preferences")
        val sharedPreferences = EncryptedSharedPreferences.create(
            "secret_shared_prefs",
            masterKeyAlias,
            this,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
        val tokenString = sharedPreferences.getString("password_token", null)
        return if (tokenString != null) {
            Log.d(TAG, "Token retrieved: $tokenString")
            Base64.decode(tokenString, Base64.DEFAULT)
        } else {
            Log.d(TAG, "No token found")
            ByteArray(0)
        }
    }

    private fun lockDevice(pin: String) {
        Log.d(TAG, "Attempting to lock device with PIN: $pin")
        if (isDeviceOwner()) {
            try {
                var token = getToken()
                if (token.isEmpty()) {
                    token = generateAndSaveToken()
                }
                Log.d(TAG, "Generated token: ${Base64.encodeToString(token, Base64.DEFAULT)}")

                // Activate the token if not already active
                if (!devicePolicyManager.isResetPasswordTokenActive(compName)) {
                    activateToken(token)
                }

                // Wait and retry until the token is confirmed as active
                var retryCount = 0
                val maxRetries = 5
                val retryDelay = 1000L
                var tokenActive = false

                while (retryCount < maxRetries && !tokenActive) {
                    Thread.sleep(retryDelay)
                    tokenActive = devicePolicyManager.isResetPasswordTokenActive(compName)
                    retryCount++
                    Log.d(TAG, "Retry count: $retryCount, Token active: $tokenActive")
                }

                if (tokenActive) {
                    Log.d(TAG, "Token is active")
                    resetPasswordWithToken(pin, token)
                    devicePolicyManager.lockNow()
                    Log.d(TAG, "Device locked successfully")
                } else {
                    Log.e(TAG, "Token activation failed or token is not active after retries")
                }
            } catch (e: SecurityException) {
                Log.e(TAG, "Failed to lock device: ${e.message}")
            } catch (e: InterruptedException) {
                Log.e(TAG, "Thread sleep interrupted: ${e.message}")
            }
        } else {
            Log.e(TAG, "This app is not the device owner")
            requestDeviceAdminPermission()
        }
    }


    private fun activateToken(token: ByteArray) {
        Log.d(TAG, "Activating token...")

        // Check if the device has a lock screen password set
        if (isLockScreenPasswordSet()) {
            val confirmIntent = keyguardManager.createConfirmDeviceCredentialIntent(null, ACTIVATE_TOKEN_PROMPT)

            if (confirmIntent != null) {
                startActivityForResult(confirmIntent, ACTIVATE_TOKEN_REQUEST)
            } else {
                // Null means the user doesn't have a lock screen, so the token is already active
                Log.d(TAG, "Device does not have a lock screen password, token is already active")
            }
        } else {
            // No lock screen password set, token is already active
            Log.d(TAG, "Device does not have a lock screen password, token is already active")
        }
    }

    private fun resetPasswordWithToken(newPassword: String, token: ByteArray) {
        try {
            Log.d(TAG, "Attempting to reset password with token")
            val result = devicePolicyManager.resetPasswordWithToken(compName, newPassword, token, 0)

            if (result) {
                Log.d(TAG, "Password reset successfully to: $newPassword")
            } else {
                Log.e(TAG, "Password reset failed due to password restrictions or other reasons")
            }
        } catch (e: IllegalStateException) {
            Log.e(TAG, "Token mismatch: ${e.message}")
        } catch (e: SecurityException) {
            Log.e(TAG, "Security exception: ${e.message}")
        }
    }

    private fun generateAndSaveToken(): ByteArray {
        Log.d(TAG, "Generating reset password token")
        val token = ByteArray(32)
        val random = SecureRandom()
        random.nextBytes(token)
        val success = devicePolicyManager.setResetPasswordToken(compName, token)
        Log.d(TAG, "Token generation success: $success")

        if (success) {
            val encodedToken = Base64.encodeToString(token, Base64.DEFAULT)
            saveToken(encodedToken)
            Log.d(TAG, "Token saved: $encodedToken")
        }

        return token
    }

    private fun saveToken(token: String) {
        Log.d(TAG, "Saving token to encrypted shared preferences")
        val masterKeyAlias = MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC)
        val sharedPreferences = EncryptedSharedPreferences.create(
            "secret_shared_prefs",
            masterKeyAlias,
            this,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )

        with(sharedPreferences.edit()) {
            putString("password_token", token)
            apply()
        }
        Log.d(TAG, "Token saved successfully")
    }

    private fun isLockScreenPasswordSet(): Boolean {
        return keyguardManager.isKeyguardSecure
    }

    private fun unlockDevice() {
        Log.d(TAG, "Attempting to unlock device...")
        if (devicePolicyManager.isAdminActive(compName)) {
            try {
                val token = getToken()
                if (token.isNotEmpty()) {
                    resetPasswordWithToken("", token)
                    Log.d(TAG, "Device unlocked successfully")
                } else {
                    Log.e(TAG, "No token available to reset password")
                }
            } catch (e: SecurityException) {
                Log.e(TAG, "Failed to unlock device: ${e.message}")
            }
        } else {
            Log.e(TAG, "This app is not a device admin")
            requestDeviceAdminPermission()
        }
    }

    private fun requestDeviceAdminPermission() {
        Log.d(TAG, "Requesting device admin permission...")
        val intent = Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN).apply {
            putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, compName)
            putExtra(DevicePolicyManager.EXTRA_ADD_EXPLANATION, "Please enable device admin to use lock/unlock features.")
        }
        startActivityForResult(intent, REQUEST_CODE_ENABLE_ADMIN)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == ACTIVATE_TOKEN_REQUEST) {
            if (resultCode == RESULT_OK) {

                Log.d(TAG, "Token activation confirmed by user")
                lockDevice("1234")
            } else {
                Log.e(TAG, "Token activation failed or cancelled by user")
            }
        } else if (requestCode == REQUEST_CODE_ENABLE_ADMIN) {
            if (resultCode == RESULT_OK) {
                Log.d(TAG, "Admin enabled!")
            } else {
                Log.d(TAG, "Admin enable FAILED!")
            }
        }
    }

    companion object {
        private const val REQUEST_CODE_ENABLE_ADMIN = 1
        private const val ACTIVATE_TOKEN_PROMPT = "Use your credentials to enable remote password reset"
        private const val ACTIVATE_TOKEN_REQUEST = 1
        private const val TAG = "MainActivity"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        Log.d(TAG, "onCreate: Checking if device admin is active")
        if (!devicePolicyManager.isAdminActive(compName)) {
            Log.d(TAG, "Device admin is not active, requesting permission")
            val intent = Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN).apply {
                putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, compName)
                putExtra(DevicePolicyManager.EXTRA_ADD_EXPLANATION, "You need to enable Device Admin to use this app.")
            }
            requestBatteryOptimizationExemption()

            // Start the foreground service
            val serviceIntent = Intent(this, BackgroundService::class.java)
            startActivityForResult(intent, REQUEST_CODE_ENABLE_ADMIN)
        } else {
            Log.d(TAG, "Device admin is already active")
        }

        Log.d(TAG, "Checking if app is device owner")
        if (devicePolicyManager.isDeviceOwnerApp(packageName)) {
            Log.d(TAG, "App is device owner, setting uninstall blocked")
            devicePolicyManager.setUninstallBlocked(compName, packageName, true)
        }
    }


    private fun requestBatteryOptimizationExemption() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val intent = Intent()
            val packageName = packageName
            val pm = getSystemService(Context.POWER_SERVICE) as PowerManager
            if (!pm.isIgnoringBatteryOptimizations(packageName)) {
                intent.action = Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS
                intent.data = Uri.parse("package:$packageName")
                startActivity(intent)
            }
        }
    }
}
