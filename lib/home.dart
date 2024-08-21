import 'dart:convert';

// import 'package:device_information/device_information.dart';
import 'package:device_imei/device_imei.dart';
import 'package:device_info_plus/device_info_plus.dart';
import 'package:device_owner_app/device_card.dart';
import 'package:device_owner_app/device_form.dart';
import 'package:firebase_crashlytics/firebase_crashlytics.dart';
import 'package:firebase_messaging/firebase_messaging.dart';
import 'package:flutter/material.dart';
import 'package:http/http.dart' as http;
import 'package:device_locker/device_locker.dart';
import 'package:permission_handler/permission_handler.dart';

class DeviceOwnerHome extends StatefulWidget {
  @override
  _DeviceOwnerHomeState createState() => _DeviceOwnerHomeState();
}

class _DeviceOwnerHomeState extends State<DeviceOwnerHome> {
  final TextEditingController _nameController = TextEditingController();
  final TextEditingController _emiController = TextEditingController();
  final TextEditingController _adharController = TextEditingController();
  final TextEditingController _totalAmtController = TextEditingController();
  final TextEditingController _phoneController = TextEditingController();
  final TextEditingController _downPayment = TextEditingController();

  var sellectedDuration;
  String? deviceName;
  String? deviceModel;
  String? imeiNumber;
  bool isRegistered = false;
  bool isLoading = false;
  String? fcmToken;
  final _formKey = GlobalKey<FormState>();

  @override
  void initState() {
    super.initState();
    _configureFirebaseMessaging();
    _getDeviceDetails();
  }

  void _configureFirebaseMessaging() {
    FirebaseMessaging.instance.getToken().then((token) {
      print('FCM Token: $token');
      setState(() {
        fcmToken = token;
      });
    }).catchError((e) {
      FirebaseCrashlytics.instance.log("Failed to get FCM token: $e");
    });

    // Listen for token refresh
    FirebaseMessaging.instance.onTokenRefresh.listen((newToken) {
      print('FCM Token Refreshed: $newToken');
      setState(() {
        fcmToken = newToken;
      });
      _updateDeviceFcmToken(newToken); // Update token in backend
    });

    FirebaseMessaging.onMessage.listen((RemoteMessage message) {
      if (message.notification != null) {
        _handleDeviceAction(message.notification!.body);
      }
    });

    FirebaseMessaging.onMessageOpenedApp.listen((RemoteMessage message) {
      if (message.notification != null) {
        _handleDeviceAction(message.notification!.body);
      }
    });
  }

  Future<void> _updateDeviceFcmToken(String newToken) async {
    try {
      final response = await http.post(
        Uri.parse(
            'https://morning-rounded-care.glitch.me/api/device/update-token'),
        headers: {'Content-Type': 'application/json'},
        body: jsonEncode({'imei': imeiNumber, 'fcmToken': newToken}),
      );

      if (response.statusCode == 200) {
        print('FCM Token updated successfully on the server');
      } else {
        print(
            'Failed to update FCM Token on the server: ${response.statusCode}');
      }
    } catch (e) {
      print('Error updating FCM Token: $e');
    }
  }

  void _handleDeviceAction(String? action) {
    if (action == null) return;

    final splitCommand = action.split(' ');
    final command = splitCommand[0].toLowerCase();
    final pin = splitCommand.length > 1 ? splitCommand[1] : null;

    switch (command) {
      case "lock":
        if (pin != null) {
          _lockDevice(pin);
        } else {
          print("PIN is required to lock the device.");
        }
        break;
      case "unlock":
        _unlockDevice();
        break;
      default:
        print("Unknown action received: $command");
        break;
    }
  }

  Future<void> _lockDevice(String pin) async {
    try {
      await DeviceLocker.lockDevice(pin);
      _showDialog("Device Locked", "Your device is now locked.");
    } catch (e) {
      print("Failed to lock device: $e");
      _showDialog("Error", "Failed to lock device.");
    }
  }

  Future<void> _unlockDevice() async {
    try {
      await DeviceLocker.unlockDevice();
      _showDialog("Device Unlocked", "Your device is now unlocked.");
    } catch (e) {
      print("Failed to unlock device: $e");
      _showDialog("Error", "Failed to unlock device.");
    }
  }

  void _showDialog(String title, String message) {
    showDialog(
      context: context,
      builder: (BuildContext context) {
        return AlertDialog(
          title: Text(title),
          content: Text(message),
          actions: <Widget>[
            TextButton(
              child: Text('OK'),
              onPressed: () {
                Navigator.of(context).pop();
              },
            ),
          ],
        );
      },
    );
  }

  Future<void> _getDeviceInfo() async {
    DeviceInfoPlugin deviceInfo = DeviceInfoPlugin();
    String name = '';
    String model = '';
    String imei = '';

    if (Theme.of(context).platform == TargetPlatform.android) {
      AndroidDeviceInfo androidInfo = await deviceInfo.androidInfo;
      name = androidInfo.device;
      model = androidInfo.model;
      imei = (await DeviceImei().getDeviceImei())!;
    } else if (Theme.of(context).platform == TargetPlatform.iOS) {
      IosDeviceInfo iosInfo = await deviceInfo.iosInfo;
      name = iosInfo.name;
      model = iosInfo.model;
      imeiNumber = imei;
    }

    setState(() {
      deviceName = name;
      deviceModel = model;
      imeiNumber = imei;
    });
  }

  Future<void> _getDeviceDetails() async {
    setState(() {
      isLoading = true;
    });

    if (await Permission.phone.request().isGranted) {
      try {
        await _getDeviceInfo();
        // deviceName = await DeviceInformation.deviceName;
        // deviceModel = await DeviceInformation.deviceModel;
        // imeiNumber = await DeviceInformation.deviceIMEINumber;

        final response = await http.get(
          Uri.parse(
              'https://morning-rounded-care.glitch.me/api/device/$imeiNumber'),
        );

        if (response.statusCode == 200) {
          setState(() {
            isRegistered = true;
            isLoading = false;
          });
        } else {
          setState(() {
            isRegistered = false;
            isLoading = false;
          });
        }
      } catch (e) {
        _showDialog("Error", "Failed to get device details: $e");
        print("Error $e");
        setState(() {
          isLoading = false;
        });
      }
    } else {
      _showDialog("Permission Denied",
          "Phone state permission is required to get device details.");
      setState(() {
        isLoading = false;
      });
    }
  }

  void _submitDetails() async {
    print(
        'xyz  $imeiNumber $fcmToken ${_nameController.text} $deviceModel  $deviceName ${_adharController.text} $sellectedDuration ${_emiController.text} ${_totalAmtController.text}');
    final response = await http.post(
      Uri.parse('https://morning-rounded-care.glitch.me/api/device/register'),
      headers: <String, String>{
        'Content-Type': 'application/json; charset=UTF-8',
      },
      body: jsonEncode(<String, dynamic>{
        'imei': imeiNumber,
        'fcmToken': fcmToken, // If applicable
        'ownerName': _nameController.text,
        'emiAmount': double.parse(_emiController.text),
        'deviceName': deviceName,
        'deviceModel': deviceModel,
        'totalAmount': double.parse(_totalAmtController.text),
        'adhar': _adharController.text,
        'emiDuration': sellectedDuration,
      }),
    );
    if (response.statusCode == 201) {
      print('Device registered successfully');
      _getDeviceDetails();
    } else {
      print('Failed to register device: ${response.body}');
    }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: Colors.teal,
      appBar: AppBar(
        title: const Text(
          'XYZ Emi App',
          style: TextStyle(fontWeight: FontWeight.bold),
        ),
      ),
      body: isLoading
          ? Center(child: CircularProgressIndicator())
          : Container(
              height: MediaQuery.of(context).size.height,
              child: SingleChildScrollView(
                padding: const EdgeInsets.all(16.0),
                child: Center(
                  child: Card(
                    elevation: 8,
                    shape: RoundedRectangleBorder(
                      borderRadius: BorderRadius.circular(15),
                    ),
                    child: Padding(
                      padding: const EdgeInsets.all(16.0),
                      child: isRegistered
                          ? DeviceInfoCard(
                              deviceName: deviceName,
                              deviceModel: deviceModel,
                              imeiNumber: imeiNumber)
                          : RegistrationForm(
                              downPayment: _downPayment,
                              contact: _phoneController,
                              nameController: _nameController,
                              emiController: _emiController,
                              adharController: _adharController,
                              totalAmtController: _totalAmtController,
                              deviceName: deviceName,
                              deviceModel: deviceModel,
                              imeiNumber: imeiNumber,
                              selectedDuration: sellectedDuration,
                              onDurationChanged: (value) {
                                setState(() {
                                  sellectedDuration = value;
                                });
                              },
                              onSubmit: _submitDetails,
                            ),
                    ),
                  ),
                ),
              ),
            ),
    );
  }
}
