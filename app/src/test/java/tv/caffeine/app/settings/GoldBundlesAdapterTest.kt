package tv.caffeine.app.settings

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.squareup.picasso.Picasso
import io.mockk.MockKAnnotations
import io.mockk.impl.annotations.MockK
import org.junit.Before
import org.junit.Test
import org.junit.Assert.assertEquals
import org.junit.Rule
import tv.caffeine.app.R

class GoldBundlesAdapterTest {

    @get:Rule val instantExecutorRule = InstantTaskExecutorRule()

    @MockK private lateinit var fakePicasso: Picasso
    @MockK private lateinit var fakeIsReleaseDesignConfig: ReleaseDesignConfig
    @MockK private lateinit var fakeBuyGoldOption: BuyGoldOption
    @MockK private lateinit var fakeGoldBundleCLickListener: GoldBundleClickListener

    private lateinit var subject: GoldBundlesAdapter

    @Before
    fun setUp() {
        MockKAnnotations.init(this)

        subject = GoldBundlesAdapter(fakeBuyGoldOption, fakePicasso, fakeIsReleaseDesignConfig, true, fakeGoldBundleCLickListener)
    }

    @Test
    fun `get container background returns very dark gray rect when is release design and is dark mode`() {
        val colorToTest = subject.getGoldBundleContainerBackgroundResource(true, true)
        assertEquals(colorToTest, R.drawable.very_dark_gray_rounded_rect)
    }

    @Test
    fun `get container background returns gray rect when not release design and is dark mode`() {
        val colorToTest = subject.getGoldBundleContainerBackgroundResource(false, true)
        assertEquals(colorToTest, R.drawable.gray_rounded_rect)
    }

    @Test
    fun `get container background returns gray rect when is release design and not dark mode`() {
        val colorToTest = subject.getGoldBundleContainerBackgroundResource(true, false)
        assertEquals(colorToTest, R.drawable.gray_rounded_rect)
    }

    @Test
    fun `get container background returns gray rect when not release design and not dark mode`() {
        val colorToTest = subject.getGoldBundleContainerBackgroundResource(false, false)
        assertEquals(colorToTest, R.drawable.gray_rounded_rect)
    }
}