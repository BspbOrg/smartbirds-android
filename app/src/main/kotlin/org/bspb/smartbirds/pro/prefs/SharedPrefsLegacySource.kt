package org.bspb.smartbirds.pro.prefs

import android.content.Context
import androidx.preference.PreferenceManager

/**
 * Reads the pre-SB-C06 plaintext storage:
 *  - "UserPrefs" MODE_PRIVATE file (all string keys + isAuthenticated boolean)
 *  - default shared prefs "PREF_COOKIES" StringSet
 * clear() wipes both so nothing plaintext remains.
 */
class SharedPrefsLegacySource(context: Context) : LegacyPrefsSource {

    private val app = context.applicationContext
    private val legacyUser = app.getSharedPreferences(LEGACY_USER_FILE, Context.MODE_PRIVATE)
    private val defaultPrefs = PreferenceManager.getDefaultSharedPreferences(app)

    override fun readUserValues(): Map<String, String> {
        val out = HashMap<String, String>()
        for ((key, value) in legacyUser.all) {
            when (value) {
                is String -> out[key] = value
                is Boolean -> out[key] = value.toString()
                else -> { /* ignore unknown types */ }
            }
        }
        return out
    }

    override fun readCookies(): String? {
        val set = defaultPrefs.getStringSet(PREF_COOKIES, null) ?: return null
        if (set.isEmpty()) return null
        return set.joinToString(COOKIE_DELIMITER)
    }

    override fun clear() {
        legacyUser.edit().clear().apply()
        defaultPrefs.edit().remove(PREF_COOKIES).apply()
    }

    companion object {
        const val LEGACY_USER_FILE = "UserPrefs"
        const val PREF_COOKIES = "PREF_COOKIES"
        const val COOKIE_DELIMITER = "\n"
    }
}
