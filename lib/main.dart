import 'dart:isolate';

import 'package:device_locker/device_locker.dart';
import 'package:flutter/material.dart';
import 'package:firebase_core/firebase_core.dart';
import 'package:firebase_crashlytics/firebase_crashlytics.dart';
import 'package:firebase_messaging/firebase_messaging.dart';
import 'package:workmanager/workmanager.dart';

void callbackDispatcher() {
  Workmanager().executeTask((task, inputData) async {
    WidgetsFlutterBinding.ensureInitialized();
    await Firebase.initializeApp();

    final action = inputData?['action'] as String?;
    final pin = inputData?['pin'] as String?;
    print('Bg lockDevice called in isolate: ${Isolate.current.debugName}');
    try {
      switch (action) {
        case 'lock':
          await DeviceLocker.lockDevice(pin!);
          break;
        case 'unlock':
          await DeviceLocker.unlockDevice();
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
      apiKey: 'your-api-key',
      appId: 'your-app-id',
      messagingSenderId: 'your-messaging-sender-id',
      projectId: 'your-project-id',
      storageBucket: 'your-storage-bucket',
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
  await DeviceLocker.activateDeviceAdmin();
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
