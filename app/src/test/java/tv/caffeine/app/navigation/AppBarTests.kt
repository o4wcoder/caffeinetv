package tv.caffeine.app.navigation

import android.content.Intent
import androidx.core.view.isVisible
import io.mockk.MockKAnnotations
import io.mockk.every
import io.mockk.impl.annotations.MockK
import org.junit.After
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import tv.caffeine.app.MainActivity
import tv.caffeine.app.R
import tv.caffeine.app.di.DaggerTestComponent
import tv.caffeine.app.di.InjectionActivityTestRule
import tv.caffeine.app.settings.ReleaseDesignConfig

@RunWith(RobolectricTestRunner::class)
class AppBarTests {

    private val activityTestRule =
        InjectionActivityTestRule(MainActivity::class.java, DaggerTestComponent.factory())
    private lateinit var mainActivity: MainActivity

    @MockK lateinit var releaseDesignConfig: ReleaseDesignConfig

    @Before
    fun setup() {
        MockKAnnotations.init(this)
        mainActivity = activityTestRule.launchActivity(Intent())
        mainActivity.releaseDesignConfig = releaseDesignConfig
    }

    @After
    fun cleanup() {
        activityTestRule.finishActivity()
    }

    @Test
    fun `the release app bar is visible on the lobby fragment in release UI`() {
        every { releaseDesignConfig.isReleaseDesignActive() } returns true
        val binding = mainActivity.binding
        mainActivity.updateUiOnDestinationChange(R.id.lobbySwipeFragment, binding)
        assertTrue(binding.releaseAppBar.isVisible)
        assertFalse(binding.classicAppBar.isVisible)
    }

    @Test
    fun `the release app bar is visible on the FPG fragment in release UI`() {
        every { releaseDesignConfig.isReleaseDesignActive() } returns true
        val binding = mainActivity.binding
        mainActivity.updateUiOnDestinationChange(R.id.featuredProgramGuideFragment, binding)
        assertTrue(binding.releaseAppBar.isVisible)
        assertFalse(binding.classicAppBar.isVisible)
    }

    @Test
    fun `the release app bar is visible on the my profile fragment in release UI`() {
        every { releaseDesignConfig.isReleaseDesignActive() } returns true
        val binding = mainActivity.binding
        mainActivity.updateUiOnDestinationChange(R.id.myProfileFragment, binding)
        assertTrue(binding.releaseAppBar.isVisible)
        assertFalse(binding.classicAppBar.isVisible)
    }

    @Test
    fun `the standard classic app bar is visible on the notifications fragment in release UI`() {
        every { releaseDesignConfig.isReleaseDesignActive() } returns true
        val binding = mainActivity.binding
        mainActivity.updateUiOnDestinationChange(R.id.notificationsFragment, binding)
        assertFalse(binding.releaseAppBar.isVisible)
        assertTrue(binding.classicAppBar.isVisible)
    }

    @Test
    fun `the standard classic app bar is visible on the search fragment in release UI`() {
        every { releaseDesignConfig.isReleaseDesignActive() } returns true
        val binding = mainActivity.binding
        mainActivity.updateUiOnDestinationChange(R.id.exploreFragment, binding)
        assertFalse(binding.releaseAppBar.isVisible)
        assertTrue(binding.classicAppBar.isVisible)
    }

    @Test
    fun `the app bar's visibility does not change when the report or ignore dialog shows in the release UI`() {
        every { releaseDesignConfig.isReleaseDesignActive() } returns true
        val binding = mainActivity.binding
        mainActivity.updateUiOnDestinationChange(R.id.lobbySwipeFragment, binding)
        mainActivity.updateUiOnDestinationChange(R.id.reportOrIgnoreDialogFragment, binding)
        assertTrue(binding.releaseAppBar.isVisible)
        assertFalse(binding.classicAppBar.isVisible)

        mainActivity.updateUiOnDestinationChange(R.id.exploreFragment, binding)
        mainActivity.updateUiOnDestinationChange(R.id.reportOrIgnoreDialogFragment, binding)
        assertFalse(binding.releaseAppBar.isVisible)
        assertTrue(binding.classicAppBar.isVisible)
    }

    @Test
    fun `the app bar's visibility does not change when the unfollow dialog shows in the release UI`() {
        every { releaseDesignConfig.isReleaseDesignActive() } returns true
        val binding = mainActivity.binding
        mainActivity.updateUiOnDestinationChange(R.id.lobbySwipeFragment, binding)
        mainActivity.updateUiOnDestinationChange(R.id.unfollowUserDialogFragment, binding)
        assertTrue(binding.releaseAppBar.isVisible)
        assertFalse(binding.classicAppBar.isVisible)

        mainActivity.updateUiOnDestinationChange(R.id.exploreFragment, binding)
        mainActivity.updateUiOnDestinationChange(R.id.unfollowUserDialogFragment, binding)
        assertFalse(binding.releaseAppBar.isVisible)
        assertTrue(binding.classicAppBar.isVisible)
    }

    /**
     * In the classic UI, the lobby has a customized app bar so the one in the Activity is hidden.
     */
    @Test
    fun `the standard classic app bar is not visible on the lobby fragment in classic UI`() {
        every { releaseDesignConfig.isReleaseDesignActive() } returns false
        val binding = mainActivity.binding
        mainActivity.updateUiOnDestinationChange(R.id.lobbySwipeFragment, binding)
        assertFalse(binding.releaseAppBar.isVisible)
        assertFalse(binding.classicAppBar.isVisible)
    }

    @Test
    fun `the standard classic app bar is visible on the my profile fragment in classic UI`() {
        every { releaseDesignConfig.isReleaseDesignActive() } returns false
        val binding = mainActivity.binding
        mainActivity.updateUiOnDestinationChange(R.id.myProfileFragment, binding)
        assertFalse(binding.releaseAppBar.isVisible)
        assertTrue(binding.classicAppBar.isVisible)
    }

    @Test
    fun `the standard classic app bar is visible on the notifications fragment in classic UI`() {
        every { releaseDesignConfig.isReleaseDesignActive() } returns false
        val binding = mainActivity.binding
        mainActivity.updateUiOnDestinationChange(R.id.notificationsFragment, binding)
        assertFalse(binding.releaseAppBar.isVisible)
        assertTrue(binding.classicAppBar.isVisible)
    }

    @Test
    fun `the standard classic app bar is visible on the search fragment in classic UI`() {
        every { releaseDesignConfig.isReleaseDesignActive() } returns false
        val binding = mainActivity.binding
        mainActivity.updateUiOnDestinationChange(R.id.exploreFragment, binding)
        assertFalse(binding.releaseAppBar.isVisible)
        assertTrue(binding.classicAppBar.isVisible)
    }

    @Test
    fun `the app bar's visibility does not change when the report or ignore dialog shows in classic UI`() {
        every { releaseDesignConfig.isReleaseDesignActive() } returns false
        val binding = mainActivity.binding
        mainActivity.updateUiOnDestinationChange(R.id.lobbySwipeFragment, binding)
        mainActivity.updateUiOnDestinationChange(R.id.reportOrIgnoreDialogFragment, binding)
        assertFalse(binding.releaseAppBar.isVisible)
        assertFalse(binding.classicAppBar.isVisible)

        mainActivity.updateUiOnDestinationChange(R.id.exploreFragment, binding)
        mainActivity.updateUiOnDestinationChange(R.id.reportOrIgnoreDialogFragment, binding)
        assertFalse(binding.releaseAppBar.isVisible)
        assertTrue(binding.classicAppBar.isVisible)
    }

    @Test
    fun `the app bar's visibility does not change when the unfollow dialog shows in classic UI`() {
        every { releaseDesignConfig.isReleaseDesignActive() } returns false
        val binding = mainActivity.binding
        mainActivity.updateUiOnDestinationChange(R.id.lobbySwipeFragment, binding)
        mainActivity.updateUiOnDestinationChange(R.id.unfollowUserDialogFragment, binding)
        assertFalse(binding.releaseAppBar.isVisible)
        assertFalse(binding.classicAppBar.isVisible)

        mainActivity.updateUiOnDestinationChange(R.id.exploreFragment, binding)
        mainActivity.updateUiOnDestinationChange(R.id.unfollowUserDialogFragment, binding)
        assertFalse(binding.releaseAppBar.isVisible)
        assertTrue(binding.classicAppBar.isVisible)
    }
}
