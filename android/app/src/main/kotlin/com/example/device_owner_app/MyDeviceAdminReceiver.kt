package com.example.device_owner_app

import android.app.admin.DeviceAdminReceiver
import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.UserHandle
import android.util.Base64
import android.util.Log
import android.widget.Toast
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKeys
import java.security.GeneralSecurityException
import java.security.SecureRandom

class MyDeviceAdminReceiver : DeviceAdminReceiver() {

    override fun onEnabled(context: Context, intent: Intent) {
        super.onEnabled(context, intent)
        Log.d(TAG, "Device Admin Enabled")
    }

    override fun onDisabled(context: Context, intent: Intent) {
        super.onDisabled(context, intent)
        Log.d(TAG, "Device Admin Disabled")
    }

    override fun onPasswordChanged(context: Context, intent: Intent, userHandle: UserHandle) {
        super.onPasswordChanged(context, intent, userHandle)
        Log.d(TAG, "Password Changed")

        val devicePolicyManager = context.getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
        val compName = ComponentName(context, MyDeviceAdminReceiver::class.java)

        try {
            val token = generateAndActivateResetPasswordToken(devicePolicyManager, compName)

            // Save the token
            if (token != null) {
                saveToken(context, token)
            } else {
                Log.e(TAG, "Failed to generate reset password token")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error generating or saving reset password token", e)
        }
    }

    private fun generateAndActivateResetPasswordToken(devicePolicyManager: DevicePolicyManager, compName: ComponentName): ByteArray? {
        return try {
            val token = ByteArray(32)
            SecureRandom().nextBytes(token)
            val success = devicePolicyManager.setResetPasswordToken(compName, token)
            if (success) {
                Log.d(TAG, "Reset password token generated and set successfully")
                token
            } else {
                Log.e(TAG, "Failed to set reset password token")
                null
            }
        } catch (e: SecurityException) {
            Log.e(TAG, "SecurityException: ${e.message}", e)
            null
        }
    }

    private fun saveToken(context: Context, token: ByteArray) {
        try {
            val masterKeyAlias = MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC)
            val sharedPreferences = EncryptedSharedPreferences.create(
                PREFS_NAME,
                masterKeyAlias,
                context,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            )

            val encodedToken = Base64.encodeToString(token, Base64.DEFAULT)
            sharedPreferences.edit().putString(TOKEN_KEY, encodedToken).apply()
            Log.d(TAG, "Reset password token saved successfully")
        } catch (e: GeneralSecurityException) {
            Log.e(TAG, "GeneralSecurityException: ${e.message}", e)
        } catch (e: Exception) {
            Log.e(TAG, "Exception: ${e.message}", e)
        }
    }

    private fun showToast(context: Context, msg: String) {
        Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
    }

    companion object {
        private const val TAG = "MyDeviceAdminReceiver"
        private const val PREFS_NAME = "secret_shared_prefs"
        private const val TOKEN_KEY = "password_token"
    }
}
