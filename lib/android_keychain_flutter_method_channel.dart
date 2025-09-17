import 'dart:async';

import 'package:flutter/services.dart';

import 'android_keychain_flutter_platform_interface.dart';

/// An implementation of [AndroidKeychainFlutterPlatform] that uses method channels.
class MethodChannelAndroidKeychainFlutter extends AndroidKeychainFlutterPlatform {
  final MethodChannel _channel = const MethodChannel('android.keychain.flutter/attestation');

  @override
  Future<Map<String, Object?>> generateKeyPairWithAttestation(Uint8List nonce) async {
    final result = await _channel.invokeMethod<Map<dynamic, dynamic>>('generateKeyPairWithAttestation', <String, dynamic>{'nonce': nonce});

    if (result == null) {
      throw PlatformException(code: 'NULL_RESULT', message: 'Platform returned null');
    }

    // Convert dynamic map to Map<String, Object?>
    return Map<String, Object?>.from(result);
  }
}
