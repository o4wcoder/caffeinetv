package tv.caffeine.app.util

import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import tv.caffeine.app.R
import tv.caffeine.app.chat.chatMessageTextColor

@RunWith(RobolectricTestRunner::class)
class MessageTextColorTests {

    @Test
    fun `message text color is black when self`() {
        val messageText = chatMessageTextColor(true, true)
        assertEquals(messageText, R.color.black)
    }

    @Test
    fun `message text color is white when not self`() {
        val messageText = chatMessageTextColor(false, true)
        assertEquals(messageText, R.color.white)
    }

    @Test
    fun `message text color is white when not release ui`() {
        val messageText = chatMessageTextColor(false, false)
        assertEquals(messageText, R.color.white)
    }
}