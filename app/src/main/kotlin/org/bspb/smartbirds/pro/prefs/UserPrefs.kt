package org.bspb.smartbirds.pro.prefs

import android.content.Context

class UserPrefs(context: Context) {

    companion object {
        const val KEY_IS_AUTHENTICATED = "isAuthenticated"
        const val KEY_AUTH_TOKEN = "authToken"
        const val KEY_USERNAME = "username"
        const val KEY_PASSWORD = "password"
        const val KEY_USER_ID = "userId"
        const val KEY_FIRST_NAME = "firstName"
        const val KEY_LAST_NAME = "lastName"
        const val KEY_EMAIL = "email"
        const val KEY_BG_ATLAS_CELLS = "bgAtlasCells"

        private const val SECURE_FILE = "UserPrefsSecure"
        private const val SECURE_KEY_ALIAS = "sb_user_prefs_key"
        private const val COOKIE_FILE = "UserPrefsCookies"
        private const val COOKIE_KEY_ALIAS = "sb_cookie_store_key"

        // Holds only the migration done-flag; never cleared on logout, so the
        // one-time migration is not re-run after every logout.
        private const val MIGRATION_FILE = "UserPrefsMigration"
    }

    private val store: SecureStore =
        EncryptedSecureStore(context, SECURE_FILE, SECURE_KEY_ALIAS)

    init {
        val cookieStore: SecureStore =
            EncryptedSecureStore(context, COOKIE_FILE, COOKIE_KEY_ALIAS)
        val flagStore: SecureStore = PlainPrefsStore(context, MIGRATION_FILE)
        LegacyPrefsMigration(store, cookieStore, flagStore)
            .migrateIfNeeded(SharedPrefsLegacySource(context))
    }

    fun clear() {
        store.clear()
    }

    fun isAuthenticated(): Boolean = store.getString(KEY_IS_AUTHENTICATED) == "true"

    fun setAuthenticated(isAuthenticated: Boolean) {
        store.putString(KEY_IS_AUTHENTICATED, isAuthenticated.toString())
    }

    fun getAuthToken(): String = store.getString(KEY_AUTH_TOKEN, "") ?: ""

    fun setAuthToken(authToken: String?) = store.putString(KEY_AUTH_TOKEN, authToken)

    fun getUsername(): String = store.getString(KEY_USERNAME, "") ?: ""

    fun setUsername(username: String?) = store.putString(KEY_USERNAME, username)

    fun getPassword(): String = store.getString(KEY_PASSWORD, "") ?: ""

    fun setPassword(password: String?) = store.putString(KEY_PASSWORD, password)

    fun getUserId(): String = store.getString(KEY_USER_ID, "") ?: ""

    fun setUserId(userId: String?) = store.putString(KEY_USER_ID, userId)

    fun getFirstName(): String = store.getString(KEY_FIRST_NAME, "") ?: ""

    fun setFirstName(firstName: String?) = store.putString(KEY_FIRST_NAME, firstName)

    fun getLastName(): String = store.getString(KEY_LAST_NAME, "") ?: ""

    fun setLastName(lastName: String?) = store.putString(KEY_LAST_NAME, lastName)

    fun getEmail(): String = store.getString(KEY_EMAIL, "") ?: ""

    fun setEmail(email: String?) = store.putString(KEY_EMAIL, email)

    fun getBgAtlasCells(): String = store.getString(KEY_BG_ATLAS_CELLS, "") ?: ""

    fun setBgAtlasCells(bgAtlasCells: String?) = store.putString(KEY_BG_ATLAS_CELLS, bgAtlasCells)
}
