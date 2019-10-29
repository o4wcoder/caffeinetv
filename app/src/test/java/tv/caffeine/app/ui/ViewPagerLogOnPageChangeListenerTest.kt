package tv.caffeine.app.ui

import com.google.firebase.analytics.FirebaseAnalytics
import io.mockk.MockKAnnotations
import io.mockk.Runs
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.just
import io.mockk.verify
import org.junit.Before
import org.junit.Test
import tv.caffeine.app.analytics.FirebaseEvent

class ViewPagerLogOnPageChangeListenerTest {

    private lateinit var subject: ViewPagerLogOnPageChangeListener
    @MockK private lateinit var firebaseAnalytics: FirebaseAnalytics

    @Before
    fun setup() {
        MockKAnnotations.init(this)
        every { firebaseAnalytics.logEvent(any(), any()) } just Runs
        subject = ViewPagerLogOnPageChangeListener(2, firebaseAnalytics)
    }

    @Test
    fun `the swipe next event is logged if the user swipes to the next item`() {
        subject.onPageScrolled(3, 0f, 0)
        verify(exactly = 1) { firebaseAnalytics.logEvent(FirebaseEvent.StageSwipeNext.name, any()) }
    }

    @Test
    fun `the swipe previous event is logged if the user swipes to the previous item`() {
        subject.onPageScrolled(1, 0f, 0)
        verify(exactly = 1) { firebaseAnalytics.logEvent(FirebaseEvent.StageSwipePrevious.name, any()) }
    }

    @Test
    fun `no swipe event is logged if the user doesn't swipe to the previous or next item`() {
        subject.onPageScrolled(2, 0f, 0)
        verify(exactly = 0) { firebaseAnalytics.logEvent(any(), any()) }
    }
}
