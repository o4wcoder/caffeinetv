package tv.caffeine.app.util

import android.text.style.UnderlineSpan
import androidx.core.text.getSpans
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import tv.caffeine.app.R
import tv.caffeine.app.chat.highlightUsernames
import tv.caffeine.app.chat.userReferenceStyle

@RunWith(RobolectricTestRunner::class)
class HighlightUsernamesTests {

    @Test
    fun `username by itself`() {
        val text = "@caffeine"
        val result = highlightUsernames(text) {
            UnderlineSpan()
        }
        val spans = result.getSpans<UnderlineSpan>()
        assertEquals(spans.size, 1)
        assertEquals(result.getSpanStart(spans[0]), 0)
        assertEquals(result.getSpanEnd(spans[0]), 9)
    }

    @Test
    fun `username with space in front`() {
        val text = " @caffeine"
        val result = highlightUsernames(text) {
            UnderlineSpan()
        }
        val spans = result.getSpans<UnderlineSpan>()
        assertEquals(spans.size, 1)
        assertEquals(result.getSpanStart(spans[0]), 1)
        assertEquals(result.getSpanEnd(spans[0]), 10)
    }

    @Test
    fun `two usernames`() {
        val text = "@caffeine and @zand present"
        val result = highlightUsernames(text) {
            UnderlineSpan()
        }
        val spans = result.getSpans<UnderlineSpan>()
        assertEquals(spans.size, 2)
        assertEquals(result.getSpanStart(spans[0]), 0)
        assertEquals(result.getSpanEnd(spans[0]), 9)
        assertEquals(result.getSpanStart(spans[1]), 14)
        assertEquals(result.getSpanEnd(spans[1]), 19)
    }

    @Test
    fun `messages from self use followed user style`() {
        val style = userReferenceStyle(true, false, false, false)
        assertEquals(style, R.style.ChatMessageText_FollowedUserReference)
    }

    @Test
    fun `messages from self talking about self use followed user style`() {
        val style = userReferenceStyle(true, true, false, false)
        assertEquals(style, R.style.ChatMessageText_FollowedUserReference)
    }

    @Test
    fun `messages from others talking about self use current user style`() {
        val style = userReferenceStyle(false, true, false, false)
        assertEquals(style, R.style.ChatMessageText_CurrentUserReference)
    }

    @Test
    fun `messages from followed users use followed user style`() {
        val style = userReferenceStyle(false, false, true, false)
        assertEquals(style, R.style.ChatMessageText_FollowedUserReference)
    }

    @Test
    fun `messages from non-followed users use default user style`() {
        val style = userReferenceStyle(false, false, false, false)
        assertEquals(style, R.style.ChatMessageText_DefaultUserReference)
    }
}

@RunWith(RobolectricTestRunner::class)
class ReleaseHighlightUsernamesTests {

    @Test
    fun `messages from self use from self style`() {
        val style = userReferenceStyle(true, false, false, true)
        assertEquals(style, R.style.StageChatText_UserReference_FromSelf)
    }

    @Test
    fun `messages that mention self use mention self style`() {
        val style = userReferenceStyle(false, true, false, true)
        assertEquals(style, R.style.StageChatText_UserReference_MentionSelf)
    }

    @Test
    fun `messages from non-followed users use default user style`() {
        val style = userReferenceStyle(false, false, false, true)
        assertEquals(style, R.style.StageChatText_UserReference)
    }
}

@RunWith(RobolectricTestRunner::class)
class UsernameColorTests {

    val usernameChatTheme = UsernameTheming.getChatTheme(false)

    @Test
    fun `verify chat message username following text color`() {
        assertEquals(usernameChatTheme.followedTheme.usernameTextAppearance, R.style.ChatMessageUsername_Following)
    }

    @Test
    fun `verify chat message username not following text color`() {
        assertEquals(usernameChatTheme.notFollowedTheme.usernameTextAppearance, R.style.ChatMessageUsername_NotFollowing)
    }

    @Test
    fun `verify chat message username current user text color`() {
        assertEquals(usernameChatTheme.currentUserTheme.usernameTextAppearance, R.style.ChatMessageUsername_NotFollowing)
    }
}

@RunWith(RobolectricTestRunner::class)
class ReleaseUsernameColorTests {

    val usernameChatTheme = UsernameTheming.getChatTheme(true)

    @Test
    fun `verify chat message username following text color`() {
        assertEquals(usernameChatTheme.followedTheme.usernameTextAppearance, R.style.ChatMessageUsername_Release_Following)
    }

    @Test
    fun `verify chat message username not following text color`() {
        assertEquals(usernameChatTheme.notFollowedTheme.usernameTextAppearance, R.style.ChatMessageUsername_Release_NotFollowing)
    }

    @Test
    fun `verify chat message username current user text color`() {
        assertEquals(usernameChatTheme.currentUserTheme.usernameTextAppearance, R.style.ChatMessageUsername_Release_CurrentUser)
    }
}
