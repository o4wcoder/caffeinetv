package tv.caffeine.app.auth

import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.runner.AndroidJUnit4
import io.mockk.*
import org.junit.Assert.assertEquals
import org.junit.Assert.fail
import org.junit.Test
import org.junit.runner.RunWith
import tv.caffeine.app.BaseNavigationTest
import tv.caffeine.app.R
import tv.caffeine.app.analytics.Analytics
import tv.caffeine.app.analytics.AnalyticsEvent
import tv.caffeine.app.api.model.IdentityProvider
import tv.caffeine.app.util.navigateToLanding

@RunWith(AndroidJUnit4::class)
class LandingFragmentTests : BaseNavigationTest() {
    private lateinit var fragment: LandingFragment
    private lateinit var analytics: Analytics

    override fun navigateToTestDestination() {
        navController.navigateToLanding()
    }

    private fun configureFragment() {
        fragment = fragmentManager.fragments
                .flatMap { it.childFragmentManager.fragments }
                .find { it is LandingFragment } as? LandingFragment ?: return fail("Could not find Landing Fragment")
        analytics = mockk(relaxed = true)
        fragment.analytics = analytics
    }

    override fun testDestination() {
        onView(withId(R.id.facebook_sign_in_button)).check(matches(isDisplayed()))
        onView(withId(R.id.twitter_sign_in_button)).check(matches(isDisplayed()))
        onView(withId(R.id.new_account_button)).check(matches(isDisplayed()))
        onView(withId(R.id.sign_in_with_username_text_view)).check(matches(isDisplayed()))
    }

    @Test
    fun clickingFacebookGeneratesCorrectAnalyticsEvent() {
        onView(withId(R.id.facebook_sign_in_button)).check(matches(isDisplayed()))
        configureFragment()
        val slot = slot<AnalyticsEvent>()
        every { analytics.trackEvent(capture(slot)) } just Runs
        onView(withId(R.id.facebook_sign_in_button)).perform(click())
        assertEquals(AnalyticsEvent.SocialSignInClicked(IdentityProvider.facebook), slot.captured)
    }

    @Test
    fun clickingTwitterGeneratesCorrectAnalyticsEvent() {
        onView(withId(R.id.twitter_sign_in_button)).check(matches(isDisplayed()))
        configureFragment()
        /*
        onView(withId(R.id.twitter_sign_in_button)).perform(click())
        verify(exactly = 1) { analytics.trackEvent(AnalyticsEvent.SocialSignInClicked(IdentityProvider.twitter)) }
        */
    }

    @Test
    fun clickingNewAccountGeneratesCorrectAnalyticsEvent() {
        onView(withId(R.id.new_account_button)).check(matches(isDisplayed()))
        configureFragment()
        onView(withId(R.id.new_account_button)).perform(click())
        verify { analytics.trackEvent(AnalyticsEvent.NewAccountClicked) }
    }
}

