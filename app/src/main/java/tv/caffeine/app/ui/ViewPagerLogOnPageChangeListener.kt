package tv.caffeine.app.ui

import androidx.viewpager.widget.ViewPager
import com.google.firebase.analytics.FirebaseAnalytics
import tv.caffeine.app.analytics.FirebaseEvent
import tv.caffeine.app.analytics.logEvent

/**
 * Log the swipe direction.
 */
class ViewPagerLogOnPageChangeListener(
    private var lastPosition: Int,
    private val firebaseAnalytics: FirebaseAnalytics,
    private val swipeNextEvent: FirebaseEvent = FirebaseEvent.StageSwipeNext,
    private val swipePreviousEvent: FirebaseEvent = FirebaseEvent.StageSwipePrevious
) : ViewPager.OnPageChangeListener {

    override fun onPageScrollStateChanged(state: Int) {
    }

    override fun onPageScrolled(position: Int, positionOffset: Float, positionOffsetPixels: Int) {
        if (lastPosition < position) {
            firebaseAnalytics.logEvent(swipeNextEvent)
        } else if (lastPosition > position) {
            firebaseAnalytics.logEvent(swipePreviousEvent)
        }
        lastPosition = position
    }

    override fun onPageSelected(position: Int) {
        // Intentionally empty since it can cause artifacts during the scroll.
    }
}
