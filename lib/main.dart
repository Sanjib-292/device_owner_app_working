import 'dart:isolate';
import 'package:device_locker/device_locker.dart';
import 'package:device_owner_app/home.dart';
import 'package:flutter/material.dart';
import 'package:firebase_core/firebase_core.dart';
import 'package:firebase_crashlytics/firebase_crashlytics.dart';
import 'package:firebase_messaging/firebase_messaging.dart';
import 'package:flutter/services.dart';
import 'package:workmanager/workmanager.dart';

void callbackDispatcher() {
  Workmanager().executeTask((task, inputData) async {
    WidgetsFlutterBinding.ensureInitialized();
    if (Firebase.apps.isEmpty) {
      await Firebase.initializeApp();
    }

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
  if (Firebase.apps.isEmpty) {
    await Firebase.initializeApp();
  }

  if (message.notification != null) {
    String? title = message.notification!.title;
    String? body = message.notification!.body;
    final splitCommand = body!.split(' ');
    final command2 = splitCommand[0].toLowerCase();
    final pin2 = splitCommand.length > 1 ? splitCommand[1] : 'Test12';
    if (title != null && body != null) {
      final command = command2;
      final pin = pin2;

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

  if (Firebase.apps.isEmpty) {
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
  }

  FirebaseMessaging.onBackgroundMessage(firebaseMessagingBackgroundHandler);
  FlutterError.onError = FirebaseCrashlytics.instance.recordFlutterError;
  await Workmanager().initialize(
    callbackDispatcher,
    isInDebugMode: true,
  );
  await DeviceLocker.activateDeviceAdmin();
  runApp(DeviceOwnerApp());
}

class DeviceOwnerApp extends StatelessWidget {
  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      title: 'Device Owner App',
      theme: ThemeData(
        primarySwatch: Colors.blue,
        visualDensity: VisualDensity.adaptivePlatformDensity,
        textTheme: TextTheme(
          headlineMedium:
              TextStyle(fontSize: 32.0, fontWeight: FontWeight.bold),
          bodyLarge: TextStyle(fontSize: 18.0),
        ),
      ),
      home: DeviceOwnerHome(),
    );
  }
}
