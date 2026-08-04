package org.bspb.smartbirds.pro.backend

import android.content.Context
import org.bspb.smartbirds.pro.prefs.EncryptedSecureStore
import org.bspb.smartbirds.pro.prefs.SecureStore

/**
 * Process-wide encrypted storage for persisted Set-Cookie values.
 * Cookies are stored as one delimiter-joined string under KEY_COOKIES.
 */
object SecureCookieStore {
    const val KEY_COOKIES = "cookies"
    private const val COOKIE_FILE = "UserPrefsCookies"
    private const val COOKIE_KEY_ALIAS = "sb_cookie_store_key"
    private const val DELIMITER = "\n"

    @Volatile
    private var store: SecureStore? = null

    fun init(context: Context) {
        if (store == null) {
            synchronized(this) {
                if (store == null) {
                    store = EncryptedSecureStore(
                        context.applicationContext, COOKIE_FILE, COOKIE_KEY_ALIAS
                    )
                }
            }
        }
    }

    fun get(): SecureStore =
        checkNotNull(store) { "SecureCookieStore.init(context) must be called first" }

    fun readCookies(): List<String> {
        val joined = get().getString(KEY_COOKIES) ?: return emptyList()
        if (joined.isEmpty()) return emptyList()
        return joined.split(DELIMITER)
    }

    fun writeCookies(cookies: Set<String>) {
        get().putString(KEY_COOKIES, cookies.joinToString(DELIMITER))
    }

    fun clear() {
        store?.remove(KEY_COOKIES)
    }
}
