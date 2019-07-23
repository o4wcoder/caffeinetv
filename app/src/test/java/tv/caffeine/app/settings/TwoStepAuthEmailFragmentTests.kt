package tv.caffeine.app.settings

import androidx.fragment.app.testing.launchFragmentInContainer
import androidx.lifecycle.MutableLiveData
import androidx.navigation.NavController
import androidx.navigation.Navigation
import androidx.test.core.app.ApplicationProvider
import io.mockk.MockKAnnotations
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.mockk
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import tv.caffeine.app.CaffeineApplication
import tv.caffeine.app.api.model.CaffeineEmptyResult
import tv.caffeine.app.api.model.Event
import tv.caffeine.app.di.DaggerTestComponent
import tv.caffeine.app.di.ViewModelFactory
import tv.caffeine.app.di.setApplicationInjector
import tv.caffeine.app.settings.authentication.TwoStepAuthEmailFragment
import tv.caffeine.app.settings.authentication.TwoStepAuthEmailFragmentArgs
import tv.caffeine.app.settings.authentication.TwoStepAuthViewModel

@RunWith(RobolectricTestRunner::class)
class TwoStepAuthEmailFragmentTests {
    private lateinit var subject: TwoStepAuthEmailFragment
    @MockK lateinit var mockViewModel: TwoStepAuthViewModel
    @MockK lateinit var mockViewModelFactory: ViewModelFactory

    @Before
    fun setup() {
        MockKAnnotations.init(this)
        val app = ApplicationProvider.getApplicationContext<CaffeineApplication>()
        val testComponent = DaggerTestComponent.factory().create(app)
        app.setApplicationInjector(testComponent)
        every { mockViewModelFactory.create(TwoStepAuthViewModel::class.java) } returns mockViewModel
        val arguments = TwoStepAuthEmailFragmentArgs("user@email.com").toBundle()
        val navController = mockk<NavController>(relaxed = true)

        val scenario = launchFragmentInContainer(arguments) {
            TwoStepAuthEmailFragment().also { fragment ->
                fragment.viewLifecycleOwnerLiveData.observeForever { viewLifecycleOwner ->
                    if (viewLifecycleOwner != null) {
                        Navigation.setViewNavController(fragment.requireView(), navController)
                    }
                }
                fragment.viewModelFactory = mockViewModelFactory
            }
        }
        scenario.onFragment { fragment ->
            subject = fragment
        }
        val result = MutableLiveData<Event<CaffeineEmptyResult>>()
        result.postValue(Event(CaffeineEmptyResult.Success))
        every { mockViewModel.sendVerificationCode(any()) } returns result
    }

    @Test
    fun `clicking next button will send out verification check to endpoint`() {
        // Moved test over to TwoStepAuthEmailViewModelTests

        // subject.binding.verificationCodeButton.isEnabled = true
        // subject.binding.verificationCodeButton.performClick()
        // verify (exactly = 1) { mockViewModel.sendVerificationCode(any()) }
    }
}
