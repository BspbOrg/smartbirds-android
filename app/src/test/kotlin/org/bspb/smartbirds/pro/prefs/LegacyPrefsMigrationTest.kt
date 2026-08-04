package org.bspb.smartbirds.pro.prefs

import org.bspb.smartbirds.pro.backend.SecureCookieStore
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

private class FakeSecureStore : SecureStore {
    val map = HashMap<String, String>()
    override fun getString(key: String, default: String?) = map[key] ?: default
    override fun putString(key: String, value: String?) {
        if (value == null) map.remove(key) else map[key] = value
    }
    override fun remove(key: String) { map.remove(key) }
    override fun clear() { map.clear() }
    override fun contains(key: String) = map.containsKey(key)
}

private class FakeLegacySource(
    private val users: MutableMap<String, String>,
    private var cookies: String?,
) : LegacyPrefsSource {
    var cleared = false
    override fun readUserValues() = users.toMap()
    override fun readCookies() = cookies
    override fun clear() { cleared = true; users.clear(); cookies = null }
}

class LegacyPrefsMigrationTest {

    @Test
    fun copiesUserValuesAndCookiesThenClearsSource() {
        val userStore = FakeSecureStore()
        val cookieStore = FakeSecureStore()
        val flagStore = FakeSecureStore()
        val source = FakeLegacySource(
            users = mutableMapOf("authToken" to "T", "password" to "P", "isAuthenticated" to "true"),
            cookies = "c1\nc2",
        )

        LegacyPrefsMigration(userStore, cookieStore, flagStore).migrateIfNeeded(source)

        assertEquals("T", userStore.getString("authToken"))
        assertEquals("P", userStore.getString("password"))
        assertEquals("true", userStore.getString("isAuthenticated"))
        assertEquals("c1\nc2", cookieStore.getString(SecureCookieStore.KEY_COOKIES))
        assertTrue(source.cleared)
        assertEquals("true", flagStore.getString(LegacyPrefsMigration.KEY_MIGRATION_DONE))
    }

    @Test
    fun isIdempotent_secondRunDoesNotTouchSource() {
        val userStore = FakeSecureStore()
        val cookieStore = FakeSecureStore()
        val flagStore = FakeSecureStore()
        flagStore.putString(LegacyPrefsMigration.KEY_MIGRATION_DONE, "true")
        val source = FakeLegacySource(mutableMapOf("authToken" to "SHOULD_NOT_COPY"), null)

        LegacyPrefsMigration(userStore, cookieStore, flagStore).migrateIfNeeded(source)

        assertNull(userStore.getString("authToken"))
        assertFalse(source.cleared)
    }

    @Test
    fun noCookies_leavesCookieStoreEmpty() {
        val userStore = FakeSecureStore()
        val cookieStore = FakeSecureStore()
        val flagStore = FakeSecureStore()
        val source = FakeLegacySource(mutableMapOf("email" to "a@b.c"), null)

        LegacyPrefsMigration(userStore, cookieStore, flagStore).migrateIfNeeded(source)

        assertNull(cookieStore.getString(SecureCookieStore.KEY_COOKIES))
        assertEquals("a@b.c", userStore.getString("email"))
    }

    @Test
    fun doneFlagSurvivesUserStoreClear_doesNotReRunAfterLogout() {
        val userStore = FakeSecureStore()
        val cookieStore = FakeSecureStore()
        val flagStore = FakeSecureStore()
        val migration = LegacyPrefsMigration(userStore, cookieStore, flagStore)
        val firstSource = FakeLegacySource(mutableMapOf("password" to "P"), "c1")
        migration.migrateIfNeeded(firstSource)

        // Simulate logout: UserPrefs.clear() wipes the user + cookie stores, but not flagStore.
        userStore.clear()
        cookieStore.clear()

        // A relaunch after logout must NOT re-run the migration.
        val secondSource = FakeLegacySource(mutableMapOf("password" to "SHOULD_NOT_COPY"), "c2")
        migration.migrateIfNeeded(secondSource)

        assertFalse(secondSource.cleared)
        assertNull(userStore.getString("password"))
    }
}
