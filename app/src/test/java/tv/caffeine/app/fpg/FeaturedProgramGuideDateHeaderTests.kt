package tv.caffeine.app.fpg

import android.content.Context
import android.text.format.DateUtils
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import tv.caffeine.app.R
import tv.caffeine.app.lobby.FeaturedGuideItem
import tv.caffeine.app.lobby.getDateText
import java.util.concurrent.TimeUnit

@RunWith(RobolectricTestRunner::class)
class FeaturedProgramGuideDateHeaderTests {

    private lateinit var context: Context
    private lateinit var today: String
    private lateinit var tomorrow: String

    @Before
    fun setup() {
        context = InstrumentationRegistry.getInstrumentation().context
        today = context.getString(R.string.today)
        tomorrow = context.getString(R.string.tomorrow)
    }

    @Test
    fun `the date header is today for today's broadcast`() {
        val nowInMs = System.currentTimeMillis()
        val todayInSeconds = TimeUnit.MILLISECONDS.toSeconds(nowInMs)
        assertEquals(FeaturedGuideItem.DateHeader(todayInSeconds).getDateText(context), today)
    }

    @Test
    fun `the date header is tomorrow for tomorrow's broadcast`() {
        val nowInMs = System.currentTimeMillis()
        val tomorrowInSeconds = TimeUnit.MILLISECONDS.toSeconds(nowInMs + DateUtils.DAY_IN_MILLIS)
        assertEquals(FeaturedGuideItem.DateHeader(tomorrowInSeconds).getDateText(context), tomorrow)
    }

    @Test
    fun `the date header is not today for yesterday's broadcast`() {
        val nowInMs = System.currentTimeMillis()
        val yesterdayInSeconds = TimeUnit.MILLISECONDS.toSeconds(nowInMs - DateUtils.DAY_IN_MILLIS)
        assertNotEquals(FeaturedGuideItem.DateHeader(yesterdayInSeconds).getDateText(context), today)
    }

    @Test
    fun `the date header is not tomorrow for the day after tomorrow's broadcast`() {
        val nowInMs = System.currentTimeMillis()
        val theDayAfterTomorrowInSeconds = TimeUnit.MILLISECONDS.toSeconds(nowInMs + DateUtils.DAY_IN_MILLIS * 2)
        assertNotEquals(FeaturedGuideItem.DateHeader(theDayAfterTomorrowInSeconds).getDateText(context), tomorrow)
    }
}
