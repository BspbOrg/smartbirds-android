package org.bspb.smartbirds.pro.prefs

import android.content.Context
import android.util.Base64

/**
 * SecureStore that stores base64(KeystoreCrypto.encrypt(utf8(value))) in a private
 * SharedPreferences file. If the key is missing/invalidated or a value fails to decrypt
 * (corruption), the store wipes itself + the Keystore key and invokes [onReset]; reads
 * then return their default. Wiping the token/password forces re-login on the next request.
 */
class EncryptedSecureStore(
    context: Context,
    fileName: String,
    keyAlias: String,
    private val onReset: (() -> Unit)? = null,
) : SecureStore {

    private val prefs = context.applicationContext.getSharedPreferences(fileName, Context.MODE_PRIVATE)
    private val crypto = KeystoreCrypto(keyAlias)

    override fun getString(key: String, default: String?): String? {
        val stored = prefs.getString(key, null) ?: return default
        return try {
            String(crypto.decrypt(Base64.decode(stored, Base64.NO_WRAP)), Charsets.UTF_8)
        } catch (e: Exception) {
            reset()
            default
        }
    }

    override fun putString(key: String, value: String?) {
        if (value == null) {
            remove(key)
            return
        }
        try {
            val encoded = Base64.encodeToString(
                crypto.encrypt(value.toByteArray(Charsets.UTF_8)), Base64.NO_WRAP
            )
            prefs.edit().putString(key, encoded).apply()
        } catch (e: Exception) {
            reset()
        }
    }

    override fun remove(key: String) {
        prefs.edit().remove(key).apply()
    }

    override fun clear() {
        prefs.edit().clear().apply()
    }

    override fun contains(key: String): Boolean = prefs.contains(key)

    private fun reset() {
        try {
            prefs.edit().clear().commit()
        } catch (ignored: Throwable) {
        }
        try {
            crypto.deleteKey()
        } catch (ignored: Throwable) {
        }
        onReset?.invoke()
    }
}
