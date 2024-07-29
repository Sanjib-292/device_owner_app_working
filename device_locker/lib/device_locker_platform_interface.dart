import 'package:plugin_platform_interface/plugin_platform_interface.dart';

import 'device_locker_method_channel.dart';

abstract class DeviceLockerPlatform extends PlatformInterface {
  /// Constructs a DeviceLockerPlatform.
  DeviceLockerPlatform() : super(token: _token);

  static final Object _token = Object();

  static DeviceLockerPlatform _instance = MethodChannelDeviceLocker();

  /// The default instance of [DeviceLockerPlatform] to use.
  ///
  /// Defaults to [MethodChannelDeviceLocker].
  static DeviceLockerPlatform get instance => _instance;

  /// Platform-specific implementations should set this with their own
  /// platform-specific class that extends [DeviceLockerPlatform] when
  /// they register themselves.
  static set instance(DeviceLockerPlatform instance) {
    PlatformInterface.verifyToken(instance, _token);
    _instance = instance;
  }

  Future<String?> getPlatformVersion() {
    throw UnimplementedError('platformVersion() has not been implemented.');
  }
}
