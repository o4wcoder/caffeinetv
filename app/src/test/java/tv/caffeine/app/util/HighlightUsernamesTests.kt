package tv.caffeine.app.util

import android.text.style.UnderlineSpan
import androidx.core.text.getSpans
import androidx.test.runner.AndroidJUnit4
import org.hamcrest.CoreMatchers.equalTo
import org.hamcrest.MatcherAssert.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import tv.caffeine.app.R
import tv.caffeine.app.chat.highlightUsernames
import tv.caffeine.app.chat.userReferenceStyle

@RunWith(AndroidJUnit4::class)
class HighlightUsernamesTests {

    @Test
    fun `username by itself`() {
        val text = "@caffeine"
        val result = highlightUsernames(text) {
            UnderlineSpan()
        }
        val spans = result.getSpans<UnderlineSpan>()
        assertThat(spans.size, equalTo(1))
        assertThat(result.getSpanStart(spans[0]), equalTo(0))
        assertThat(result.getSpanEnd(spans[0]), equalTo(9))
    }

    @Test
    fun `username with space in front`() {
        val text = " @caffeine"
        val result = highlightUsernames(text) {
            UnderlineSpan()
        }
        val spans = result.getSpans<UnderlineSpan>()
        assertThat(spans.size, equalTo(1))
        assertThat(result.getSpanStart(spans[0]), equalTo(1))
        assertThat(result.getSpanEnd(spans[0]), equalTo(10))
    }

    @Test
    fun `two usernames`() {
        val text = "@caffeine and @zand present"
        val result = highlightUsernames(text) {
            UnderlineSpan()
        }
        val spans = result.getSpans<UnderlineSpan>()
        assertThat(spans.size, equalTo(2))
        assertThat(result.getSpanStart(spans[0]), equalTo(0))
        assertThat(result.getSpanEnd(spans[0]), equalTo(9))
        assertThat(result.getSpanStart(spans[1]), equalTo(14))
        assertThat(result.getSpanEnd(spans[1]), equalTo(19))
    }

    @Test
    fun `messages from self use followed user style`() {
        val style = userReferenceStyle(true, false, false)
        assertThat(style, equalTo(R.style.ChatMessageText_FollowedUserReference))
    }

    @Test
    fun `messages from self talking about self use followed user style`() {
        val style = userReferenceStyle(true, true, false)
        assertThat(style, equalTo(R.style.ChatMessageText_FollowedUserReference))
    }

    @Test
    fun `messages from others talking about self use current user style`() {
        val style = userReferenceStyle(false, true, false)
        assertThat(style, equalTo(R.style.ChatMessageText_CurrentUserReference))
    }

    @Test
    fun `messages from followed users use followed user style`() {
        val style = userReferenceStyle(false, false, true)
        assertThat(style, equalTo(R.style.ChatMessageText_FollowedUserReference))
    }

    @Test
    fun `messages from non-followed users use default user style`() {
        val style = userReferenceStyle(false, false, false)
        assertThat(style, equalTo(R.style.ChatMessageText_DefaultUserReference))
    }

}
