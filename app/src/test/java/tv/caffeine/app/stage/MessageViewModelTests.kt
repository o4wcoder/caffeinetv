package tv.caffeine.app.stage

import android.content.Context
import android.view.View
import androidx.annotation.ColorRes
import androidx.core.content.ContextCompat
import androidx.test.platform.app.InstrumentationRegistry
import io.mockk.MockKAnnotations
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.verify
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import tv.caffeine.app.R
import tv.caffeine.app.api.model.Message
import tv.caffeine.app.api.model.User
import tv.caffeine.app.session.FollowManager

@RunWith(RobolectricTestRunner::class)
class MessageViewModelTests {

    lateinit var subject: MessageViewModel
    lateinit var context: Context
    @MockK(relaxed = true) lateinit var callback: ChatMessageAdapter.Callback
    @MockK lateinit var followManager: FollowManager
    @MockK lateinit var message: Message
    @MockK lateinit var user: User
    @Before
    fun setup() {
        MockKAnnotations.init(this)
        context = InstrumentationRegistry.getInstrumentation().context
        subject = MessageViewModel(context, followManager, callback)
        every { user.caid } returns "caid123"
        every { user.username } returns "username123"
        every { user.avatarImageUrl } returns "https://avatar.com/image.jpg"
        every { message.publisher } returns user
        every { message.body.text } returns "text"
        every { message.body.digitalItem } returns null
        every { message.endorsementCount } returns 0
        every { followManager.currentUserDetails() } returns user
        every { followManager.isSelf(any()) } returns false
        every { followManager.isFollowing(any()) } returns false
    }

    @Test
    fun `clicking on the message toggles the highlight mode`() {
        subject.updateMessage(message)
        subject.onMessageClicked()
        assertEquals(View.INVISIBLE, subject.avatarImageViewVisibility)
        assertEquals(View.VISIBLE, subject.highlightVisibility)
        subject.onMessageClicked()
        assertEquals(View.VISIBLE, subject.avatarImageViewVisibility)
        assertEquals(View.GONE, subject.highlightVisibility)
    }

    @Test
    fun `clicking on the reply image exits the highlight mode and triggers the callback`() {
        subject.updateMessage(message)
        subject.onMessageClicked()
        subject.onReplyClicked()
        assertEquals(View.VISIBLE, subject.avatarImageViewVisibility)
        assertEquals(View.GONE, subject.highlightVisibility)
        verify(exactly = 1) { callback.replyClicked(any()) }
    }

    @Test
    fun `clicking on other's username triggers the callback`() {
        subject.updateMessage(message)
        subject.onUsernameClicked()
        verify(exactly = 1) { callback.usernameClicked("username123") }
    }

    @Test
    fun `clicking on my username does not trigger the callback`() {
        every { followManager.isSelf(any()) } returns true
        subject.updateMessage(message)
        subject.onUsernameClicked()
        verify(exactly = 0) { callback.usernameClicked("username123") }
    }

    @Test
    fun `clicking on the upvote image exits the highlight mode and triggers the callback`() {
        subject.updateMessage(message)
        subject.onMessageClicked()
        subject.onUpvoteClicked()
        assertEquals(View.VISIBLE, subject.avatarImageViewVisibility)
        assertEquals(View.GONE, subject.highlightVisibility)
        verify(exactly = 1) { callback.upvoteClicked(any()) }
    }

    @Test
    fun `clicking on the upvote text triggers the callback`() {
        subject.updateMessage(message)
        subject.onUpvoteClicked()
        assertEquals(View.VISIBLE, subject.avatarImageViewVisibility)
        assertEquals(View.GONE, subject.highlightVisibility)
        verify(exactly = 1) { callback.upvoteClicked(any()) }
    }

    @Test
    fun `do not show the upvote text if the count is 0`() {
        every { message.endorsementCount } returns 0
        subject.updateMessage(message)
        assertEquals(View.GONE, subject.upvoteTextViewVisbility)
    }

    @Test
    fun `show the upvote text in the correct color if the count is 9`() {
        every { message.endorsementCount } returns 9
        subject.updateMessage(message)
        assertEquals(View.VISIBLE, subject.upvoteTextViewVisbility)
        assertEquals("9", subject.upvoteText)
        assertEquals(getColor(R.color.chat_bubble_upvote_0_to_9), subject.upvoteBackground)
    }

    @Test
    fun `show the upvote text in the correct color if the count is 99`() {
        every { message.endorsementCount } returns 99
        subject.updateMessage(message)
        assertEquals(View.VISIBLE, subject.upvoteTextViewVisbility)
        assertEquals("99", subject.upvoteText)
        assertEquals(getColor(R.color.chat_bubble_upvote_10_to_99), subject.upvoteBackground)
    }

    @Test
    fun `show the upvote text in the correct color if the count is 999`() {
        every { message.endorsementCount } returns 999
        subject.updateMessage(message)
        assertEquals(View.VISIBLE, subject.upvoteTextViewVisbility)
        assertEquals("999", subject.upvoteText)
        assertEquals(getColor(R.color.chat_bubble_upvote_100_to_999), subject.upvoteBackground)
    }

    @Test
    fun `show the upvote text in the correct color if the count is 1000`() {
        every { message.endorsementCount } returns 1000
        subject.updateMessage(message)
        assertEquals(View.VISIBLE, subject.upvoteTextViewVisbility)
        assertEquals("1,000", subject.upvoteText)
        assertEquals(getColor(R.color.chat_bubble_upvote_1000_and_above), subject.upvoteBackground)
    }

    @Test
    fun `message mentions me from a friend shows the mention self decoration`() {
        every { message.body.text } returns "@username123 hi!"
        every { followManager.isFollowing(any()) } returns true
        subject.updateMessage(message)
        assertEquals(View.VISIBLE, subject.mentionSelfDecorationImageViewVisibility)
    }

    @Test
    fun `message mentions me from a stranger does not show the mention self decoration`() {
        every { message.body.text } returns "@username123 hi!"
        every { followManager.isFollowing(any()) } returns false
        subject.updateMessage(message)
        assertEquals(View.GONE, subject.mentionSelfDecorationImageViewVisibility)
    }

    @Test
    fun `message is not highlighted even if the previous message in the same bubble was highlighted`() {
        subject.updateMessage(message)
        subject.onMessageClicked()
        assertEquals(View.VISIBLE, subject.highlightVisibility)
        subject.updateMessage(message)
        assertEquals(View.GONE, subject.highlightVisibility)
    }

    @Test
    fun `the upvote text hides in the highlighted mode and shows otherwise`() {
        every { message.endorsementCount } returns 1
        subject.updateMessage(message)
        assertEquals(View.VISIBLE, subject.upvoteTextViewVisbility)
        subject.onMessageClicked()
        assertEquals(View.GONE, subject.upvoteTextViewVisbility)
        subject.onMessageClicked()
        assertEquals(View.VISIBLE, subject.upvoteTextViewVisbility)
    }

    @Test
    fun `the upvote text never shows if the count is 0`() {
        every { message.endorsementCount } returns 0
        subject.updateMessage(message)
        assertEquals(View.GONE, subject.upvoteTextViewVisbility)
        subject.onMessageClicked()
        assertEquals(View.GONE, subject.upvoteTextViewVisbility)
        subject.onMessageClicked()
        assertEquals(View.GONE, subject.upvoteTextViewVisbility)
    }

    @Test
    fun `a line break is inserted after a username is mentioned at the beginning`() {
        every { message.body.text } returns "@username123,hi!"
        subject.updateMessage(message)
        assertEquals("@username123\n,hi!", subject.messageText.toString())
    }

    @Test
    fun `a line break is inserted after a username is mentioned at the beginning and the whitespaces are removed`() {
        every { message.body.text } returns "@username123   hi!"
        subject.updateMessage(message)
        assertEquals("@username123\nhi!", subject.messageText.toString())
    }

    @Test
    fun `a line break is not inserted after a username fewer than 3 characters is mentioned at the beginning`() {
        // the caffeine username is at least 3 characters
        every { message.body.text } returns "@us hi!"
        subject.updateMessage(message)
        assertEquals("@us hi!", subject.messageText.toString())
    }

    @Test
    fun `a line break is not inserted after a username is mentioned in the middle`() {
        every { message.body.text } returns "Hi @us!"
        subject.updateMessage(message)
        assertEquals("Hi @us!", subject.messageText.toString())
    }

    private fun getColor(@ColorRes colorRes: Int) = ContextCompat.getColor(context, colorRes)
}