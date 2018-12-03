package tv.caffeine.app.stage

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Transformations
import kotlinx.coroutines.channels.consumeEach
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import timber.log.Timber
import tv.caffeine.app.api.Reaction
import tv.caffeine.app.api.Realtime
import tv.caffeine.app.api.UsersService
import tv.caffeine.app.api.model.Message
import tv.caffeine.app.api.model.MessageWrapper
import tv.caffeine.app.api.model.User
import tv.caffeine.app.auth.TokenStore
import tv.caffeine.app.session.FollowManager
import tv.caffeine.app.ui.CaffeineViewModel
import tv.caffeine.app.util.DispatchConfig

private const val MESSAGE_EXPIRATION_CHECK_PERIOD = 3 * 1000L // milliseconds

class ChatViewModel(
        dispatchConfig: DispatchConfig,
        private val realtime: Realtime,
        private val tokenStore: TokenStore,
        private val usersService: UsersService,
        private val followManager: FollowManager
) : CaffeineViewModel(dispatchConfig) {
    private lateinit var messageHandshake: MessageHandshake
    private val latestMessages: MutableList<MessageWrapper> = mutableListOf()

    private val _messages = MutableLiveData<List<Message>>()
    val messages: LiveData<List<Message>> = Transformations.map(_messages) { it }

    fun load(stageIdentifier: String) {
        messageHandshake = MessageHandshake(dispatchConfig, tokenStore, followManager, stageIdentifier)
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
            val caid = tokenStore.caid ?: error("Not logged in")
            val signedUserDetails = usersService.signedUserDetails(caid)
            val publisher = signedUserDetails.await().token
            val stageId = userDetails.stageId
            val message = Reaction("reaction", publisher, Message.Body(text))
            val deferred = realtime.sendMessage(stageId, message)
            val result = deferred.await()
            Timber.d("Sent message $text with result $result")
        }
    }

    private val stageReducer = StageReducer()

    private fun processMessage(messageWrapper: MessageWrapper) {
        val message = messageWrapper.message
        Timber.d("Received message (${message.type}) from ${message.publisher.username} (${message.publisher.name}): ${message.body.text}")
        val currentTime = System.currentTimeMillis()
        val preferredPositions = listOf(0, 1, 2, 3)
        val maxVisibleReactions = 4
        val position = stageReducer.determineReactionPosition(latestMessages, messageWrapper, preferredPositions, maxVisibleReactions, currentTime)
        if (position >= 0) {
            latestMessages.removeAll { it.position == position }
            latestMessages.add(messageWrapper.copy(position = position))
        }
        displayMessages()
    }

    private val dummyPublisher = User("0", "", null, "", 0, 0, false, null, "", mapOf(), null, null, "", null, null, null, false, false)
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
        val messagesToShow = 0.rangeTo(3).map { position ->
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
