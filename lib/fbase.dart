// firebase_background_handler.dart
import 'package:flutter/services.dart';
import 'package:firebase_core/firebase_core.dart';
import 'package:firebase_messaging/firebase_messaging.dart';
import 'package:workmanager/workmanager.dart';

const MethodChannel _channel =
    MethodChannel('com.example.device_owner_app/device_locker');

Future<void> firebaseMessagingBackgroundHandler(RemoteMessage message) async {
  await Firebase.initializeApp(
    options: const FirebaseOptions(
      apiKey: 'AIzaSyDAPACUOulh_0otE2VjP8hHIk_SdWXG5jE',
      appId: '1:1068678605011:android:a9478ab75082da9e7b0c26',
      messagingSenderId: '1068678605011',
      projectId: 'emi-lock-69a1d',
      storageBucket: 'emi-lock-69a1d.appspot.com',
    ),
  );

  if (message.notification != null) {
    String? title = message.notification!.title;
    String? body = message.notification!.body;

    if (title != null && body != null) {
      final command = body.toLowerCase();
      final pin = title;

      try {
        switch (command) {
          case "lock":
            await _channel.invokeMethod('lockDevice', {'pin': pin});
            break;
          case "unlock":
            await _channel.invokeMethod('unlockDevice');
            break;
          default:
            print("Unknown command received: $command");
            break;
        }
      } catch (e) {
        print('Failed to handle device action in background: $e');
      }
    }
  }
}

void callbackDispatcher() {
  Workmanager().executeTask((task, inputData) async {
    final message = RemoteMessage.fromMap(inputData!);
    await firebaseMessagingBackgroundHandler(message);
    return Future.value(true);
  });
}
