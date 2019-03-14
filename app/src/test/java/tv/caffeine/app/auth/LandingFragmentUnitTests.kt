package tv.caffeine.app.auth

import androidx.fragment.app.testing.launchFragmentInContainer
import androidx.navigation.NavController
import androidx.navigation.Navigation
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.scrollTo
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.runner.AndroidJUnit4
import com.facebook.FacebookSdk
import io.mockk.mockk
import io.mockk.verify
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import tv.caffeine.app.CaffeineApplication
import tv.caffeine.app.MainNavDirections
import tv.caffeine.app.R
import tv.caffeine.app.analytics.Analytics
import tv.caffeine.app.analytics.AnalyticsEvent
import tv.caffeine.app.analytics.LogAnalytics
import tv.caffeine.app.api.model.IdentityProvider
import tv.caffeine.app.di.DaggerTestComponent
import tv.caffeine.app.di.setApplicationInjector

//@RunWith(AndroidJUnit4::class)
class LandingFragmentUnitTests {
    private lateinit var analytics: Analytics

    //@Before
    fun setup() {
        val app = ApplicationProvider.getApplicationContext<CaffeineApplication>()
        val testComponent = DaggerTestComponent.builder().create(app)
        app.setApplicationInjector(testComponent)
        FacebookSdk.sdkInitialize(app)
        val directions = MainNavDirections.actionGlobalLandingFragment(null)
        val scenario = launchFragmentInContainer<LandingFragment>(directions.arguments)
        val navController = mockk<NavController>(relaxed = true)
        scenario.onFragment {
            analytics = mockk<LogAnalytics>(relaxed = true)
            it.analytics = analytics
            Navigation.setViewNavController(it.view!!, navController)
        }
    }

    //@Test
    fun `clicking facebook generates correct analytics event`() {
        onView(withId(R.id.facebook_sign_in_button)).check(matches(isDisplayed()))
        onView(withId(R.id.facebook_sign_in_button)).perform(click())
        verify(exactly = 1) { analytics.trackEvent(AnalyticsEvent.SocialSignInClicked(IdentityProvider.facebook)) }
    }

    //@Test
    fun `clicking twitter generates correct analytics event`() {
        onView(withId(R.id.twitter_sign_in_button)).check(matches(isDisplayed()))
        onView(withId(R.id.twitter_sign_in_button)).perform(click())
        verify(exactly = 1) { analytics.trackEvent(AnalyticsEvent.SocialSignInClicked(IdentityProvider.twitter)) }
    }

    //@Test
    fun `clicking new account generates correct analytics event`() {
        onView(withId(R.id.new_account_button)).check(matches(isDisplayed()))
        onView(withId(R.id.new_account_button)).perform(scrollTo()).perform(click())
        verify(exactly = 1) { analytics.trackEvent(AnalyticsEvent.NewAccountClicked) }
    }

}
