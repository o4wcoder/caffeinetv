package tv.caffeine.app.settings

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.google.gson.Gson
import io.mockk.MockKAnnotations
import io.mockk.coEvery
import io.mockk.impl.annotations.MockK
import kotlinx.coroutines.Deferred
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import retrofit2.Response
import tv.caffeine.app.api.AccountsService
import tv.caffeine.app.api.model.CaffeineEmptyResult
import tv.caffeine.app.api.model.awaitEmptyAndParseErrors
import tv.caffeine.app.settings.authentication.TwoStepAuthViewModel

@RunWith(RobolectricTestRunner::class)
class TwoStepAuthViewModelTests {
    @get:Rule val instantExecutorRule = InstantTaskExecutorRule()

    private lateinit var subject: TwoStepAuthViewModel
    @MockK lateinit var mockGson: Gson
    @MockK lateinit var mockAccountsService: AccountsService
    @MockK lateinit var setMfaResponse: Deferred<Response<Void>>
    @MockK lateinit var setMfaSuccess: CaffeineEmptyResult.Success

    @Before
    fun setup() {
        MockKAnnotations.init(this)
        subject = TwoStepAuthViewModel(mockAccountsService, mockGson)
        coEvery { mockAccountsService.setMFA(any()) } returns setMfaResponse
        coEvery { setMfaResponse.awaitEmptyAndParseErrors(mockGson) } returns setMfaSuccess
    }

    @Test
    fun `verify mfaEnabled live data returns false if set to false`() {
        subject.updateMfaEnabled(false)
        subject.mfaEnabled.observeForever { event ->
            event.getContentIfNotHandled()?.let { result ->
                assertFalse(result)
            }
        }
    }

    @Test
    fun `verify mfaEnabled live data returns true if set to true`() {
        subject.updateMfaEnabled(true)
        subject.mfaEnabled.observeForever { event ->
            event.getContentIfNotHandled()?.let { result ->
                assertTrue(result)
            }
        }
    }

    @Test
    fun `success in disabling mfa will turn off mta livedata`() {
        subject.disableAuth()
        subject.mfaEnabled.observeForever { event ->
            event.getContentIfNotHandled()?.let { result ->
                assertFalse(result)
            }
        }
    }
}
