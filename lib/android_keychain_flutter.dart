export 'android_keychain_flutter_platform_interface.dart';

import 'dart:typed_data';

import 'android_keychain_flutter_platform_interface.dart';

/// A Flutter plugin for generating attested key pairs using Android's KeyStore.
///
/// This class provides a high-level interface to the platform-specific implementation
/// for generating hardware-attested ECDSA key pairs.
class AndroidKeychainFlutter {
  /// Generates a new ECDSA P-256 key pair in the Android KeyStore and returns
  /// a hardware-attested certificate chain.
  ///
  /// The [nonce] is a challenge provided by a server to prevent replay attacks.
  /// It will be included in the attestation record of the certificate.
  ///
  /// The method attempts to use StrongBox for key generation if available, falling
  /// back to the standard hardware-backed keystore if not.
  ///
  /// Returns a [Future] that completes with a map containing:
  /// - `alias`: A [String] representing the alias of the generated key in the KeyStore.
  ///   This is typically the package name of the application.
  /// - `certs`: A `List<String>` of Base64-encoded X.509 certificates. The first
  ///   certificate is the leaf, containing the public key of the generated pair.
  ///   The rest of the list forms the chain of trust up to a Google root certificate.
  ///
  /// Throws a [PlatformException] if key generation fails.
  static Future<Map<String, Object?>> generateKeyPairWithAttestation(Uint8List nonce) {
    return AndroidKeychainFlutterPlatform.instance.generateKeyPairWithAttestation(nonce);
  }
}

