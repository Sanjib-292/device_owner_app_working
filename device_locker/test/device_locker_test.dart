import 'package:flutter_test/flutter_test.dart';
import 'package:device_locker/device_locker.dart';
import 'package:device_locker/device_locker_platform_interface.dart';
import 'package:device_locker/device_locker_method_channel.dart';
import 'package:plugin_platform_interface/plugin_platform_interface.dart';

class MockDeviceLockerPlatform
    with MockPlatformInterfaceMixin
    implements DeviceLockerPlatform {

  @override
  Future<String?> getPlatformVersion() => Future.value('42');
}

void main() {
  final DeviceLockerPlatform initialPlatform = DeviceLockerPlatform.instance;

  test('$MethodChannelDeviceLocker is the default instance', () {
    expect(initialPlatform, isInstanceOf<MethodChannelDeviceLocker>());
  });

  test('getPlatformVersion', () async {
    DeviceLocker deviceLockerPlugin = DeviceLocker();
    MockDeviceLockerPlatform fakePlatform = MockDeviceLockerPlatform();
    DeviceLockerPlatform.instance = fakePlatform;

    expect(await deviceLockerPlugin.getPlatformVersion(), '42');
  });
}
