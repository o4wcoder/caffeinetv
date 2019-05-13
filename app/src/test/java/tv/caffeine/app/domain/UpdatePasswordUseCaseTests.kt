package tv.caffeine.app.domain

import android.content.res.Resources
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.google.gson.Gson
import io.mockk.MockKAnnotations
import io.mockk.every
import io.mockk.impl.annotations.MockK
import kotlinx.coroutines.runBlocking
import org.junit.Assert
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import tv.caffeine.app.api.AccountsService
import tv.caffeine.app.api.model.CaffeineResult
import tv.caffeine.app.api.passwordErrorsString
import tv.caffeine.app.auth.TokenStore
import tv.caffeine.app.profile.UpdatePasswordUseCase

class UpdatePasswordUseCaseTests {
    @Rule @JvmField val instantExecutorRule = InstantTaskExecutorRule()

    @MockK lateinit var accountsService: AccountsService
    @MockK lateinit var tokenStore: TokenStore
    @MockK lateinit var resources: Resources
    private lateinit var subject: UpdatePasswordUseCase

    @Before
    fun setup() {
        MockKAnnotations.init(this)
        every { resources.getString(any()) } returns "Passwords don't match"
        val gson = Gson()
        subject = UpdatePasswordUseCase(accountsService, tokenStore, resources, gson)
    }

    @Test
    fun passingMismatchedPasswordsReturnsError() {
        val result = runBlocking {
            val currentPassword = ""
            val password1 = "A"
            val password2 = "B"
            Assert.assertNotEquals(password1, password2) // pre-condition
            subject(currentPassword, password1, password2)
        }
        when (result) {
            is CaffeineResult.Error -> Assert.assertNotNull(result.error.passwordErrorsString)
            else -> Assert.fail("Was expecting lobby to load")
        }
    }
}
