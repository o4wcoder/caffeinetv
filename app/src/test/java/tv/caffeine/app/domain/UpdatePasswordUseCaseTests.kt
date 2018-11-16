package tv.caffeine.app.domain

import android.content.res.Resources
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.google.gson.Gson
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.doReturn
import com.nhaarman.mockitokotlin2.mock
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.junit.Assert
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import tv.caffeine.app.api.AccountsService
import tv.caffeine.app.api.model.CaffeineResult
import tv.caffeine.app.auth.TokenStore
import tv.caffeine.app.profile.UpdatePasswordUseCase

class UpdatePasswordUseCaseTests {
    @Rule @JvmField val instantExecutorRule = InstantTaskExecutorRule()

    private lateinit var subject: UpdatePasswordUseCase

    @Before
    fun setup() {
        val accountsService = mock<AccountsService> {}
        val tokenStore = mockk<TokenStore> {}
        val resources = mock<Resources> {
            on { getString(any()) } doReturn "Passwords don't match"
        }
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
            is CaffeineResult.Error -> Assert.assertNotNull(result.error.errors.password)
            else -> Assert.fail("Was expecting lobby to load")
        }
    }
}

