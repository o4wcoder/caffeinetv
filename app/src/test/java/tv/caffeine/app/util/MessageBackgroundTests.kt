package tv.caffeine.app.util

import androidx.test.runner.AndroidJUnit4
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.equalTo
import org.junit.Test
import org.junit.runner.RunWith
import tv.caffeine.app.R
import tv.caffeine.app.chat.mentionsUsername
import tv.caffeine.app.chat.messageBackground


@RunWith(AndroidJUnit4::class)
class MessageBackgroundTests {

    @Test
    fun `messages from self are blue`() {
        val background = messageBackground(true, false, false)
        assertThat(background, equalTo(R.color.chat_bubble_blue))
    }

    @Test
    fun `messages from self talking about self are blue`() {
        val background = messageBackground(true, true, false)
        assertThat(background, equalTo(R.color.chat_bubble_blue))
    }

    @Test
    fun `messages from others talking about self are orange`() {
        val background = messageBackground(false, true, false)
        assertThat(background, equalTo(R.color.chat_bubble_orange))
    }

    @Test
    fun `messages from followed users are blue`() {
        val background = messageBackground(false, false, true)
        assertThat(background, equalTo(R.color.chat_bubble_blue))
    }

    @Test
    fun `messages from non-followed users are blue`() {
        val background = messageBackground(false, false, false)
        assertThat(background, equalTo(R.color.chat_bubble_gray))
    }

    @Test
    fun `mentions detected`() {
        val mentions = "@caffeine you're cool".mentionsUsername("caffeine")
        assertThat(mentions, equalTo(true))
    }

    @Test
    fun `lack of mentions`() {
        val mentions = "caffeine you're cool".mentionsUsername("caffeine")
        assertThat(mentions, equalTo(false))
    }

}
