package tv.caffeine.app

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import tv.caffeine.app.settings.EncryptedSettingsStorage
import tv.caffeine.app.settings.InMemorySettingsStorage
import tv.caffeine.app.settings.KeyStoreHelper
import java.nio.charset.Charset

class EncryptedSettingsStorageTests {
    private val inMemorySettingsStorage = InMemorySettingsStorage()

    private val keyStoreHelper = KeyStoreHelper(KeyStoreHelper.defaultKeyStore())

    private val subject = EncryptedSettingsStorage(keyStoreHelper, inMemorySettingsStorage)

    @Test
    fun keyStoreHelperDoesTheRightThing() {
        val originalString = "ABC, 123, UN me, Caffeine"
        val bytes = originalString.toByteArray()
        val encrypted = keyStoreHelper.encrypt(bytes)
        val decrypted = keyStoreHelper.decrypt(encrypted)
        assertEquals(bytes.size, decrypted.size)
        assertEquals(bytes.toString(Charset.forName("UTF-8")), decrypted.toString(Charset.forName("UTF-8")))
    }

    @Test
    fun encryptedThingsAreDecryptable() {
        val originalString = "ABCDEFG random stuff, Caffeine"
        subject.refreshToken = originalString
        val decryptedString = subject.refreshToken
        assertEquals(originalString, decryptedString)
    }

    @Test
    fun swappingKeyStoreHelperIsHarmless() {
        val anotherKeyStoreHelper = KeyStoreHelper(KeyStoreHelper.defaultKeyStore())
        val anotherStorage = EncryptedSettingsStorage(anotherKeyStoreHelper, inMemorySettingsStorage)
        val originalString = "ABCDEFG random stuff, Caffeine"
        subject.refreshToken = originalString
        val decryptedString = anotherStorage.refreshToken
        assertEquals(originalString, decryptedString)
    }

    @Test
    fun deletingKeysResultsInDecryptReturningNull() {
        val originalString = "ABCDEFG random stuff, Caffeine"
        subject.refreshToken = originalString
        deleteKey()
        val decryptedString = subject.refreshToken
        assertNull(decryptedString)
    }

    @Test
    fun regeneratingKeysResultsInDecryptReturningNull() {
        val originalString = "ABCDEFG random stuff, Caffeine"
        subject.refreshToken = originalString
        deleteKey()
        val anotherKeyStoreHelper = KeyStoreHelper(KeyStoreHelper.defaultKeyStore()) // triggers regenerating keys
        val decryptedString = subject.refreshToken
        assertNull(decryptedString)
    }

    @Test
    fun regeneratingKeysResultsInAllDecryptsReturningNull() {
        val originalString = "ABCDEFG random stuff, Caffeine"
        subject.refreshToken = originalString
        subject.caid = originalString
        deleteKey()
        val anotherKeyStoreHelper = KeyStoreHelper(KeyStoreHelper.defaultKeyStore()) // triggers regenerating keys
        val decryptedString = subject.refreshToken
        assertNull(decryptedString)
        subject.refreshToken = originalString // encrypt again
        val decryptedAgainString = subject.refreshToken
        assertEquals(originalString, decryptedAgainString)
        val decryptedCaid = subject.caid // this should not cause a failure
        assertNull(decryptedCaid)
        val decryptedThirdTimeString = subject.refreshToken
        assertEquals(originalString, decryptedThirdTimeString)
    }

    private fun deleteKey() {
        // this is an implementation detail
        KeyStoreHelper.defaultKeyStore().deleteEntry("CaffeineKeyAlias")
    }
}
