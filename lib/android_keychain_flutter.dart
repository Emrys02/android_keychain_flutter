export 'android_keychain_flutter_platform_interface.dart';

import 'dart:typed_data';

import 'android_keychain_flutter_platform_interface.dart';

class AndroidKeychainFlutter {
  /// Generate an attested keypair using Android KeyStore (KeyMint).
  /// `nonce` must be provided (server-provided nonce recommended).
  /// Returns a map with "alias" (String) and "certs" (List<String> of Base64).
  static Future<Map<String, Object?>> generateKeyPairWithAttestation(Uint8List nonce) {
    return AndroidKeychainFlutterPlatform.instance.generateKeyPairWithAttestation(nonce);
  }
}

