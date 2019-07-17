package tv.caffeine.app.stage

import android.content.Context
import android.content.res.Resources
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.lifecycle.viewModelScope
import io.mockk.MockKAnnotations
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.mockk
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runBlockingTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import tv.caffeine.app.api.model.CaffeineEmptyResult
import tv.caffeine.app.api.model.CaffeineResult
import tv.caffeine.app.api.model.Message
import tv.caffeine.app.api.model.MessageWrapper
import tv.caffeine.app.api.model.SignedUserToken
import tv.caffeine.app.api.model.User
import tv.caffeine.app.auth.TokenStore
import tv.caffeine.app.session.FollowManager
import tv.caffeine.app.util.CoroutinesTestRule
import tv.caffeine.app.test.observeForTesting

class ChatViewModelTests {
    @get:Rule val instantTaskExecutorRule = InstantTaskExecutorRule()
    @get:Rule val coroutinesTestRule = CoroutinesTestRule()

    lateinit var subject: ChatViewModel
    @MockK lateinit var context: Context
    @MockK lateinit var resources: Resources
    @MockK lateinit var tokenStore: TokenStore
    @MockK lateinit var getSignedUserDetailsUseCase: GetSignedUserDetailsUseCase
    @MockK lateinit var sendMessageUseCase: SendMessageUseCase
    @MockK lateinit var endorseMessageUseCase: EndorseMessageUseCase
    @MockK lateinit var followManager: FollowManager
    @MockK lateinit var messageController: MessageController
    @MockK lateinit var user: User
    @MockK lateinit var signedUserToken: SignedUserToken

    @Before
    fun setup() {
        MockKAnnotations.init(this)
        every { context.resources } returns resources
        every { resources.getInteger(any()) } returns 2
        subject = ChatViewModel(context, tokenStore, getSignedUserDetailsUseCase, sendMessageUseCase, endorseMessageUseCase, followManager, messageController)
    }

    @Test
    fun `sendMessage calls realtime sendMessage`() {
        coEvery { sendMessageUseCase(any(), any()) } returns CaffeineResult.Success(mockk())
        every { tokenStore.caid } returns "caid"
        coEvery { followManager.userDetails(any()) } returns user
        coEvery { getSignedUserDetailsUseCase(any()) } returns CaffeineResult.Success(signedUserToken)
        every { signedUserToken.token } returns "token"
        every { user.stageId } returns "stageId"
        subject.sendMessage("text", "broadcaster")
        coVerify { sendMessageUseCase("stageId", any()) }
    }

    @Test
    fun `endorseMessage calls realtime endorseMessage`() {
        val message = mockk<Message>()
        every { message.id } returns "messageId"
        coEvery { endorseMessageUseCase(any()) } returns CaffeineEmptyResult.Success
        subject.endorseMessage(message)
        coVerify { endorseMessageUseCase("messageId") }
    }

    @Test
    fun `rescind removes all messages by publisher`() {
        every { user.caid } returns "CAID1"
        every { user.username } returns "username1"
        every { user.name } returns "name1"
        val anotherUser = mockk<User>()
        every { anotherUser.caid } returns "CAID2"
        every { anotherUser.username } returns "username2"
        every { anotherUser.name } returns "name2"
        val message1 = MessageWrapper(Message(user, "1", Message.Type.reaction, Message.Body("body1"), 0), 1, 0)
        val message2 = MessageWrapper(Message(anotherUser, "2", Message.Type.reaction, Message.Body("body2"), 0), 2, 0)
        val message3 = MessageWrapper(Message(user, "3", Message.Type.reaction, Message.Body("body3"), 0), 3, 0)
        val message4 = MessageWrapper(Message(user, "4", Message.Type.rescind, Message.Body("body4"), 0), 4, 0)
        coEvery { messageController.connect(any()) } returns flowOf(message1, message2, message3, message4)
        subject.load("stageId")
        subject.messages.observeForTesting { list ->
            val nonDummyMessages = list.filter { it.type != Message.Type.dummy }
            assertEquals(1, nonDummyMessages.size)
            assertEquals("CAID2", nonDummyMessages.first().publisher.caid)
        }
        // manually stop the coroutine scope, because the subject uses an infinite loop
        subject.viewModelScope.coroutineContext.cancelChildren()
    }

    @Test
    fun `reactions are all published`() = coroutinesTestRule.testDispatcher.runBlockingTest {
        every { user.caid } returns "CAID1"
        every { user.username } returns "username1"
        every { user.name } returns "name1"
        val anotherUser = mockk<User>()
        every { anotherUser.caid } returns "CAID2"
        every { anotherUser.username } returns "username2"
        every { anotherUser.name } returns "name2"
        val message1 = MessageWrapper(Message(user, "1", Message.Type.reaction, Message.Body("body1"), 0), 1, 0)
        val message2 = MessageWrapper(Message(anotherUser, "2", Message.Type.reaction, Message.Body("body2"), 0), 2, 0)
        val message3 = MessageWrapper(Message(user, "3", Message.Type.reaction, Message.Body("body3"), 0), 3, 0)
        val message4 = MessageWrapper(Message(anotherUser, "4", Message.Type.reaction, Message.Body("body4"), 0), 4, 0)
        coEvery { messageController.connect(any()) } returns flowOf(message1, message2, message3, message4)
        subject.load("stageId")
        subject.messages.observeForTesting {
            assertEquals(4, it.size)
        }
        // manually stop the coroutine scope, because the subject uses an infinite loop
        subject.viewModelScope.coroutineContext.cancelChildren()
    }
}
