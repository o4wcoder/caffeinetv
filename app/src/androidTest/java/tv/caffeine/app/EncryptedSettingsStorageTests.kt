package tv.caffeine.app

import org.junit.Assert
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
        Assert.assertEquals(bytes.size, decrypted.size)
        Assert.assertEquals(bytes.toString(Charset.forName("UTF-8")), decrypted.toString(Charset.forName("UTF-8")))
    }

    @Test
    fun encryptedThingsAreDecryptable() {
        val originalString = "ABCDEFG random stuff, Caffeine"
        subject.refreshToken = originalString
        val decryptedString = subject.refreshToken
        Assert.assertEquals(originalString, decryptedString)
    }

    @Test
    fun swappingKeyStoreHelperIsHarmless() {
        val anotherKeyStoreHelper = KeyStoreHelper(KeyStoreHelper.defaultKeyStore())
        val anotherStorage = EncryptedSettingsStorage(anotherKeyStoreHelper, inMemorySettingsStorage)
        val originalString = "ABCDEFG random stuff, Caffeine"
        subject.refreshToken = originalString
        val decryptedString = anotherStorage.refreshToken
        Assert.assertEquals(originalString, decryptedString)
    }

    @Test(expected = java.security.InvalidKeyException::class)
    fun deletingKeysCausesDecryptionToFail() {
        val originalString = "ABCDEFG random stuff, Caffeine"
        subject.refreshToken = originalString
        deleteKey()
        val decryptedString = subject.refreshToken // expect exception to be thrown
        Assert.assertNull(decryptedString) // this is unreachable
    }

    @Test(expected = javax.crypto.BadPaddingException::class)
    fun regeneratingKeysCausesDecryptionToFail() {
        val originalString = "ABCDEFG random stuff, Caffeine"
        subject.refreshToken = originalString
        deleteKey()
        val anotherKeyStoreHelper = KeyStoreHelper(KeyStoreHelper.defaultKeyStore()) // triggers regenerating keys
        val decryptedString = subject.refreshToken // expect exception to be thrown
        Assert.assertNull(decryptedString) // this is unreachable
    }

    private fun deleteKey() {
        // this is an implementation detail
        KeyStoreHelper.defaultKeyStore().deleteEntry("CaffeineKeyAlias")
    }
}
