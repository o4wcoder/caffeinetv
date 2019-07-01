package tv.caffeine.app.login

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.fragment.app.testing.FragmentScenario
import androidx.fragment.app.testing.launchFragmentInContainer
import androidx.lifecycle.Lifecycle
import androidx.navigation.NavController
import androidx.navigation.Navigation
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.scrollTo
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import io.mockk.MockKAnnotations
import io.mockk.impl.annotations.MockK
import io.mockk.mockk
import io.mockk.verify
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import tv.caffeine.app.CaffeineApplication
import tv.caffeine.app.MainNavDirections
import tv.caffeine.app.R
import tv.caffeine.app.analytics.Analytics
import tv.caffeine.app.analytics.AnalyticsEvent
import tv.caffeine.app.api.model.IdentityProvider
import tv.caffeine.app.di.DaggerTestComponent
import tv.caffeine.app.di.setApplicationInjector

@RunWith(RobolectricTestRunner::class)
class LandingFragmentUnitTests {

    @MockK(relaxed = true) lateinit var analytics: Analytics
    @MockK(relaxed = true) lateinit var navController: NavController
    private lateinit var fragment: LandingFragment
    private lateinit var scenario: FragmentScenario<LandingFragment>
    @Rule @JvmField val instantExecutorRule = InstantTaskExecutorRule()

    @Before
    fun setup() {
        MockKAnnotations.init(this)
        val app = ApplicationProvider.getApplicationContext<CaffeineApplication>()
        val testComponent = DaggerTestComponent.factory().create(app)
        app.setApplicationInjector(testComponent)
        val directions = MainNavDirections.actionGlobalLandingFragment(null)
        scenario = launchFragmentInContainer<LandingFragment>(directions.arguments) {
            createLandingFragment(analytics, navController)
        }
        scenario.onFragment {
            fragment = it
        }
    }

    private fun createLandingFragment(analytics: Analytics, navController: NavController) = LandingFragment(
            mockk(), mockk(), mockk(), mockk(), mockk(), analytics, mockk(relaxed = true), mockk(relaxed = true)
    ).also { fragment ->
        fragment.viewLifecycleOwnerLiveData.observeForever { viewLifecycleOwner ->
            if (viewLifecycleOwner != null) {
                Navigation.setViewNavController(fragment.requireView(), navController)
            }
        }
    }

    @After
    fun cleanup() {
        scenario.moveToState(Lifecycle.State.DESTROYED)
    }

    @Test
    fun `clicking facebook generates correct analytics event`() {
        onView(withId(R.id.facebook_sign_in_button)).check(matches(isDisplayed()))
        onView(withId(R.id.facebook_sign_in_button)).perform(click())
        verify(exactly = 1) { analytics.trackEvent(AnalyticsEvent.SocialSignInClicked(IdentityProvider.facebook)) }
    }

    @Test
    fun `clicking twitter generates correct analytics event`() {
        onView(withId(R.id.twitter_sign_in_button)).check(matches(isDisplayed()))
        onView(withId(R.id.twitter_sign_in_button)).perform(click())
        verify(exactly = 1) { analytics.trackEvent(AnalyticsEvent.SocialSignInClicked(IdentityProvider.twitter)) }
    }

    @Test
    fun `clicking new account generates correct analytics event`() {
        onView(withId(R.id.new_account_button)).check(matches(isDisplayed()))
        onView(withId(R.id.new_account_button)).perform(scrollTo()).perform(click())
        verify(exactly = 1) { analytics.trackEvent(AnalyticsEvent.NewAccountClicked) }
    }
}
