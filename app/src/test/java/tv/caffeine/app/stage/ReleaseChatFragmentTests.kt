package tv.caffeine.app.stage

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.fragment.app.testing.launchFragmentInContainer
import androidx.test.core.app.ApplicationProvider
import io.mockk.MockKAnnotations
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.Assert.assertNotNull
import org.robolectric.RobolectricTestRunner
import org.robolectric.shadows.ShadowDialog
import tv.caffeine.app.CaffeineApplication
import tv.caffeine.app.di.DaggerTestComponent
import tv.caffeine.app.di.setApplicationInjector
import tv.caffeine.app.stage.release.ReleaseChatFragment
import androidx.appcompat.app.AlertDialog
import com.google.android.material.bottomsheet.BottomSheetDialog
import org.junit.Assert.assertTrue
import tv.caffeine.app.CaffeineConstants.TAG_SEND_MESSAGE
import tv.caffeine.app.CaffeineConstants.TAG_VERIFY_EMAIL

@RunWith(RobolectricTestRunner::class)
class ReleaseChatFragmentTests {
    @get:Rule
    val instantExecutorRule = InstantTaskExecutorRule()

    private lateinit var fragment: ReleaseChatFragment

    @Before
    fun setup() {
        MockKAnnotations.init(this)
        val app = ApplicationProvider.getApplicationContext<CaffeineApplication>()
        val testComponent = DaggerTestComponent.factory().create(app)
        app.setApplicationInjector(testComponent)
        val arguments = ChatFragmentArgs("username").toBundle()
        val scenario = launchFragmentInContainer(arguments, tv.caffeine.app.R.style.AppTheme) {
            ChatFragment.newInstance("username", true)
        }

        scenario.onFragment {
            fragment = it as ReleaseChatFragment
        }
    }

    @Test
    fun `clicking react button and email has been verified shows chat message bottom sheet dialog`() {
        fragment.showMessageDialog(true)

        // test that there is a bottom sheet dialog showing
        val dialog = ShadowDialog.getLatestDialog() as BottomSheetDialog
        assertNotNull(dialog)
        assertTrue(dialog.isShowing)

        // test that this is the send message bottom sheet dialog fragment
        val newMessageDialog = fragment.fragmentManager?.findFragmentByTag(TAG_SEND_MESSAGE)
        assertNotNull(newMessageDialog)
    }

    @Test
    fun `clicking the react button and email has not been verified shows verify email alert dialog`() {
        fragment.showMessageDialog(false)

        // test that there is an alert dialog showing
        val dialog = ShadowDialog.getLatestDialog() as AlertDialog
        assertNotNull(dialog)
        assertTrue(dialog.isShowing)

        // test that this is the verify email alert dialog fragment
        val newEmailVerifyDialog = fragment.fragmentManager?.findFragmentByTag(TAG_VERIFY_EMAIL)
        assertNotNull(newEmailVerifyDialog)
    }
}