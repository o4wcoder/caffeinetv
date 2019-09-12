package tv.caffeine.app.settings

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.squareup.picasso.Picasso
import io.mockk.MockKAnnotations
import io.mockk.every
import io.mockk.impl.annotations.MockK
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import tv.caffeine.app.R

@RunWith(RobolectricTestRunner::class)
class GoldBundlesFragmentTests {

    @get:Rule
    val instantExecutorRule = InstantTaskExecutorRule()

    @MockK private lateinit var fakePicasso: Picasso
    @MockK private lateinit var fakeIsReleaseDesignConfig: ReleaseDesignConfig

    private lateinit var subject: GoldBundlesFragment

    @Before
    fun setup() {
        MockKAnnotations.init(this)
        every { fakeIsReleaseDesignConfig.isReleaseDesignActive() } returns true
        val args = GoldBundlesFragmentArgs(BuyGoldOption.UsingPlayStore, true)
        subject = GoldBundlesFragment(fakePicasso, fakeIsReleaseDesignConfig)
        subject.arguments = args.toBundle()
    }

    @Test
    fun `validate dark mode argument value`() {
        val isDarkMode = subject.arguments?.getBoolean("isDarkMode")
        assertTrue(isDarkMode!!)
    }

    @Test
    fun `get scrollview background returns almost black when is release design and is dark mode`() {
        val colorToTest = subject.getScrollviewBackground(true, true)
        assertEquals(colorToTest, R.color.almost_black)
    }

    @Test
    fun `get scrollview background returns transparent when not release design and is dark mode`() {
        val colorToTest = subject.getScrollviewBackground(false, true)
        assertEquals(colorToTest, R.color.transparent)
    }

    @Test
    fun `get scrollview background returns transparent when is release design and not dark mode`() {
        val colorToTest = subject.getScrollviewBackground(true, false)
        assertEquals(colorToTest, R.color.transparent)
    }

    @Test
    fun `get scrollview background returns transparent when not release design and not dark mode`() {
        val colorToTest = subject.getScrollviewBackground(false, false)
        assertEquals(colorToTest, R.color.transparent)
    }
}