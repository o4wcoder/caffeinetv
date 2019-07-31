package tv.caffeine.app.session

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import tv.caffeine.app.api.isTokenExpirationError
import tv.caffeine.app.api.model.CaffeineResult
import tv.caffeine.app.auth.TokenStore
import tv.caffeine.app.settings.InMemorySecureSettingsStorage
import tv.caffeine.app.settings.InMemorySettingsStorage
import tv.caffeine.app.test.observeForTesting

class SessionCheckViewModelTest {
    @get:Rule val instantExecutorRule = InstantTaskExecutorRule()

    @Test fun `missing refresh token fails session check`() {
        val settingsStorage = InMemorySettingsStorage(refreshToken = null)
        val secureSettingsStorage = InMemorySecureSettingsStorage()
        val tokenStore = TokenStore(settingsStorage, secureSettingsStorage)
        val subject = SessionCheckViewModel(tokenStore)
        subject.sessionCheck.observeForTesting { result ->
            assertTrue(result is CaffeineResult.Error)
            assertTrue((result as CaffeineResult.Error).error.isTokenExpirationError())
        }
    }

    @Test fun `when refresh token is present session check succeeds`() {
        val settingsStorage = InMemorySettingsStorage(refreshToken = "abc")
        val secureSettingsStorage = InMemorySecureSettingsStorage()
        val tokenStore = TokenStore(settingsStorage, secureSettingsStorage)
        val subject = SessionCheckViewModel(tokenStore)
        subject.sessionCheck.observeForTesting { result ->
            assertTrue(result is CaffeineResult.Success)
            assertTrue((result as CaffeineResult.Success).value)
        }
    }
}
