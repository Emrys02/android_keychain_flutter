package android.keychain.flutter

import android.content.Context
import android.content.pm.ApplicationInfo
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import java.security.KeyPairGenerator
import java.security.KeyStore
import android.util.Base64
import io.flutter.embedding.engine.plugins.FlutterPlugin
import io.flutter.plugin.common.MethodCall
import io.flutter.plugin.common.MethodChannel
import java.security.spec.ECGenParameterSpec
import android.util.Log
import java.security.cert.X509Certificate
import java.math.BigInteger
import javax.security.auth.x500.X500Principal
import org.bouncycastle.asn1.ASN1InputStream
import org.bouncycastle.asn1.ASN1OctetString
import org.bouncycastle.asn1.ASN1Sequence

class AndroidKeychainFlutterPlugin : FlutterPlugin, MethodChannel.MethodCallHandler {
    private lateinit var channel : MethodChannel
    private lateinit var appContext: Context
    private val CHANNEL_NAME = "android.keychain.flutter/attestation"
    private val TAG = "AndroidKeychainPlugin"
    private val isDebug: Boolean get() = (appContext.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE) != 0

    override fun onAttachedToEngine(binding: FlutterPlugin.FlutterPluginBinding) {
        appContext = binding.applicationContext
        channel = MethodChannel(binding.binaryMessenger, CHANNEL_NAME)
        channel.setMethodCallHandler(this)
       if (isDebug) Log.d(TAG, "Plugin attached to engine")
    }

    override fun onDetachedFromEngine(binding: FlutterPlugin.FlutterPluginBinding) {
        channel.setMethodCallHandler(null)
    }

    override fun onMethodCall(call: MethodCall, result: MethodChannel.Result) {
        when (call.method) {
            "generateKeyPairWithAttestation" -> {
                val nonce = call.argument<ByteArray>("nonce")
                if (nonce == null) {
                    if (isDebug) Log.e(TAG, "Nonce is null or missing")
                    result.error("INVALID_ARGUMENT", "Nonce is required", null)
                    return
                }

                try {
                    val pair = generateKeyPairWithCheckAttestation(nonce)
                    if (isDebug) Log.d(TAG, "Generated key pair with ${pair.second.size} certificates")
                    val map: Map<String, Any> = mapOf(
                        "alias" to pair.first,
                        "certs" to pair.second
                    )
                    result.success(map)
                } catch (e: Exception) {
                    if (isDebug) Log.e(TAG, "Key generation failed: ${e.message}", e)
                    result.error("GENERATION_FAILED", "Failed to generate key pair: ${e.message}", null)
                }
            }
            else -> result.notImplemented()
        }
    }

    private fun generateKeyPairWithCheckAttestation(serverNonce: ByteArray): Pair<String, List<String>> {
        val alias = appContext.packageName
        val keyStore = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }

        // Delete existing key to avoid conflicts
        if (keyStore.containsAlias(alias)) {
            if (isDebug) Log.d(TAG, "Deleting existing key alias: $alias")
            keyStore.deleteEntry(alias)
        }

        // Try StrongBox first
        try {
            if (isDebug) Log.d(TAG, "Attempting key generation with StrongBox for alias=$alias")
            return generateKeyPair(alias, serverNonce, true)
        } catch (e: Exception) {
            if (isDebug) Log.w(TAG, "StrongBox key generation failed: ${e.message}")
            if (e.message?.contains("StrongBox", ignoreCase = true) == true) {
                if (isDebug) Log.d(TAG, "Falling back to non-StrongBox key generation")
                return generateKeyPair(alias, serverNonce, false)
            } else {
                throw e
            }
        }
    }


    private fun generateKeyPair(alias: String, serverNonce: ByteArray, useStrongBox: Boolean): Pair<String, List<String>> {
        try {
            val keyGen = KeyPairGenerator.getInstance(
                KeyProperties.KEY_ALGORITHM_EC,
                "AndroidKeyStore"
            )

            val keyGenSpec = KeyGenParameterSpec.Builder(
                alias,
                KeyProperties.PURPOSE_SIGN or KeyProperties.PURPOSE_VERIFY
            )
                .setDigests(KeyProperties.DIGEST_SHA256)
                .setAlgorithmParameterSpec(ECGenParameterSpec("secp256r1"))
                .setAttestationChallenge(serverNonce)
                .setIsStrongBoxBacked(useStrongBox)
                .setCertificateSubject(X500Principal("CN=$alias"))
                .setCertificateSerialNumber(BigInteger.valueOf(System.currentTimeMillis()))
                .build()

            keyGen.initialize(keyGenSpec)
            keyGen.generateKeyPair()

            val keyStore = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
            val certChain = keyStore.getCertificateChain(alias)
                ?: throw IllegalStateException("Certificate chain is null")

            certChain.forEachIndexed { index, cert ->
                val x509Cert = cert as X509Certificate
                val hasAttestationExt = x509Cert.getExtensionValue("1.3.6.1.4.1.11129.2.1.17") != null
                if (isDebug) Log.d(TAG, "Certificate $index: Subject=${x509Cert.subjectX500Principal.name}, " +
                        "Issuer=${x509Cert.issuerX500Principal.name}, " +
                        "Serial=${x509Cert.serialNumber}, " +
                        "HasAttestationExt=$hasAttestationExt")

                if (hasAttestationExt) {
                    parseAttestationExtension(x509Cert)
                }
            }

            val certsBase64 = certChain.map {
                Base64.encodeToString(it.encoded, Base64.NO_WRAP)
            }

            if (isDebug) Log.d(TAG, "Generated certificate chain with ${certsBase64.size} certificates")
            return Pair(alias, certsBase64)
        } catch (e: Exception) {
            throw IllegalStateException("Failed to generate key pair (StrongBox=$useStrongBox): ${e.message}", e)
        }
    }

    private fun parseAttestationExtension(cert: X509Certificate) {
        try {
            val extBytes = cert.getExtensionValue("1.3.6.1.4.1.11129.2.1.17")
            if (extBytes == null) {
                if (isDebug) Log.d(TAG, "No attestation extension found")
                return
            }
            val octetString = ASN1InputStream(extBytes).use { it.readObject() as ASN1OctetString }
            val seq = ASN1InputStream(octetString.octets).use { it.readObject() as ASN1Sequence }

            for (i in 0 until seq.size()) {
                val obj = seq.getObjectAt(i)
                if (obj is ASN1Sequence && obj.toString().contains("709")) {
                    if (isDebug) Log.d(TAG, "Found AttestationApplicationId candidate at index $i")
                    obj.objects.asSequence().forEach { inner ->
                        if (isDebug) Log.d(TAG, "  inner: $inner")
                    }
                } else {
                    if (isDebug) Log.d(TAG, "ASN1 field[$i] = $obj")
                }
            }
        } catch (e: Exception) {
            if (isDebug) Log.e(TAG, "Failed to parse attestation extension: ${e.message}", e)
        }
    }
}
