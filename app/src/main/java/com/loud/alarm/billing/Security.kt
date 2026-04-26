package com.loud.alarm.billing

import android.text.TextUtils
import android.util.Base64
import android.util.Log
import java.io.IOException
import java.security.InvalidKeyException
import java.security.KeyFactory
import java.security.NoSuchAlgorithmException
import java.security.PublicKey
import java.security.Signature
import java.security.SignatureException
import java.security.spec.InvalidKeySpecException
import java.security.spec.X509EncodedKeySpec

/**
 * Security-related methods. For a secure implementation, all of this code should be implemented on
 * a server that communicates with the application on the device.
 */
object Security {
    private const val TAG = "IABUtil/Security"
    private const val KEY_FACTORY_ALGORITHM = "RSA"
    private const val SIGNATURE_ALGORITHM = "SHA1withRSA"

    // The key is broken into chunks so automated hackers can't easily find it by searching your APK for giant base64 strings
    private val KEY_PARTS = arrayOf(
        "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAq9i",
        "CldXIfnutHiOelfBezdRhvn9ddPU4x0CWOvrEC6lkjTGr",
        "izg6T7EqyI/etS2a4T3Z5crBnWphd/eNUe/SmvAVFwIgF",
        "a7uTjd6Ow5EN8okL9tJClCjstFfb5ZvP5nfuiKER6C0fd",
        "TODJTsriIdcjIoDmuExmtTV0lKJE/U1uBgPJixDxWyDKe",
        "DvPGhyA3wFv6gl+CcigbQ7ZQwRkvB8Dty8AcWEnaG84np",
        "5G+5vnd0LKxSMt4HnTkcangTuOwwzSguGvMH4xQM2MZ4a",
        "PwmcyZM9qxGas0Tlsi7+MbCT0HiB7m/JNEWuUHYH1AUTF",
        "Xnhy0wvL/rTMXX69zTh5VUpQIDAQAB"
    )

    val BASE_64_ENCODED_PUBLIC_KEY: String
        get() = KEY_PARTS.joinToString("")

    /**
     * Verifies that the data was signed with the given signature
     *
     * @param base64PublicKey the base64-encoded public key to use for verifying.
     * @param signedData the signed JSON string (signed, not encrypted)
     * @param signature the signature for the data, signed with the private key
     */
    fun verifyPurchase(base64PublicKey: String, signedData: String, signature: String): Boolean {
        if (TextUtils.isEmpty(signedData) || TextUtils.isEmpty(base64PublicKey) ||
            TextUtils.isEmpty(signature)
        ) {
            Log.e(TAG, "Purchase verification failed: missing data.")
            return false
        }
        return try {
            val key = generatePublicKey(base64PublicKey)
            verify(key, signedData, signature)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to generate public key or verify signature.", e)
            false
        }
    }

    /**
     * Generates a PublicKey instance from a string containing the Base64-encoded public key.
     *
     * @param encodedPublicKey Base64-encoded public key
     * @throws IllegalArgumentException if encodedPublicKey is invalid
     */
    private fun generatePublicKey(encodedPublicKey: String): PublicKey {
        try {
            val decodedKey = Base64.decode(encodedPublicKey, Base64.DEFAULT)
            val keyFactory = KeyFactory.getInstance(KEY_FACTORY_ALGORITHM)
            return keyFactory.generatePublic(X509EncodedKeySpec(decodedKey))
        } catch (e: NoSuchAlgorithmException) {
            throw RuntimeException(e)
        } catch (e: InvalidKeySpecException) {
            Log.e(TAG, "Invalid key specification.")
            throw IllegalArgumentException(e)
        }
    }

    /**
     * Verifies that the signature from the server matches the computed signature on the data.
     * Returns true if the data is correctly signed.
     *
     * @param publicKey public key associated with the developer account
     * @param signedData signed data from server
     * @param signature server signature
     * @return true if the data and signature match
     */
    private fun verify(publicKey: PublicKey, signedData: String, signature: String): Boolean {
        val signatureBytes: ByteArray
        try {
            signatureBytes = Base64.decode(signature, Base64.DEFAULT)
        } catch (e: IllegalArgumentException) {
            Log.e(TAG, "Base64 decoding failed.")
            return false
        }
        try {
            val signatureAlgorithm = Signature.getInstance(SIGNATURE_ALGORITHM)
            signatureAlgorithm.initVerify(publicKey)
            signatureAlgorithm.update(signedData.toByteArray())
            if (!signatureAlgorithm.verify(signatureBytes)) {
                Log.e(TAG, "Signature verification failed.")
                return false
            }
            return true
        } catch (e: NoSuchAlgorithmException) {
            Log.e(TAG, "NoSuchAlgorithmException.", e)
        } catch (e: InvalidKeyException) {
            Log.e(TAG, "Invalid key specification.")
        } catch (e: SignatureException) {
            Log.e(TAG, "Signature exception.", e)
        }
        return false
    }
}
