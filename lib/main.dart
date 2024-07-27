import 'dart:isolate';
import 'package:device_owner_app/device_locker.dart';
import 'package:firebase_core/firebase_core.dart';
import 'package:firebase_crashlytics/firebase_crashlytics.dart';
import 'package:flutter/material.dart';
import 'package:firebase_messaging/firebase_messaging.dart';
import 'package:flutter/services.dart';
import 'package:workmanager/workmanager.dart';

const MethodChannel _channel =
    MethodChannel('com.example.device_owner_app/device_locker');

void callbackDispatcher() {
  Workmanager().executeTask((task, inputData) async {
    WidgetsFlutterBinding.ensureInitialized();
    await Firebase.initializeApp();

    final MethodChannel _channel =
        MethodChannel('com.example.device_owner_app/device_locker');

    final action = inputData?['action'] as String?;
    final pin = inputData?['pin'] as String?;
    print('Bg lockDevice called in isolate: ${Isolate.current.debugName}');
    try {
      switch (action) {
        case 'lock':
          await _channel.invokeMethod('lockDevice', {'pin': pin});
          break;
        case 'unlock':
          await _channel.invokeMethod('unlockDevice');
          break;
        default:
          print('Unknown command received: $action');
          break;
      }
    } catch (e) {
      print('Failed to handle device action in background: $e');
    }

    return Future.value(true);
  });
}

Future<void> firebaseMessagingBackgroundHandler(RemoteMessage message) async {
  await Firebase.initializeApp();

  if (message.notification != null) {
    String? title = message.notification!.title;
    String? body = message.notification!.body;

    if (title != null && body != null) {
      final command = body.toLowerCase();
      final pin = title;

      Workmanager().registerOneOffTask(
        DateTime.now().toString(),
        'backgroundTask',
        inputData: {
          'action': command,
          'pin': pin,
        },
      ).then((v) {
        print('done Background>>>>>>>>>>>');
      }).catchError((e) {
        print('error is $e');
      });
    }
  }
}

void main() async {
  WidgetsFlutterBinding.ensureInitialized();
  await Firebase.initializeApp(
    options: const FirebaseOptions(
      apiKey: 'AIzaSyDAPACUOulh_0otE2VjP8hHIk_SdWXG5jE',
      appId: '1:1068678605011:android:a9478ab75082da9e7b0c26',
      messagingSenderId: '1068678605011',
      projectId: 'emi-lock-69a1d',
      storageBucket: 'emi-lock-69a1d.appspot.com',
    ),
  ).then((e) {
    print('E is $e');
  }).catchError((er) {
    print('er is $er');
  });
  FirebaseMessaging.onBackgroundMessage(firebaseMessagingBackgroundHandler);
  FlutterError.onError = FirebaseCrashlytics.instance.recordFlutterError;
  await Workmanager().initialize(
    callbackDispatcher,
    isInDebugMode: true,
  );
  runApp(MaterialApp(home: DeviceOwnerApp()));
}

class DeviceOwnerApp extends StatefulWidget {
  @override
  _DeviceOwnerAppState createState() => _DeviceOwnerAppState();
}

class _DeviceOwnerAppState extends State<DeviceOwnerApp> {
  @override
  void initState() {
    super.initState();
    FirebaseMessaging.instance.getToken().then((token) {
      print('tok $token');
    }).catchError((e) {
      FirebaseCrashlytics.instance.log("Failed to get FCM token: $e");
    });
    _configureFirebaseMessaging();
  }

  void _configureFirebaseMessaging() {
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

    // FirebaseMessaging.onBackgroundMessage(firebaseMessagingBackgroundHandler);
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
      await DeviceLockService.lockDevice(pin);
      _showDialog("Device Locked", "Your device is now locked.");
    } catch (e) {
      print("Failed to lock device: $e");
      _showDialog("Error", "Failed to lock device.");
    }
  }

  Future<void> _unlockDevice() async {
    try {
      await DeviceLockService.unlockDevice();
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

  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      title: 'Device Owner App',
      home: Scaffold(
        appBar: AppBar(
          title: const Text('Device Owner App'),
        ),
        body: Center(
          child: Column(
            mainAxisAlignment: MainAxisAlignment.center,
            children: <Widget>[
              const Text(
                'Device Owner App',
                style: TextStyle(fontSize: 24.0),
              ),
              const SizedBox(height: 20.0),
              ElevatedButton(
                onPressed: () {
                  _lockDevice('123456'); // Test with a sample PIN
                },
                child: const Text('Lock Device'),
              ),
              const SizedBox(height: 10.0),
              ElevatedButton(
                onPressed: _unlockDevice,
                child: const Text('Unlock Device'),
              ),
            ],
          ),
        ),
      ),
    );
  }
}

class DeviceLockService {
  static const platform =
      MethodChannel('com.example.device_owner_app/device_locker');

  static Future<void> lockDevice(String pin) async {
    print(
        'Foreground lockDevice called in isolate: ${Isolate.current.debugName}');

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
