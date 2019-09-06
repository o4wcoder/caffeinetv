package tv.caffeine.app.stage

import android.content.Context
import androidx.core.content.ContextCompat
import androidx.test.platform.app.InstrumentationRegistry
import io.mockk.MockKAnnotations
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import tv.caffeine.app.R

@RunWith(RobolectricTestRunner::class)
class StageProfileOverlayViewModelTests {
    lateinit var subject: StageProfileOverlayViewModel
    lateinit var context: Context

    @Before
    fun setup() {
        MockKAnnotations.init(this)
        context = InstrumentationRegistry.getInstrumentation().context
        subject = StageProfileOverlayViewModel(context, mockk())
    }

    @Test
    fun `profile is showing sets icon to cyan`() {
        subject.isProfileShowing = true
        assertEquals(subject.getTint(), ContextCompat.getColor(context, R.color.cyan))
    }

    @Test
    fun `chat is showing sets icon to white`() {
        subject.isProfileShowing = false
        assertEquals(subject.getTint(), ContextCompat.getColor(context, R.color.white))
    }
}