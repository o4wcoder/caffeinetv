package tv.caffeine.app.ui

import android.graphics.drawable.ColorDrawable
import android.view.View
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.runner.AndroidJUnit4
import com.google.android.material.animation.ArgbEvaluatorCompat
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ViewPagerColorOnPageChangeListenerTest {

    private lateinit var view: View
    private lateinit var listener: ViewPagerColorOnPageChangeListener
    private val startColor = 0x000000
    private val endColor = 0x222222

    @Before
    fun setup() {
        val context = InstrumentationRegistry.getInstrumentation().context
        view = View(context)
        listener = ViewPagerColorOnPageChangeListener(view, listOf(startColor, endColor))
    }

    @Test
    fun `the color before the scroll begins matches the start color`() {
        listener.onPageScrolled(0, 0f, 0)
        val color = (view.background as ColorDrawable).color
        assertEquals(color, startColor)
    }

    @Test
    fun `the color after the scroll ends matches the end color`() {
        listener.onPageScrolled(0, 1f, 0)
        val color = (view.background as ColorDrawable).color
        assertEquals(color, endColor)
    }

    @Test
    fun `the color during the scroll matches the mixed color`() {
        val offset = 0.4f
        listener.onPageScrolled(0, offset, 0)
        val color = (view.background as ColorDrawable).color
        assertEquals(color, ArgbEvaluatorCompat.getInstance().evaluate(offset, startColor, endColor))
    }
}