package tv.caffeine.app.ui

import android.view.View
import io.mockk.MockKAnnotations
import junit.framework.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

private lateinit var subject: LiveStatusIndicatorViewModel

@RunWith(RobolectricTestRunner::class)
class LiveStatusIndicatorViewModelTests {

    @Before
    fun setup() {
        MockKAnnotations.init(this)
        subject = LiveStatusIndicatorViewModel()
    }

    @Test
    fun `live status indicator is shown if user is live`() {
        subject.isUserLive = true
        assertEquals(subject.getIndicatorVisibility(), View.VISIBLE)
    }

    @Test
    fun `live status indicator is not shown if user is not live`() {
        subject.isUserLive = false
        assertEquals(subject.getIndicatorVisibility(), View.GONE)
    }
}