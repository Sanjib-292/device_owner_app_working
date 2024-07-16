import 'package:firebase_core/firebase_core.dart';
import 'package:firebase_crashlytics/firebase_crashlytics.dart';
import 'package:flutter/material.dart';
import 'package:firebase_messaging/firebase_messaging.dart';
import 'package:flutter/services.dart';

Future<void> _firebaseMessagingBackgroundHandler(RemoteMessage message) async {
  await Firebase.initializeApp();
  if (message.notification != null) {
    String? command = message.notification!.body;
    if (command != null) {
      switch (command.toLowerCase()) {
        case "lock":
          try {
            await DeviceLocker.lockDevice();
          } catch (e) {
            print("Failed to lock device: $e");
          }
          break;
        case "unlock":
          try {
            await DeviceLocker.unlockDevice();
          } catch (e) {
            print("Failed to unlock device: $e");
          }
          break;
        default:
          break;
      }
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
  );
  FirebaseCrashlytics.instance.setCrashlyticsCollectionEnabled(true);
  FlutterError.onError = FirebaseCrashlytics.instance.recordFlutterError;
  runApp(MaterialApp(home: DeviceOwnerApp()));
}

class DeviceOwnerApp extends StatefulWidget {
  @override
  _DeviceOwnerAppState createState() => _DeviceOwnerAppState();
}

class _DeviceOwnerAppState extends State<DeviceOwnerApp> {
  // static const platform =
  //     MethodChannel('com.example.device_owner_app/device_locker');

  @override
  void initState() {
    super.initState();
    print('init');
    FirebaseMessaging.instance.getToken().then((token) {
      print('FCM Token: $token');
    }).catchError((e) {
      FirebaseCrashlytics.instance.log("Failed to get FCM token: $e");
    });
    _configureFirebaseMessaging();
  }

  void testCrash() {
    FirebaseCrashlytics.instance.crash();
  }

  void _configureFirebaseMessaging() {
    FirebaseMessaging.onMessage.listen((RemoteMessage message) {
      if (message.notification != null) {
        String? command = message.notification!.body;
        if (command != null) {
          _handleDeviceAction(command);
        }
      }
    });

    FirebaseMessaging.onMessageOpenedApp.listen((RemoteMessage message) {
      if (message.notification != null) {
        String? command = message.notification!.body;
        if (command != null) {
          _handleDeviceAction(command);
        }
      }
    });

    FirebaseMessaging.onBackgroundMessage(_firebaseMessagingBackgroundHandler);
  }

  void _handleDeviceAction(String action) {
    print('Action: $action');
    switch (action.toLowerCase()) {
      case "lock":
        _lockDevice();
        break;
      case "unlock":
        _unlockDevice();
        break;
      default:
        print("Unknown action received: $action");
        break;
    }
  }

  Future<void> _lockDevice() async {
    try {
      await DeviceLocker.lockDevice();
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

  //static FirebaseAnalytics analytics = FirebaseAnalytics.instance;
  // static FirebaseAnalyticsObserver observer =
  //     FirebaseAnalyticsObserver(analytics: analytics);

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
                  _lockDevice();
                },
                child: const Text('Lock Device'),
              ),
              const SizedBox(height: 10.0),
              ElevatedButton(
                onPressed: () {
                  _unlockDevice();
                },
                child: const Text('Unlock Device'),
              ),
            ],
          ),
        ),
      ),
    );
  }
}

class DeviceLocker {
  static const MethodChannel _channel =
      MethodChannel('com.example.device_owner_app/device_locker');

  static Future<void> lockDevice() async {
    try {
      await _channel.invokeMethod('lockDevice', {'pin': '123455'});
    } catch (e) {
      throw Exception('Failed to lock device: $e');
    }
  }

  static Future<void> unlockDevice() async {
    try {
      await _channel.invokeMethod('unlockDevice');
    } catch (e) {
      throw Exception('Failed to unlock device: $e');
    }
  }
}
