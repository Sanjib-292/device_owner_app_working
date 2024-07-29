import 'dart:async';
import 'package:flutter/services.dart';

class DeviceLocker {
  static const MethodChannel _channel = MethodChannel('device_locker');

  static Future<void> activateDeviceAdmin() async {
    try {
      await _channel.invokeMethod('activateDeviceAdmin');
    } on PlatformException catch (e) {
      throw Exception('Failed to activate device admin: ${e.message}');
    }
  }

  static Future<void> lockDevice(String pin) async {
    try {
      await _channel.invokeMethod('lockDevice', {'pin': pin});
    } on PlatformException catch (e) {
      throw Exception('Failed to lock device: ${e.message}');
    }
  }

  static Future<void> unlockDevice() async {
    try {
      await _channel.invokeMethod('unlockDevice');
    } on PlatformException catch (e) {
      throw Exception('Failed to unlock device: ${e.message}');
    }
  }
}
