# Android Keychain Flutter

A Flutter plugin for generating attested key pairs using Android's KeyStore.

This plugin provides a simple interface to generate a new key pair (ECDSA P-256) and receive a certificate chain that includes an attestation record. The attestation ensures that the key was generated in hardware-backed storage on a genuine Android device.

## Features

- **Hardware-Backed Key Generation:** Keys are generated and stored in the Android KeyStore, which provides hardware-level security.
- **Key Attestation:** Provides cryptographic proof that a key pair was generated on a specific device.
- **StrongBox Support:** Attempts to use StrongBox for key generation for the highest level of security, with a fallback to the standard hardware-backed keystore.
- **Simple API:** A single static method to generate the key pair and receive the attestation certificate chain.

## Getting Started

To use this plugin, add `android_keychain_flutter` as a [dependency in your `pubspec.yaml` file](https://flutter.dev/to/add-a-dependency).

```yaml
dependencies:
  android_keychain_flutter: ^1.0.0
```

Then, run `flutter pub get` to install the plugin.

## Usage

Here's an example of how to use the plugin to generate an attested key pair. You must provide a nonce, which should ideally be a challenge from your server to prevent replay attacks.

```dart
import 'dart:typed_data';
import 'package:android_keychain_flutter/android_keychain_flutter.dart';
import 'package:flutter/services.dart';

// ...

Future<void> generateAttestedKeyPair() async {
  try {
    // It is recommended to use a server-provided nonce.
    // For demonstration purposes, we'll use a random nonce.
    final nonce = Uint8List(16); // Replace with your server-provided nonce

    final result = await AndroidKeychainFlutter.generateKeyPairWithAttestation(nonce);

    final String alias = result['alias'] as String;
    final List<String> certs = (result['certs'] as List<dynamic>).cast<String>();

    print('Key pair generated with alias: $alias');
    print('Certificate chain contains ${certs.length} certificates.');

    // You can now send the certificate chain to your server for verification.
    // The first certificate in the list is the leaf certificate for the generated key.
  } on PlatformException catch (e) {
    print('Failed to generate key pair: ${e.message}');
  }
}
```

## Certificate Chain and Attestation

The `generateKeyPairWithAttestation` method returns a certificate chain as a list of Base64-encoded X.509 certificates. This chain can be used to verify the authenticity and properties of the generated key pair.

The certificate chain typically has the following structure:

1.  **Leaf Certificate:** The first certificate in the list is the leaf certificate, which contains the public key of the newly generated key pair. This certificate is signed by the next certificate in the chain.
2.  **Intermediate Certificate(s):** One or more intermediate certificates that establish a chain of trust.
3.  **Root Certificate:** The last certificate in the chain is the root certificate. For devices that pass Google's certification, this will be a Google hardware attestation root certificate.

The leaf certificate contains an **attestation extension** (OID `1.3.6.1.4.1.11129.2.1.17`) that holds the attestation record. This record includes:

- The **attestation challenge** (the nonce you provided).
- Information about the device's hardware and software.
- The properties of the generated key.

Your server can verify the certificate chain and parse the attestation extension to confirm that the key was generated in a secure environment on a genuine Android device.

## License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.
