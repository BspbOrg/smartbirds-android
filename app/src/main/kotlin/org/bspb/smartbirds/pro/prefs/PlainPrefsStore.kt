package org.bspb.smartbirds.pro.prefs

import android.content.Context

/**
 * Unencrypted [SecureStore] over a private SharedPreferences file. Intended for
 * non-secret bookkeeping (e.g. the migration done-flag) that must survive logout —
 * unlike the encrypted user/cookie stores, this file is never cleared on logout.
 */
class PlainPrefsStore(context: Context, fileName: String) : SecureStore {

    private val prefs =
        context.applicationContext.getSharedPreferences(fileName, Context.MODE_PRIVATE)

    override fun getString(key: String, default: String?): String? =
        prefs.getString(key, default)

    override fun putString(key: String, value: String?) {
        if (value == null) {
            remove(key)
            return
        }
        prefs.edit().putString(key, value).apply()
    }

    override fun remove(key: String) {
        prefs.edit().remove(key).apply()
    }

    override fun clear() {
        prefs.edit().clear().apply()
    }

    override fun contains(key: String): Boolean = prefs.contains(key)
}
