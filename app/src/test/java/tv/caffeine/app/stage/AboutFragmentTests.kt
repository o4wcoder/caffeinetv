package tv.caffeine.app.stage

import android.content.Context
import androidx.core.os.bundleOf
import androidx.fragment.app.testing.launchFragmentInContainer
import androidx.navigation.NavController
import androidx.navigation.Navigation
import androidx.test.core.app.ApplicationProvider
import androidx.test.platform.app.InstrumentationRegistry
import io.mockk.MockKAnnotations
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import tv.caffeine.app.CaffeineApplication
import tv.caffeine.app.R
import tv.caffeine.app.di.DaggerTestComponent
import tv.caffeine.app.di.setApplicationInjector
import tv.caffeine.app.profile.UserProfile

@RunWith(RobolectricTestRunner::class)
class AboutFragmentTests {

    private lateinit var context: Context
    lateinit var subject: AboutFragment

    @MockK lateinit var userProfile: UserProfile

    @Before
    fun setup() {
        MockKAnnotations.init(this)
        every { userProfile.username } returns "username"

        context = InstrumentationRegistry.getInstrumentation().context
        val app = ApplicationProvider.getApplicationContext<CaffeineApplication>()
        val testComponent = DaggerTestComponent.factory().create(app)
        app.setApplicationInjector(testComponent)
        val navController = mockk<NavController>(relaxed = true)
        val scenario = launchFragmentInContainer<AboutFragment>(bundleOf("caid" to "caid"))
        scenario.onFragment { fragment ->
            Navigation.setViewNavController(fragment.view!!, navController)
            subject = fragment
        }
    }

    @Test
    fun `when user bio is missing show empty text`() {
        every { userProfile.bio } returns ""
        assertEquals(subject.getBioText(userProfile),
            context.getString(R.string.stage_profile_empty_biography, userProfile.username))
    }

    @Test
    fun `when user bio is not empty do not show empty text`() {
        every { userProfile.bio } returns "This is my bio"
        assertEquals(subject.getBioText(userProfile), "This is my bio")
    }
}