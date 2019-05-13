package tv.caffeine.app.fpg

import android.os.SystemClock
import android.text.format.DateUtils
import android.view.LayoutInflater
import android.widget.FrameLayout
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import tv.caffeine.app.R
import tv.caffeine.app.databinding.FeaturedGuideDateHeaderBinding
import tv.caffeine.app.lobby.DateHeaderViewHolder
import tv.caffeine.app.lobby.FeaturedGuideItem
import java.util.concurrent.TimeUnit

@RunWith(RobolectricTestRunner::class)
class FeaturedProgramGuideDateHeaderTests {

    private lateinit var viewHolder: DateHeaderViewHolder
    private lateinit var today: String
    private lateinit var tomorrow: String

    @Before
    fun setup() {
        val context = InstrumentationRegistry.getInstrumentation().context
        val parent = FrameLayout(context)
        val binding = FeaturedGuideDateHeaderBinding.inflate(LayoutInflater.from(context), parent, false)
        viewHolder = DateHeaderViewHolder(binding)
        today = context.getString(R.string.today)
        tomorrow = context.getString(R.string.tomorrow)
    }

    @Test
    fun `the date header is today for today's broadcast`() {
        val nowInMs = SystemClock.currentThreadTimeMillis()
        val todayInSeconds = TimeUnit.MILLISECONDS.toSeconds(nowInMs)
        assertEquals(viewHolder.getDateText(FeaturedGuideItem.DateHeader(todayInSeconds)), today)
    }

    @Test
    fun `the date header is tomorrow for tomorrow's broadcast`() {
        val nowInMs = SystemClock.currentThreadTimeMillis()
        val tomorrowInSeconds = TimeUnit.MILLISECONDS.toSeconds(nowInMs + DateUtils.DAY_IN_MILLIS)
        assertEquals(viewHolder.getDateText(FeaturedGuideItem.DateHeader(tomorrowInSeconds)), tomorrow)
    }

    @Test
    fun `the date header is not today for yesterday's broadcast`() {
        val nowInMs = SystemClock.currentThreadTimeMillis()
        val yesterdayInSeconds = TimeUnit.MILLISECONDS.toSeconds(nowInMs - DateUtils.DAY_IN_MILLIS)
        assertNotEquals(viewHolder.getDateText(FeaturedGuideItem.DateHeader(yesterdayInSeconds)), today)
    }

    @Test
    fun `the date header is not tomorrow for the day after tomorrow's broadcast`() {
        val nowInMs = SystemClock.currentThreadTimeMillis()
        val theDayAfterTomorrowInSeconds = TimeUnit.MILLISECONDS.toSeconds(nowInMs + DateUtils.DAY_IN_MILLIS * 2)
        assertNotEquals(viewHolder.getDateText(FeaturedGuideItem.DateHeader(theDayAfterTomorrowInSeconds)), tomorrow)
    }
}
