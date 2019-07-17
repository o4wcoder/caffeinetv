package tv.caffeine.app.settings

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.google.gson.Gson
import io.mockk.MockKAnnotations
import io.mockk.coEvery
import io.mockk.impl.annotations.MockK
import kotlinx.coroutines.Deferred
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import retrofit2.Response
import tv.caffeine.app.api.AccountsService
import tv.caffeine.app.settings.authentication.TwoStepAuthViewModel
import tv.caffeine.app.test.observeForTesting

@RunWith(RobolectricTestRunner::class)
class TwoStepAuthViewModelTests {
    @get:Rule val instantExecutorRule = InstantTaskExecutorRule()

    private lateinit var subject: TwoStepAuthViewModel
    @MockK lateinit var mockGson: Gson
    @MockK lateinit var mockAccountsService: AccountsService
    @MockK lateinit var mfaDeferredResponse: Deferred<Response<Void>>
    @MockK lateinit var mfaResponse: Response<Void>

    @Before
    fun setup() {
        MockKAnnotations.init(this)
        subject = TwoStepAuthViewModel(mockAccountsService, mockGson)
        coEvery { mockAccountsService.setMFA(any()) } returns mfaDeferredResponse
        coEvery { mfaDeferredResponse.await() } returns mfaResponse
        coEvery { mfaResponse.isSuccessful } returns true
        coEvery { mfaResponse.errorBody() } returns null
    }

    @Test
    fun `verify mfaEnabled live data returns false if set to false`() {
        subject.updateMfaEnabled(false)
        subject.mfaEnabled.observeForTesting { event ->
            event.getContentIfNotHandled()?.let { result ->
                assertFalse(result)
            }
        }
    }

    @Test
    fun `verify mfaEnabled live data returns true if set to true`() {
        subject.updateMfaEnabled(true)
        subject.mfaEnabled.observeForTesting { event ->
            val result = event.getContentIfNotHandled()
            assertNotNull(result)
            requireNotNull(result)
            assertTrue(result)
        }
    }

    @Test
    fun `success in disabling mfa will turn off mta livedata`() {
        subject.disableAuth()
        subject.mfaEnabled.observeForTesting { event ->
            val result = event.getContentIfNotHandled()
            assertNotNull(result)
            requireNotNull(result)
            assertFalse(result)
        }
    }
}
