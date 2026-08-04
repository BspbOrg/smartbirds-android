package org.bspb.smartbirds.pro.prefs

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/**
 * AES-256-GCM using a non-exportable key in AndroidKeyStore.
 * Ciphertext layout: [12-byte IV][GCM ciphertext+tag].
 */
class KeystoreCrypto(private val keyAlias: String) {

    fun encrypt(plaintext: ByteArray): ByteArray {
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, getOrCreateKey())
        val iv = cipher.iv // 12 bytes for GCM
        val ciphertext = cipher.doFinal(plaintext)
        return iv + ciphertext
    }

    fun decrypt(data: ByteArray): ByteArray {
        require(data.size > IV_SIZE) { "ciphertext too short" }
        val iv = data.copyOfRange(0, IV_SIZE)
        val ciphertext = data.copyOfRange(IV_SIZE, data.size)
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.DECRYPT_MODE, getKeyOrThrow(), GCMParameterSpec(TAG_BITS, iv))
        return cipher.doFinal(ciphertext)
    }

    fun deleteKey() {
        keyStore().deleteEntry(keyAlias)
    }

    private fun keyStore(): KeyStore =
        KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }

    private fun getKeyOrThrow(): SecretKey {
        val entry = keyStore().getEntry(keyAlias, null) as? KeyStore.SecretKeyEntry
            ?: throw java.security.KeyStoreException("missing key: $keyAlias")
        return entry.secretKey
    }

    @Synchronized
    private fun getOrCreateKey(): SecretKey {
        val ks = keyStore()
        (ks.getEntry(keyAlias, null) as? KeyStore.SecretKeyEntry)?.let { return it.secretKey }
        val generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEYSTORE)
        generator.init(
            KeyGenParameterSpec.Builder(
                keyAlias,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT,
            )
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256)
                .build()
        )
        return generator.generateKey()
    }

    companion object {
        private const val ANDROID_KEYSTORE = "AndroidKeyStore"
        private const val TRANSFORMATION = "AES/GCM/NoPadding"
        private const val IV_SIZE = 12
        private const val TAG_BITS = 128
    }
}
