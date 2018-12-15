package tv.caffeine.app.stage

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Transformations
import com.google.gson.Gson
import kotlinx.coroutines.channels.consumeEach
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import timber.log.Timber
import tv.caffeine.app.R
import tv.caffeine.app.api.Reaction
import tv.caffeine.app.api.Realtime
import tv.caffeine.app.api.UsersService
import tv.caffeine.app.api.model.*
import tv.caffeine.app.auth.TokenStore
import tv.caffeine.app.session.FollowManager
import tv.caffeine.app.ui.CaffeineViewModel
import tv.caffeine.app.util.DispatchConfig

private const val MESSAGE_EXPIRATION_CHECK_PERIOD = 3 * 1000L // milliseconds

class ChatViewModel(
        dispatchConfig: DispatchConfig,
        context: Context,
        private val realtime: Realtime,
        private val tokenStore: TokenStore,
        private val usersService: UsersService,
        private val followManager: FollowManager,
        private val gson: Gson
) : CaffeineViewModel(dispatchConfig) {
    private lateinit var messageHandshake: MessageHandshake
    private val latestMessages: MutableList<MessageWrapper> = mutableListOf()

    private val columns = context.resources.getInteger(R.integer.chat_column_count)
    private val rows = context.resources.getInteger(R.integer.chat_row_count)

    private val maxVisibleReactions = columns * rows
    private val preferredPositions = (0 until maxVisibleReactions).toList()

    private val _messages = MutableLiveData<List<Message>>()
    val messages: LiveData<List<Message>> = Transformations.map(_messages) { it }

    fun load(stageIdentifier: String) {
        messageHandshake = MessageHandshake(dispatchConfig, tokenStore, followManager, usersService, gson, stageIdentifier)
        launch {
            while (isActive) {
                displayMessages()
                delay(MESSAGE_EXPIRATION_CHECK_PERIOD)
            }
        }
        launch {
            messageHandshake.channel.consumeEach { processMessage(it) }
        }
    }

    fun sendMessage(text: String, broadcaster: String) {
        launch {
            val userDetails = followManager.userDetails(broadcaster) ?: return@launch
            val caid = tokenStore.caid ?: return@launch Timber.e("Not logged in")
            val result = usersService.signedUserDetails(caid).awaitAndParseErrors(gson)
            val publisher = when(result) {
                is CaffeineResult.Success -> result.value.token
                is CaffeineResult.Error -> return@launch Timber.e("Failed to get signed user details")
                is CaffeineResult.Failure -> return@launch Timber.e(result.throwable)
            }
            val stageId = userDetails.stageId
            val message = Reaction("reaction", publisher, Message.Body(text))
            val result2 = realtime.sendMessage(stageId, message).awaitAndParseErrors(gson)
            when(result2) {
                is CaffeineResult.Success -> Timber.d("Sent message $text with result $result")
                is CaffeineResult.Error -> Timber.e("Failed to send message")
                is CaffeineResult.Failure -> Timber.e(result2.throwable)
            }
        }
    }

    fun endorseMessage(message: Message) {
        launch {
            val result = realtime.endorseMessage(message.id).awaitEmptyAndParseErrors(gson)
            when(result) {
                is CaffeineEmptyResult.Success -> Timber.d("Successfully endorsed a message")
                is CaffeineEmptyResult.Error -> Timber.e(result.error.toString())
                is CaffeineEmptyResult.Failure -> Timber.e(result.throwable)
            }
        }
    }

    private val stageReducer = StageReducer()

    private fun processMessage(messageWrapper: MessageWrapper) {
        val message = messageWrapper.message
        Timber.d("Received message (${message.type}) from ${message.publisher.username} (${message.publisher.name}): ${message.body.text}")
        val currentTime = System.currentTimeMillis()
        val position = stageReducer.determineReactionPosition(latestMessages, messageWrapper, preferredPositions, maxVisibleReactions, currentTime)
        if (position >= 0) {
            latestMessages.removeAll { it.position == position }
            latestMessages.add(messageWrapper.copy(position = position))
        }
        displayMessages()
    }

    private val dummyPublisher = User("0", "", null, "", 0, 0, false, null, "", mapOf(), null, null, "", null, null, null, false, false, null)
    private val dummyMessageBody = Message.Body("")
    private val dummyMessage = Message(dummyPublisher, "", Message.Type.dummy, dummyMessageBody, 0)

    private fun displayMessages() {
        val currentTime = System.currentTimeMillis()
        Timber.d("Chat Messages [$currentTime] - before $latestMessages")
        val nonStaleMessages = stageReducer.handleProcessOldReactions(latestMessages, currentTime)
        Timber.d("Chat Messages [$currentTime] - processed $nonStaleMessages")
        latestMessages.clear()
        latestMessages.addAll(nonStaleMessages)
        Timber.d("Chat Messages [$currentTime] - after $latestMessages")
        val messagesToShow = preferredPositions.map { position ->
            nonStaleMessages.find { it.position == position }?.message ?: dummyMessage.copy(id = "$position")
        }
        _messages.value = messagesToShow
        Timber.d("Chat Messages [$currentTime] - to display $messagesToShow")
    }

    override fun onCleared() {
        messageHandshake.close()
        super.onCleared()
    }

}
