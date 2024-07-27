import 'dart:isolate';

import 'package:flutter/services.dart';

class DeviceLockService {
  static const platform =
      MethodChannel('com.example.device_owner_app/device_locker');

  static Future<void> lockDevice(String pin) async {
    print(
        'Forground lockDevice called in isolate: ${Isolate.current.debugName}');

    try {
      await platform.invokeMethod('lockDevice', {'pin': pin});
    } catch (e) {
      throw Exception('Failed to lock device: $e');
    }
  }

  static Future<void> unlockDevice() async {
    try {
      await platform.invokeMethod('unlockDevice');
    } on PlatformException catch (e) {
      print("Failed to unlock device: '${e.message}'.");
    }
  }
}
