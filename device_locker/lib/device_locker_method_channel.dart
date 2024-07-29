import 'package:flutter/foundation.dart';
import 'package:flutter/services.dart';

import 'device_locker_platform_interface.dart';

/// An implementation of [DeviceLockerPlatform] that uses method channels.
class MethodChannelDeviceLocker extends DeviceLockerPlatform {
  /// The method channel used to interact with the native platform.
  @visibleForTesting
  final methodChannel = const MethodChannel('device_locker');

  @override
  Future<String?> getPlatformVersion() async {
    final version = await methodChannel.invokeMethod<String>('getPlatformVersion');
    return version;
  }
}
