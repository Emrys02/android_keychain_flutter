import 'dart:typed_data';

import 'package:plugin_platform_interface/plugin_platform_interface.dart';

import 'android_keychain_flutter_method_channel.dart';

abstract class AndroidKeychainFlutterPlatform extends PlatformInterface {
  AndroidKeychainFlutterPlatform() : super(token: _token);

  static final Object _token = Object();

  static AndroidKeychainFlutterPlatform _instance = MethodChannelAndroidKeychainFlutter();

  /// The default instance that will be used by the plugin.
  static AndroidKeychainFlutterPlatform get instance => _instance;

  /// Platform-specific implementations should set this with their own
  /// platform-specific class that extends [AndroidKeychainFlutterPlatform] when
  /// they register themselves.
  static set instance(AndroidKeychainFlutterPlatform instance) {
    PlatformInterface.verifyToken(instance, _token);
    _instance = instance;
  }

  /// Generate an attested keypair. `nonce` is required.
  /// Returns a map: {"alias": String, "certs": List(String (Base64))}
  Future<Map<String, Object?>> generateKeyPairWithAttestation(Uint8List nonce) {
    throw UnimplementedError('generateKeyPairWithAttestation() has not been implemented.');
  }
}
