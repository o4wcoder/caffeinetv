package tv.caffeine.app.profile

import android.view.MenuItem
import androidx.core.os.bundleOf
import androidx.fragment.app.testing.launchFragmentInContainer
import androidx.navigation.NavController
import androidx.navigation.NavDirections
import androidx.navigation.Navigation
import androidx.test.core.app.ApplicationProvider
import io.mockk.MockKAnnotations
import io.mockk.Runs
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.just
import io.mockk.verify
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.fakes.RoboMenuItem
import tv.caffeine.app.CaffeineApplication
import tv.caffeine.app.R
import tv.caffeine.app.di.DaggerTestComponent
import tv.caffeine.app.di.setApplicationInjector
import tv.caffeine.app.util.navigateToReportOrIgnoreDialog

@RunWith(RobolectricTestRunner::class)
class ProfileFragmentTests {
    private lateinit var subject: ProfileFragment

    @MockK lateinit var navController: NavController
    @MockK(relaxed = true) lateinit var userProfile: UserProfile
    private val menuItem: MenuItem = RoboMenuItem(R.id.overflow_menu_item)

    @Before
    fun setup() {
        MockKAnnotations.init(this)
        val app = ApplicationProvider.getApplicationContext<CaffeineApplication>()
        val testComponent = DaggerTestComponent.factory().create(app)
        app.setApplicationInjector(testComponent)
        every { navController.navigate(any<NavDirections>()) } just Runs
        val scenario = launchFragmentInContainer<ProfileFragment>(bundleOf("caid" to "caid"))
        scenario.onFragment { fragment ->
            Navigation.setViewNavController(fragment.view!!, navController)
            every { userProfile.username } returns "username"
            fragment.binding.userProfile = userProfile
            subject = fragment
        }
    }

    @Test
    fun `selecting report or ignore opens dialog`() {
        subject.onOptionsItemSelected(menuItem)
        verify { navController.navigateToReportOrIgnoreDialog("caid", "username", true) }
    }
}
