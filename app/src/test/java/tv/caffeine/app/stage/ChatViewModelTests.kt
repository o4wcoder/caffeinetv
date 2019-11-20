package tv.caffeine.app.stage

import android.content.Context
import android.content.res.Resources
import android.view.View
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
import org.junit.After
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
import tv.caffeine.app.profile.UserProfile
import tv.caffeine.app.repository.ChatRepository
import tv.caffeine.app.repository.ProfileRepository
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
    @MockK lateinit var chatRepository: ChatRepository
    @MockK lateinit var followManager: FollowManager
    @MockK lateinit var messageController: MessageController
    @MockK lateinit var signedUserToken: SignedUserToken
    @MockK lateinit var profileRepository: ProfileRepository

    @Before
    fun setup() {
        MockKAnnotations.init(this)
        every { context.resources } returns resources
        every { resources.getInteger(any()) } returns 2
        every { followManager.isSelf(any()) } returns false
        subject = ChatViewModel(
            "broadcaster",
            context,
            tokenStore,
            getSignedUserDetailsUseCase,
            followManager,
            messageController,
            chatRepository,
            profileRepository,
            mockk(relaxed = true)
        )
    }

    @After
    fun cleanup() {
        // manually stop the coroutine scope, because the subject uses an infinite loop
        subject.viewModelScope.coroutineContext.cancelChildren()
    }

    @Test
    fun `sendMessage calls realtime sendMessage`() {
        val user = getMockUser()
        coEvery { chatRepository.sendMessage(any(), any()) } returns CaffeineResult.Success(mockk())
        every { tokenStore.caid } returns "caid"
        coEvery { followManager.userDetails(any()) } returns user
        coEvery { getSignedUserDetailsUseCase(any()) } returns CaffeineResult.Success(signedUserToken)
        every { signedUserToken.token } returns "token"
        every { user.stageId } returns "stageId"
        subject.sendMessage("text", "broadcaster")
        coVerify { chatRepository.sendMessage("stageId", any()) }
    }

    @Test
    fun `endorseMessage calls realtime endorseMessage`() {
        val message = mockk<Message>()
        every { message.id } returns "messageId"
        coEvery { chatRepository.endorseMessage(any()) } returns CaffeineEmptyResult.Success
        subject.endorseMessage(message)
        coVerify { chatRepository.endorseMessage("messageId") }
    }

    @Test
    fun `rescind removes all messages by publisher`() {
        val user = getMockUser()
        val anotherUser = getAnotherMockUser()
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
    }

    @Test
    fun `reactions are all published`() = coroutinesTestRule.testDispatcher.runBlockingTest {
        val user = getMockUser()
        val anotherUser = getAnotherMockUser()
        val message1 = MessageWrapper(Message(user, "1", Message.Type.reaction, Message.Body("body1"), 0), 1, 0)
        val message2 = MessageWrapper(Message(anotherUser, "2", Message.Type.reaction, Message.Body("body2"), 0), 2, 0)
        val message3 = MessageWrapper(Message(user, "3", Message.Type.reaction, Message.Body("body3"), 0), 3, 0)
        val message4 = MessageWrapper(Message(anotherUser, "4", Message.Type.reaction, Message.Body("body4"), 0), 4, 0)
        coEvery { messageController.connect(any()) } returns flowOf(message1, message2, message3, message4)
        subject.load("stageId")
        subject.messages.observeForTesting {
            assertEquals(4, it.size)
        }
        // Need get out of coroutine scope inside blocking call
        cleanup()
    }

    @Test
    fun `the gift button is visible if the broadcaster is not me`() {
        every { followManager.isSelf(any()) } returns false
        assertEquals(View.VISIBLE, subject.getGiftButtonVisibility())
    }

    @Test
    fun `the gift button is invisible if the broadcaster is me`() {
        every { followManager.isSelf(any()) } returns true
        assertEquals(View.INVISIBLE, subject.getGiftButtonVisibility())
    }

    @Test
    fun `user profile is me and classic view is set then hide friends watching button`() {
        every { followManager.isSelf(any()) } returns true
        every { subject.releaseDesignConfig.isReleaseDesignActive() } returns false
        assertEquals(View.INVISIBLE, subject.getFriendsWatchingButtonVisibility())
    }

    @Test
    fun `user profile is not me and classic view is set then show friends watching button`() {
        every { followManager.isSelf(any()) } returns false
        every { subject.releaseDesignConfig.isReleaseDesignActive() } returns false
        assertEquals(View.VISIBLE, subject.getFriendsWatchingButtonVisibility())
    }

    @Test
    fun `user profile is not me and release view is set then hide friends watching button`() {
        val mockUserProfile = mockk<UserProfile>()
        every { mockUserProfile.isMe } returns true
        coEvery { profileRepository.getUserProfile("username") } returns mockUserProfile
        every { subject.releaseDesignConfig.isReleaseDesignActive() } returns true
        subject.loadUserProfile("username")
        subject.userProfile.observeForTesting { userProfile ->
            assertEquals(subject.getFriendsWatchingButtonVisibility(), View.GONE)
        }
    }

    @Test
    fun `do not show say something text if there are real messages`() {
        every { subject.releaseDesignConfig.isReleaseDesignActive() } returns false
        testMessageTypeVisibility(Message.Type.reaction, View.GONE, false)
        every { subject.releaseDesignConfig.isReleaseDesignActive() } returns true
        testMessageTypeVisibility(Message.Type.reaction, View.GONE, true)
    }

    @Test
    fun `show say something text if there are dummy messages`() {
        every { subject.releaseDesignConfig.isReleaseDesignActive() } returns false
        testMessageTypeVisibility(Message.Type.dummy, View.VISIBLE, false)
        every { subject.releaseDesignConfig.isReleaseDesignActive() } returns true
        testMessageTypeVisibility(Message.Type.dummy, View.VISIBLE, true)
    }

    private fun testMessageTypeVisibility(messageType: Message.Type, viewType: Int, isReleaseDesign: Boolean) {
        val user = getMockUser()
        val message = MessageWrapper(Message(user, "1", messageType, Message.Body("body1"), 0), 1, 0)
        coEvery { messageController.connect(any()) } returns flowOf(message)
        subject.load("stageId")
        subject.messages.observeForTesting {
            if (isReleaseDesign) {
                assertEquals(subject.getReleaseSaySomethingTextVisibility(), viewType)
            } else {
                assertEquals(subject.getClassicSaySomethingTextVisibility(), viewType)
            }
        }
    }

    private fun getMockUser(): User {
        val mockUser = mockk<User>()
        every { mockUser.caid } returns "CAID1"
        every { mockUser.username } returns "username1"
        every { mockUser.name } returns "name1"
        every { mockUser.stageId } returns "stageId"
        return mockUser
    }

    private fun getAnotherMockUser(): User {
        val mockUser = mockk<User>()
        every { mockUser.caid } returns "CAID2"
        every { mockUser.username } returns "username2"
        every { mockUser.name } returns "name2"
        every { mockUser.stageId } returns "stageId"
        return mockUser
    }
}
