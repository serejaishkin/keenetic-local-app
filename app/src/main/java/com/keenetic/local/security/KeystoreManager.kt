package com.keenetic.local.security

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import com.keenetic.local.util.AppLogger
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

class KeystoreManager {

    private val keyStore: KeyStore = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
    private val keyAlias = "keenetic_router_password"

    init {
        try {
            if (!keyStore.containsAlias(keyAlias)) {
                generateKey()
            }
        } catch (e: Exception) {
            AppLogger.w("Android Keystore is unavailable, using fallback storage", throwable = e)
        }
    }

    private fun generateKey() {
        try {
            val keyGenerator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore")
            keyGenerator.init(
                KeyGenParameterSpec.Builder(
                    keyAlias,
                    KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
                )
                    .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                    .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                    .setRandomizedEncryptionRequired(true)
                    .build()
            )
            keyGenerator.generateKey()
            AppLogger.d("Keystore key generated: $keyAlias")
        } catch (e: Exception) {
            AppLogger.w("Failed to generate Keystore key, fallback will be used", throwable = e)
        }
    }

    private fun getKey(): SecretKey? {
        return try {
            (keyStore.getEntry(keyAlias, null) as? SecretKey)
        } catch (e: Exception) {
            AppLogger.w("Failed to access Keystore key, fallback will be used", throwable = e)
            null
        }
    }

    fun encrypt(plaintext: String): String {
        val key = getKey()
        if (key == null) {
            return fallbackEncode(plaintext)
        }

        return try {
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(Cipher.ENCRYPT_MODE, key)
            val iv = cipher.iv
            val encrypted = cipher.doFinal(plaintext.toByteArray(Charsets.UTF_8))
            val combined = iv + encrypted
            Base64.encodeToString(combined, Base64.NO_WRAP)
        } catch (e: Exception) {
            AppLogger.w("Keystore encrypt failed, using fallback storage", throwable = e)
            fallbackEncode(plaintext)
        }
    }

    fun decrypt(encrypted: String): String {
        if (encrypted.startsWith("plain:")) {
            return fallbackDecode(encrypted.removePrefix("plain:"))
        }

        val key = getKey()
        if (key == null) {
            return fallbackDecode(encrypted)
        }

        return try {
            val combined = Base64.decode(encrypted, Base64.NO_WRAP)
            val iv = combined.copyOfRange(0, 12)
            val ciphertext = combined.copyOfRange(12, combined.size)
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(128, iv))
            String(cipher.doFinal(ciphertext), Charsets.UTF_8)
        } catch (e: Exception) {
            AppLogger.w("Keystore decrypt failed, trying fallback decode", throwable = e)
            fallbackDecode(encrypted)
        }
    }

    private fun fallbackEncode(value: String): String = "plain:${Base64.encodeToString(value.toByteArray(Charsets.UTF_8), Base64.NO_WRAP)}"

    private fun fallbackDecode(value: String): String = String(Base64.decode(value, Base64.NO_WRAP), Charsets.UTF_8)
}