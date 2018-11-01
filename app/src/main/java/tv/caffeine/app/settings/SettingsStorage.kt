package tv.caffeine.app.settings

import android.content.SharedPreferences
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import androidx.core.content.edit
import okio.ByteString
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.spec.IvParameterSpec
import javax.inject.Inject

interface SettingsStorage {
    var refreshToken: String?
}

private const val REFRESH_TOKEN_KEY = "REFRESH_TOKEN"

class SharedPrefsStorage @Inject constructor(
        private val sharedPreferences: SharedPreferences
) : SettingsStorage {
    override var refreshToken: String?
        get() = sharedPreferences.getString(REFRESH_TOKEN_KEY, null)
        set(value) = sharedPreferences.edit {
            if (value == null) {
                remove(REFRESH_TOKEN_KEY)
            } else {
                putString(REFRESH_TOKEN_KEY, value)
            }
        }

}


class EncryptedSettingsStorage @Inject constructor(
        private val keyStoreHelper: KeyStoreHelper,
        private val settingsStorage: SettingsStorage
) : SettingsStorage {
    override var refreshToken: String?
        get() {
            val encryptedValue = settingsStorage.refreshToken ?: return null
            return decryptValue(encryptedValue)
        }
        set(value) {
            if (value == null) {
                settingsStorage.refreshToken = null
                return
            }
            val encryptedValue = encryptValue(value)
            settingsStorage.refreshToken = encryptedValue
        }

    private fun encryptValue(originalValue: String): String {
        val bytes = originalValue.toByteArray()
        val encryptedContent = keyStoreHelper.encrypt(bytes)
        val string1 = ByteString.of(encryptedContent.iv, 0, encryptedContent.iv.size).base64()
        val string2 = ByteString.of(encryptedContent.content, 0, encryptedContent.content.size).base64()
        return "$string1:$string2"
    }

    private fun decryptValue(encryptedValue: String): String? {
        val encryptedStrings = encryptedValue.split(":")
        if (encryptedStrings.size != 2) return null
        val iv = ByteString.decodeBase64(encryptedStrings[0])?.toByteArray() ?: return null
        val encryptedBytes = ByteString.decodeBase64(encryptedStrings[1])?.toByteArray() ?: return null
        val decrypted = keyStoreHelper.decrypt(EncryptedContent(iv, encryptedBytes))
        return decrypted.toString(Charsets.UTF_8)
    }

}

class EncryptedContent(val iv: ByteArray, val content: ByteArray)

class KeyStoreHelper @Inject constructor(
        private val keyStore: KeyStore
) {

    init {
        generateKeys()
    }

    companion object {

        private const val androidKeyStore = "AndroidKeyStore"
        private const val aesKeyAlias = "CaffeineKeyAlias"

        private const val aesMode = "${KeyProperties.KEY_ALGORITHM_AES}/${KeyProperties.BLOCK_MODE_CBC}/${KeyProperties.ENCRYPTION_PADDING_PKCS7}"

        fun defaultKeyStore(): KeyStore {
            val keyStore = KeyStore.getInstance(androidKeyStore)
            keyStore.load(null)
            return keyStore
        }
    }

    private fun generateKeys() {
        if (keyStore.containsAlias(aesKeyAlias)) return

        val keyGenParameterSpec = KeyGenParameterSpec.Builder(aesKeyAlias, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(KeyProperties.BLOCK_MODE_CBC)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_PKCS7)
                .setRandomizedEncryptionRequired(true)
                .build()

        val keyGenerator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, androidKeyStore)

        keyGenerator.init(keyGenParameterSpec)

        val secretKey = keyGenerator.generateKey()
        requireNotNull(secretKey)
    }

    private fun destroyKeys() {
        if (keyStore.containsAlias(aesKeyAlias)) {
            keyStore.deleteEntry(aesKeyAlias)
        }
    }

    fun encrypt(input: ByteArray): EncryptedContent {
        val key = keyStore.getKey(aesKeyAlias, null)
        val cipher = Cipher.getInstance(aesMode)
        cipher.init(Cipher.ENCRYPT_MODE, key)
        val encryptedBytes = cipher.doFinal(input)
        val iv = cipher.iv
        return EncryptedContent(iv, encryptedBytes)
    }

    fun decrypt(encrypted: EncryptedContent): ByteArray {
        val key = keyStore.getKey(aesKeyAlias, null)
        val cipher = Cipher.getInstance(aesMode)
        val params = IvParameterSpec(encrypted.iv)
        cipher.init(Cipher.DECRYPT_MODE, key, params)
        return cipher.doFinal(encrypted.content)
    }

}
