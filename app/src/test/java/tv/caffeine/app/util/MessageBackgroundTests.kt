package tv.caffeine.app.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import tv.caffeine.app.R
import tv.caffeine.app.chat.mentionsUsername
import tv.caffeine.app.chat.messageBackground

@RunWith(RobolectricTestRunner::class)
class MessageBackgroundTests {

    @Test
    fun `messages from self are blue`() {
        val background = messageBackground(true, false, false, false)
        assertEquals(background, R.color.chat_bubble_blue)
    }

    @Test
    fun `messages from self talking about self are blue`() {
        val background = messageBackground(true, true, false, false)
        assertEquals(background, R.color.chat_bubble_blue)
    }

    @Test
    fun `messages from others talking about self are orange`() {
        val background = messageBackground(false, true, false, false)
        assertEquals(background, R.color.chat_bubble_orange)
    }

    @Test
    fun `messages from followed users are blue`() {
        val background = messageBackground(false, false, true, false)
        assertEquals(background, R.color.chat_bubble_blue)
    }

    @Test
    fun `messages from non-followed users are blue`() {
        val background = messageBackground(false, false, false, false)
        assertEquals(background, R.color.chat_bubble_gray)
    }

    @Test
    fun `mentions detected`() {
        val mentions = "@caffeine you're cool".mentionsUsername("caffeine")
        assertTrue(mentions)
    }

    @Test
    fun `lack of mentions`() {
        val mentions = "caffeine you're cool".mentionsUsername("caffeine")
        assertFalse(mentions)
    }
}

@RunWith(RobolectricTestRunner::class)
class ReleaseMessageBackgroundTests {

    @Test
    fun `messages from self are cyan`() {
        val background = messageBackground(true, false, false, true)
        assertEquals(background, R.color.chat_bubble_cyan)
    }

    @Test
    fun `messages from followed users are purple gray`() {
        val background = messageBackground(false, false, true, true)
        assertEquals(background, R.color.chat_bubble_purple_gray_follow)
    }

    @Test
    fun `messages from non-followed users are dark gray`() {
        val background = messageBackground(false, false, false, true)
        assertEquals(background, R.color.chat_bubble_dark_gray_not_follow)
    }
}
