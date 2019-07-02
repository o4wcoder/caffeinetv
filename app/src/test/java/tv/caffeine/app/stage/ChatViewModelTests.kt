package tv.caffeine.app.stage

import android.content.Context
import android.content.res.Resources
import io.mockk.MockKAnnotations
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.newSingleThreadContext
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test
import tv.caffeine.app.api.model.CaffeineEmptyResult
import tv.caffeine.app.api.model.CaffeineResult
import tv.caffeine.app.api.model.Message
import tv.caffeine.app.api.model.SignedUserToken
import tv.caffeine.app.api.model.User
import tv.caffeine.app.auth.TokenStore
import tv.caffeine.app.session.FollowManager

class ChatViewModelTests {
    private val mainThreadSurrogate = newSingleThreadContext("UI thread")

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
        Dispatchers.setMain(mainThreadSurrogate)
    }

    @After
    fun teardown() {
        Dispatchers.resetMain()
        mainThreadSurrogate.close()
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
}
