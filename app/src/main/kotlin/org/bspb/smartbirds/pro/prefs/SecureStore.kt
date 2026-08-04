package org.bspb.smartbirds.pro.prefs

/**
 * Synchronous string key/value storage whose values are encrypted at rest.
 * Kept synchronous so existing call sites (UserPrefs, OkHttp interceptors) are unchanged.
 */
interface SecureStore {
    fun getString(key: String, default: String? = null): String?
    fun putString(key: String, value: String?)
    fun remove(key: String)
    fun clear()
    fun contains(key: String): Boolean
}

/**
 * Read-only view over the pre-existing plaintext preferences, used once during migration.
 * readUserValues() returns already-normalized string values (booleans as "true"/"false").
 * readCookies() returns the joined cookie string (see SecureCookieStore.join), or null if none.
 */
interface LegacyPrefsSource {
    fun readUserValues(): Map<String, String>
    fun readCookies(): String?
    fun clear()
}
