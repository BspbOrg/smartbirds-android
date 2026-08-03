package org.bspb.smartbirds.pro.prefs

import org.bspb.smartbirds.pro.backend.SecureCookieStore

/**
 * One-time move of legacy plaintext preferences into the encrypted stores.
 * Guarded by a done-flag persisted in [flagStore] — a store that logout does NOT
 * clear — so it runs at most once. Keeping the flag out of [userStore] (which is
 * wiped on logout) prevents the migration from re-running after every logout.
 */
class LegacyPrefsMigration(
    private val userStore: SecureStore,
    private val cookieStore: SecureStore,
    private val flagStore: SecureStore,
) {
    fun migrateIfNeeded(source: LegacyPrefsSource) {
        if (flagStore.getString(KEY_MIGRATION_DONE) == "true") return

        for ((key, value) in source.readUserValues()) {
            userStore.putString(key, value)
        }
        source.readCookies()?.let { cookieStore.putString(SecureCookieStore.KEY_COOKIES, it) }

        source.clear()
        flagStore.putString(KEY_MIGRATION_DONE, "true")
    }

    companion object {
        const val KEY_MIGRATION_DONE = "sb_secure_migration_done"
    }
}
