package com.example.device_owner_app

import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Bundle 
import android.view.WindowManager
import androidx.appcompat.app.AppCompatActivity
import com.example.device_owner_app.databinding.ActivityCustomLockScreenBinding

class CustomLockScreenActivity : AppCompatActivity() {

    private val correctPin = "1234"
    private lateinit var devicePolicyManager: DevicePolicyManager
    private lateinit var compName: ComponentName
    private lateinit var binding: ActivityCustomLockScreenBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setTheme(R.style.NormalTheme)
        binding = ActivityCustomLockScreenBinding.inflate(layoutInflater)
        setContentView(binding.root)

        devicePolicyManager = getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
        compName = ComponentName(this, MyDeviceAdminReceiver::class.java)

        window.addFlags(
            WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON or
            WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
            WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD or
            WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
        )

        binding.etPin.requestFocus()

        binding.btnUnlock.setOnClickListener {
            val enteredPin = binding.etPin.text.toString()
            if (enteredPin == correctPin) {
                unlockDevice()
            } else {
                binding.etPin.error = "Incorrect PIN"
            }
        }
    }

    private fun unlockDevice() {
        devicePolicyManager.resetPassword("", 0)
        val unlockIntent = Intent(this, MainActivity::class.java)
        unlockIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(unlockIntent)
        finish()
    }

    override fun onBackPressed() {
        // Override and do nothing to disable back button
    }

    override fun onUserLeaveHint() {
        // Override and do nothing to disable home button
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (!hasFocus) {
            val closeDialog = Intent(Intent.ACTION_CLOSE_SYSTEM_DIALOGS)
            sendBroadcast(closeDialog)
        }
    }
}
