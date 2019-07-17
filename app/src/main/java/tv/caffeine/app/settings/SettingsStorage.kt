package tv.caffeine.app.settings

import android.content.SharedPreferences
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import androidx.core.content.edit
import okio.ByteString
import tv.caffeine.app.api.model.CAID
import tv.caffeine.app.di.CAFFEINE_SHARED_PREFERENCES
import java.security.InvalidKeyException
import java.security.KeyStore
import java.security.UnrecoverableKeyException
import javax.crypto.BadPaddingException
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.spec.IvParameterSpec
import javax.inject.Inject
import javax.inject.Named
import kotlin.reflect.KProperty

interface SettingsStorage {
    var refreshToken: String?
    var caid: CAID?
    var clientId: String?
    var environment: String?

    fun clearCredentials() {
        refreshToken = null
        caid = null
        clientId = null
    }
}

class InMemorySettingsStorage(
    override var refreshToken: String? = null,
    override var caid: CAID? = null,
    override var clientId: String? = null,
    override var environment: String? = null
) : SettingsStorage

private const val REFRESH_TOKEN_KEY = "REFRESH_TOKEN"
private const val CAID_KEY = "CAID"
private const val CLIENT_ID_KEY = "CLIENT_ID_KEY"
private const val ENVIRONMENT = "ENVIRONMENT"

class SharedPrefsStorage @Inject constructor(
    @Named(CAFFEINE_SHARED_PREFERENCES) sharedPreferences: SharedPreferences
) : SettingsStorage {
    override var refreshToken by SharedPrefsDelegate(sharedPreferences, REFRESH_TOKEN_KEY)

    override var caid: CAID? by SharedPrefsDelegate(sharedPreferences, CAID_KEY)

    override var clientId by SharedPrefsDelegate(sharedPreferences, CLIENT_ID_KEY)

    override var environment by SharedPrefsDelegate(sharedPreferences, ENVIRONMENT)
}

class SharedPrefsDelegate(private val sharedPreferences: SharedPreferences, private val prefKey: String) {
    operator fun getValue(thisRef: Any?, property: KProperty<*>): String? = sharedPreferences.getString(prefKey, null)

    operator fun setValue(thisRef: Any?, property: KProperty<*>, value: String?) = sharedPreferences.edit {
        when (value) {
            null -> remove(prefKey)
            else -> putString(prefKey, value)
        }
    }
}

class EncryptedSettingsStorage @Inject constructor(
    private val keyStoreHelper: KeyStoreHelper,
    private val settingsStorage: SettingsStorage
) : SettingsStorage {
    override var refreshToken: String?
        get() = settingsStorage.refreshToken?.let { decryptValue(it) }
        set(value) {
            settingsStorage.refreshToken = value?.let { encryptValue(it) }
        }

    override var caid: CAID?
        get() = settingsStorage.caid?.let { decryptValue(it) }
        set(value) {
            settingsStorage.caid = value?.let { encryptValue(it) }
        }

    override var clientId: String?
        get() = settingsStorage.clientId?.let { decryptValue(it) }
        set(value) {
            settingsStorage.clientId = value?.let { encryptValue(it) }
        }

    override var environment: String?
        get() = settingsStorage.environment?.let { decryptValue(it) }
        set(value) {
            settingsStorage.environment = value?.let { encryptValue(it) }
        }

    private fun reset() {
        settingsStorage.apply {
            refreshToken = null
            caid = null
            clientId = null
            environment = null
        }
        keyStoreHelper.regenerateKeys()
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
        return try {
            keyStoreHelper.decrypt(EncryptedContent(iv, encryptedBytes)).toString(Charsets.UTF_8)
        } catch (t: Throwable) {
            when (t) {
                is BadPaddingException, is UnrecoverableKeyException, is InvalidKeyException -> reset()
            }
            null
        }
    }
}

class EncryptedContent(val iv: ByteArray, val content: ByteArray)

class KeyStoreHelper @Inject constructor(
    private val keyStore: KeyStore
) {

    init {
        ensureGoodKeys()
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

    fun regenerateKeys() {
        destroyKeys()
        ensureGoodKeys()
    }

    private fun ensureGoodKeys() {
        if (!keyStore.containsAlias(aesKeyAlias)) generateKeys()
    }

    private fun generateKeys() {
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
