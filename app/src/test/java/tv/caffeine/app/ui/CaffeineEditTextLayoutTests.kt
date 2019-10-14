package tv.caffeine.app.ui

import androidx.fragment.app.testing.launchFragmentInContainer
import androidx.navigation.NavController
import androidx.navigation.Navigation
import androidx.test.core.app.ApplicationProvider
import io.mockk.MockKAnnotations
import io.mockk.mockk
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import tv.caffeine.app.CaffeineApplication
import tv.caffeine.app.di.DaggerTestComponent
import tv.caffeine.app.di.setApplicationInjector
import tv.caffeine.app.login.SignInFragment
import android.text.TextUtils
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue

@RunWith(RobolectricTestRunner::class)
class CaffeineEditTextLayoutTests {

    private lateinit var caffeineEditTextLayout1: CaffeineEditTextLayout
    private lateinit var caffeineEditTextLayout2: CaffeineEditTextLayout

    @Before
    fun setup() {
        MockKAnnotations.init(this)
        val app = ApplicationProvider.getApplicationContext<CaffeineApplication>()
        val testComponent = DaggerTestComponent.factory().create(app)
        app.setApplicationInjector(testComponent)
        val navController = mockk<NavController>(relaxed = true)
        val scenario = launchFragmentInContainer() {
            // Using SignInFragment to test the custom layout
            SignInFragment(mockk())
        }
        scenario.onFragment { fragment ->
            Navigation.setViewNavController(fragment.view!!, navController)
            caffeineEditTextLayout1 = fragment.binding.usernameEditTextLayout
            caffeineEditTextLayout2 = fragment.binding.passwordEditTextLayout
        }
    }

    /**
     * Tests if the internal EditText views of custom CaffeineEditTextLayout can save their state with
     * overriding the parent implementation of the save/restore functions and manually save
     * and restore the state of the children (EditText Views) of CaffeineEditTextLayout.
     *
     * Test will save the state of layout1 and restore it into layout2 and verify they have the same text.
     */
    @Test
    fun `test OnSaveInstanceState saves text state`() {
        val testStr1 = "This is test str one"
        caffeineEditTextLayout1.text = testStr1
        caffeineEditTextLayout2.onRestoreInstanceState(caffeineEditTextLayout1.onSaveInstanceState()!!)
        assertTrue(TextUtils.equals(caffeineEditTextLayout1.text, caffeineEditTextLayout2.text))
    }

    @Test
    fun `test OnSaveInstanceState fails to save text state when using default implementation`() {
        val testStr1 = "This is test str one"
        caffeineEditTextLayout1.isSaveEnabled = false
        caffeineEditTextLayout2.isSaveEnabled = false
        caffeineEditTextLayout1.text = testStr1
        caffeineEditTextLayout2.onRestoreInstanceState(caffeineEditTextLayout1.onSaveInstanceState()!!)
        assertFalse(TextUtils.equals(caffeineEditTextLayout1.text, caffeineEditTextLayout2.text))
    }
}