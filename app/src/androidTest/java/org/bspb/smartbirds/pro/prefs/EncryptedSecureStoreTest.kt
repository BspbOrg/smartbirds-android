package org.bspb.smartbirds.pro.prefs

import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class EncryptedSecureStoreTest {

    private val context = ApplicationProvider.getApplicationContext<android.content.Context>()
    private val fileName = "test_secure_store"
    private val keyAlias = "test_secure_store_key"

    private fun newStore(onReset: (() -> Unit)? = null) =
        EncryptedSecureStore(context, fileName, keyAlias, onReset)

    @Before
    fun clean() {
        newStore().clear()
    }

    @Test
    fun roundTripsValue() {
        val store = newStore()
        store.putString("password", "s3cr3t")
        assertEquals("s3cr3t", store.getString("password"))
        assertTrue(store.contains("password"))
    }

    @Test
    fun ciphertextOnDiskIsNotPlaintext() {
        val secret = "s3cr3t"
        newStore().putString("password", secret)
        // null default is a sentinel that can't collide with plaintext or ciphertext:
        // if putString silently wrote nothing, this fails as "nothing persisted", not
        // as a misleading "plaintext leaked". (encryptUsesFreshIvEachTime covers that
        // the stored form is genuine, non-deterministic ciphertext.)
        val raw = context.getSharedPreferences(fileName, android.content.Context.MODE_PRIVATE)
            .getString("password", null)
        assertNotNull(raw)
        assertFalse(raw!!.contains(secret))
    }

    @Test
    fun absentKeyReturnsDefault() {
        val store = newStore()
        assertNull(store.getString("absent"))
        assertEquals("d", store.getString("absent", "d"))
    }

    @Test
    fun invalidatedKeyTriggersResetAndReturnsDefault() {
        var resetCalled = false
        val store = newStore { resetCalled = true }
        store.putString("token", "abc")
        // Invalidate the Keystore key WITHOUT corrupting the stored ciphertext.
        KeystoreCrypto(keyAlias).deleteKey()
        assertEquals("fallback", store.getString("token", "fallback"))
        assertTrue(resetCalled)
        assertNull(store.getString("token"))
    }

    @Test
    fun encryptUsesFreshIvEachTime() {
        val store = newStore()
        val prefs = context.getSharedPreferences(fileName, android.content.Context.MODE_PRIVATE)
        store.putString("a", "same-value")
        val first = prefs.getString("a", "")!!
        store.putString("b", "same-value")
        val second = prefs.getString("b", "")!!
        assertFalse(first == second)
    }

    @Test
    fun corruptedValueTriggersResetAndReturnsDefault() {
        var resetCalled = false
        val store = newStore { resetCalled = true }
        store.putString("token", "abc")
        // Corrupt the stored ciphertext directly.
        context.getSharedPreferences(fileName, android.content.Context.MODE_PRIVATE)
            .edit().putString("token", "not-base64-or-valid").apply()
        assertEquals("fallback", store.getString("token", "fallback"))
        assertTrue(resetCalled)
        assertNull(store.getString("token"))
    }
}
