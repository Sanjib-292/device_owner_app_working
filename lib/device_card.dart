import 'package:flutter/material.dart';

class DeviceInfoCard extends StatelessWidget {
  const DeviceInfoCard({
    super.key,
    required this.deviceName,
    required this.deviceModel,
    required this.imeiNumber,
  });

  final String? deviceName;
  final String? deviceModel;
  final String? imeiNumber;

  @override
  Widget build(BuildContext context) {
    return Column(
      mainAxisSize: MainAxisSize.min,
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Text('Device Name: $deviceName', style: TextStyle(fontSize: 18)),
        SizedBox(height: 8),
        Text('Device Model: $deviceModel', style: TextStyle(fontSize: 18)),
        SizedBox(height: 8),
        Text('IMEI: $imeiNumber', style: TextStyle(fontSize: 18)),
      ],
    );
  }
}
