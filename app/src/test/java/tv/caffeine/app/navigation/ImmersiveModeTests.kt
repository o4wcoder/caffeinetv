package tv.caffeine.app.navigation

import android.content.Intent
import io.mockk.MockKAnnotations
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.verify
import org.junit.After
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
class ImmersiveModeTests {

    private val activityTestRule =
        InjectionActivityTestRule(MainActivity::class.java, DaggerTestComponent.factory())
    private lateinit var mainActivity: MainActivity

    @MockK
    lateinit var releaseDesignConfig: ReleaseDesignConfig

    @Before
    fun setup() {
        MockKAnnotations.init(this)
        mainActivity = activityTestRule.launchActivity(Intent())
        mainActivity.releaseDesignConfig = releaseDesignConfig
        every { releaseDesignConfig.isReleaseDesignActive() } returns true
    }

    @After
    fun cleanup() {
        activityTestRule.finishActivity()
    }

    @Test
    fun `unset immersive mode when navigating away from stage`() {
        mainActivity.updateImmersiveMode(R.id.lobbySwipeFragment)
        verify(exactly = 1) { mainActivity.unsetImmersiveMode() }
    }

    @Test
    fun `do not unset immersive mode when navigate away from stage to dialog`() {
        mainActivity.updateImmersiveMode(R.id.reportOrIgnoreDialogFragment)
        verify(exactly = 0) { mainActivity.unsetImmersiveMode() }
    }
}