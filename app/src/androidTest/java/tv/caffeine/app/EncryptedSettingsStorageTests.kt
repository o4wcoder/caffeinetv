package tv.caffeine.app

import org.junit.Assert
import org.junit.Test
import tv.caffeine.app.api.model.CAID
import tv.caffeine.app.settings.EncryptedSettingsStorage
import tv.caffeine.app.settings.KeyStoreHelper
import tv.caffeine.app.settings.SettingsStorage
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
}

class InMemorySettingsStorage(override var refreshToken: String? = null, override var caid: CAID? = null) : SettingsStorage
