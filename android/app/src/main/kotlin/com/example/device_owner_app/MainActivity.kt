package com.example.device_owner_app

import android.Manifest
import android.app.KeyguardManager
import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import android.provider.Settings
import android.util.Base64
import android.util.Log
import android.widget.Toast
import androidx.annotation.NonNull
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKeys
import io.flutter.embedding.android.FlutterActivity
import io.flutter.embedding.engine.FlutterEngine
import io.flutter.plugin.common.MethodChannel
import java.security.SecureRandom
import io.flutter.plugins.GeneratedPluginRegistrant


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
       // GeneratedPluginRegistrant.registerWith(flutterEngine)

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

    private fun lockDevice(pin: String) {
        Log.d("MainActivity", "Locking device with pin: $pin")
        if (isDeviceOwner()) {
            try {
                var token = getToken()
                if (token.isEmpty()) {
                    token = generateAndSaveToken()
                }
                Log.d("MainActivity", "Generated token: ${Base64.encodeToString(token, Base64.DEFAULT)}")

                if (!devicePolicyManager.isResetPasswordTokenActive(compName)) {
                    activateToken(token)
                }

                var retryCount = 0
                val maxRetries = 5
                val retryDelay = 1000L
                var tokenActive = false

                while (retryCount < maxRetries && !tokenActive) {
                    Thread.sleep(retryDelay)
                    tokenActive = devicePolicyManager.isResetPasswordTokenActive(compName)
                    retryCount++
                    Log.d("MainActivity", "Retry count: $retryCount, Token active: $tokenActive")
                }

                if (tokenActive) {
                    Log.d("MainActivity", "Token is active")
                    resetPasswordWithToken(pin, token)
                    devicePolicyManager.lockNow()
                    Log.d("MainActivity", "Device locked successfully")
                } else {
                    Log.e("MainActivity", "Token activation failed or token is not active after retries")
                }
            } catch (e: SecurityException) {
                Log.e("MainActivity", "Failed to lock device: ${e.message}")
            } catch (e: InterruptedException) {
                Log.e("MainActivity", "Thread sleep interrupted: ${e.message}")
            }
        } else {
            Log.e("MainActivity", "This app is not the device owner")
            requestDeviceAdminPermission()
        }
    }

    private fun unlockDevice() {
        Log.d("MainActivity", "Attempting to unlock device...")
        if (devicePolicyManager.isAdminActive(compName)) {
            try {
                val token = getToken()
                if (token.isNotEmpty()) {
                    resetPasswordWithToken("", token)
                    Log.d("MainActivity", "Device unlocked successfully")
                } else {
                    Log.e("MainActivity", "No token available to reset password")
                }
            } catch (e: SecurityException) {
                Log.e("MainActivity", "Failed to unlock device: ${e.message}")
            }
        } else {
            Log.e("MainActivity", "This app is not a device admin")
            requestDeviceAdminPermission()
        }
    }

    private fun activateToken(token: ByteArray) {
        Log.d("MainActivity", "Activating token...")
        if (isLockScreenPasswordSet()) {
            val confirmIntent = keyguardManager.createConfirmDeviceCredentialIntent(null, "Use your credentials to enable remote password reset")
            confirmIntent?.let {
                startActivityForResult(it, 1)
            } ?: Log.d("MainActivity", "Device does not have a lock screen password, token is already active")
        } else {
            Log.d("MainActivity", "Device does not have a lock screen password, token is already active")
        }
    }

    private fun resetPasswordWithToken(newPassword: String, token: ByteArray) {
        try {
            Log.d("MainActivity", "Attempting to reset password with token")
            val result = devicePolicyManager.resetPasswordWithToken(compName, newPassword, token, 0)
            if (result) {
                Log.d("MainActivity", "Password reset successfully to: $newPassword")
            } else {
                Log.e("MainActivity", "Password reset failed due to password restrictions or other reasons")
            }
        } catch (e: IllegalStateException) {
            Log.e("MainActivity", "Token mismatch: ${e.message}")
        } catch (e: SecurityException) {
            Log.e("MainActivity", "Security exception: ${e.message}")
        }
    }

    private fun generateAndSaveToken(): ByteArray {
        Log.d("MainActivity", "Generating reset password token")
        val token = ByteArray(32)
        val random = SecureRandom()
        random.nextBytes(token)
        val success = devicePolicyManager.setResetPasswordToken(compName, token)
        Log.d("MainActivity", "Token generation success: $success")

        if (success) {
            val encodedToken = Base64.encodeToString(token, Base64.DEFAULT)
            saveToken(encodedToken)
            Log.d("MainActivity", "Token saved: $encodedToken")
        }

        return token
    }

    private fun saveToken(token: String) {
        Log.d("MainActivity", "Saving token to encrypted shared preferences")
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
        Log.d("MainActivity", "Token saved successfully")
    }

    private fun getToken(): ByteArray {
        val masterKeyAlias = MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC)
        Log.d("MainActivity", "Retrieving token from encrypted shared preferences")
        val sharedPreferences = EncryptedSharedPreferences.create(
            "secret_shared_prefs",
            masterKeyAlias,
            this,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
        val tokenString = sharedPreferences.getString("password_token", null)
        return if (tokenString != null) {
            Log.d("MainActivity", "Token retrieved: $tokenString")
            Base64.decode(tokenString, Base64.DEFAULT)
        } else {
            Log.d("MainActivity", "No token found")
            ByteArray(0)
        }
    }

    private fun isDeviceOwner(): Boolean {
        Log.d("MainActivity", "Checking device owner status...")
        val isOwner = devicePolicyManager.isDeviceOwnerApp(packageName)
        Log.d("MainActivity", "Is device owner: $isOwner")
        return isOwner
    }

    private fun isLockScreenPasswordSet(): Boolean {
        return keyguardManager.isKeyguardSecure
    }

    private fun requestDeviceAdminPermission() {
        Log.d("MainActivity", "Requesting device admin permission...")
        val intent = Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN).apply {
            putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, compName)
            putExtra(DevicePolicyManager.EXTRA_ADD_EXPLANATION, "Please enable device admin to use lock/unlock features.")
        }
        startActivityForResult(intent, 1)
    }

    private fun startBackgroundService() {
        val serviceIntent = Intent(this, BackgroundService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(serviceIntent)
        } else {
            startService(serviceIntent)
        }
        Log.d("MainActivity", "Background service started")
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

    companion object {
        private const val REQUEST_CODE_ENABLE_ADMIN = 1
        private const val TAG = "MainActivity"

        private val REQUIRED_PERMISSIONS = arrayOf(
            Manifest.permission.FOREGROUND_SERVICE,
            Manifest.permission.BLUETOOTH_ADVERTISE,
            Manifest.permission.BLUETOOTH_CONNECT,
            Manifest.permission.BLUETOOTH_SCAN,
            Manifest.permission.CHANGE_NETWORK_STATE,
            Manifest.permission.CHANGE_WIFI_STATE,
            Manifest.permission.CHANGE_WIFI_MULTICAST_STATE,
            Manifest.permission.NFC,
            Manifest.permission.TRANSMIT_IR,
            Manifest.permission.UWB_RANGING
        )

        private const val REQUEST_CODE_PERMISSIONS = 10
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestBatteryOptimizationExemption()
        startBackgroundService()
        if (allPermissionsGranted()) {
            proceedWithAppInitialization()
        } else {
            ActivityCompat.requestPermissions(this, REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS)
        }
    }

    private fun allPermissionsGranted() = REQUIRED_PERMISSIONS.all {
        ContextCompat.checkSelfPermission(baseContext, it) == PackageManager.PERMISSION_GRANTED
    }

    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<out String>, grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_CODE_PERMISSIONS) {
            if (allPermissionsGranted()) {
                proceedWithAppInitialization()
            } else {
                Toast.makeText(this, "Permissions not granted by the user.", Toast.LENGTH_SHORT).show()
                finish()
            }
        }
    }

    private fun proceedWithAppInitialization() {
        Log.d(TAG, "onCreate: Checking if device admin is active")
        if (!devicePolicyManager.isAdminActive(compName)) {
            Log.d(TAG, "Device admin is not active, requesting permission")
            val intent = Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN).apply {
                putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, compName)
                putExtra(DevicePolicyManager.EXTRA_ADD_EXPLANATION, "You need to enable Device Admin to use this app.")
            }
            startActivityForResult(intent, REQUEST_CODE_ENABLE_ADMIN)
        } else {
            Log.d(TAG, "Device admin is already active")
            startBackgroundService()
        }

        Log.d(TAG, "Checking if app is device owner")
        if (devicePolicyManager.isDeviceOwnerApp(packageName)) {
            Log.d(TAG, "App is device owner, setting uninstall blocked")
            devicePolicyManager.setUninstallBlocked(compName, packageName, true)
        }
    }
}
