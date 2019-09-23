package tv.caffeine.app.util

import android.os.Build
import org.junit.Assert.assertTrue
import org.junit.Assert.assertFalse
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.threeten.bp.ZonedDateTime
import tv.caffeine.app.ext.isNewer
import tv.caffeine.app.lobby.notification.TestApp

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Build.VERSION_CODES.O_MR1], application = TestApp::class)
class ZonedDateTimeExtTests {

    private val newDate = ZonedDateTime.now()
    private val oldDate = ZonedDateTime.now().minusHours(2)
    private val nullDate: ZonedDateTime? = null

    @Test
    fun `is newer returns true when date is newer`() {
        assertTrue(newDate.isNewer(oldDate))
    }

    @Test
    fun `is newer returns false when date is older`() {
        assertFalse(oldDate.isNewer(newDate))
    }

    @Test
    fun `is newer returns true when old date is null`() {
        assertTrue(newDate.isNewer(nullDate))
    }

    @Test
    fun `is newer returns true when new date is null`() {
        assertTrue(nullDate.isNewer(newDate))
    }
}