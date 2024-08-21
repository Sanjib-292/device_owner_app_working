package com.example.device_locker

import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.util.Log
import io.flutter.embedding.engine.plugins.FlutterPlugin
import io.flutter.embedding.engine.plugins.activity.ActivityAware
import io.flutter.embedding.engine.plugins.activity.ActivityPluginBinding
import io.flutter.plugin.common.MethodCall
import io.flutter.plugin.common.MethodChannel
import io.flutter.plugin.common.MethodChannel.MethodCallHandler

class DeviceLockerPlugin : FlutterPlugin, MethodCallHandler, ActivityAware {
    private lateinit var channel: MethodChannel
    private lateinit var context: Context
    private lateinit var devicePolicyManager: DevicePolicyManager
    private lateinit var compName: ComponentName

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
    }

    override fun onDetachedFromEngine(binding: FlutterPlugin.FlutterPluginBinding) {
        channel.setMethodCallHandler(null)
    }

    override fun onAttachedToActivity(binding: ActivityPluginBinding) {
        Log.d("DeviceLockerPlugin", "onAttachedToActivity called")
        initializeDevicePolicyManager()
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
                activateDeviceAdmin()
                result.success(null)
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
                devicePolicyManager.resetPassword(pin, DevicePolicyManager.RESET_PASSWORD_REQUIRE_ENTRY)
                devicePolicyManager.lockNow()
                Log.d("DeviceLockerPlugin", "Device locked successfully")
            } catch (e: SecurityException) {
                Log.e("DeviceLockerPlugin", "Failed to lock device: ${e.message}")
            }
        } else {
            Log.e("DeviceLockerPlugin", "This app is not the device owner")
        }
    }

    private fun unlockDevice() {
        Log.d("DeviceLockerPlugin", "Unlocking device")
        if (isDeviceOwner()) {
            try {
                devicePolicyManager.resetPassword("", DevicePolicyManager.RESET_PASSWORD_REQUIRE_ENTRY)
                Log.d("DeviceLockerPlugin", "Device unlocked successfully")
            } catch (e: SecurityException) {
                Log.e("DeviceLockerPlugin", "Failed to unlock device: ${e.message}")
            }
        } else {
            Log.e("DeviceLockerPlugin", "This app is not the device owner")
        }
    }

    private fun isDeviceOwner(): Boolean {
        return devicePolicyManager.isDeviceOwnerApp(context.packageName)
    }
}
