package tv.caffeine.app.ui

import android.view.View
import androidx.annotation.ColorInt
import androidx.viewpager.widget.ViewPager
import com.google.android.material.animation.ArgbEvaluatorCompat

/**
 * Change the background color of the view when the page is scrolled.
 */
class ViewPagerColorOnPageChangeListener(private val view: View, private val colors: List<Int>)
    : ViewPager.OnPageChangeListener {

    override fun onPageScrollStateChanged(state: Int) {
    }

    override fun onPageScrolled(position: Int, positionOffset: Float, positionOffsetPixels: Int) {
        @ColorInt val startColor = colors.getOrElse(position) { return }
        @ColorInt val endColor = colors.getOrElse(position + 1) { return }
        view.setBackgroundColor(ArgbEvaluatorCompat.getInstance().evaluate(positionOffset, startColor, endColor))
    }

    override fun onPageSelected(position: Int) {
        // Intentionally empty since it can cause artifacts during the scroll.
    }
}
