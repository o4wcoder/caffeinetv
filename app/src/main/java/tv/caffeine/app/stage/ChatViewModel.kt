package tv.caffeine.app.stage

import android.content.Context
import android.text.Spannable
import android.view.View
import android.widget.TextView
import androidx.annotation.VisibleForTesting
import androidx.core.text.HtmlCompat
import androidx.databinding.Bindable
import androidx.databinding.BindingAdapter
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.map
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import timber.log.Timber
import tv.caffeine.app.BR
import tv.caffeine.app.R
import tv.caffeine.app.api.Reaction
import tv.caffeine.app.api.model.CaffeineEmptyResult
import tv.caffeine.app.api.model.CaffeineResult
import tv.caffeine.app.api.model.Message
import tv.caffeine.app.api.model.MessageWrapper
import tv.caffeine.app.api.model.User
import tv.caffeine.app.auth.TokenStore
import tv.caffeine.app.profile.UserProfile
import tv.caffeine.app.repository.ChatRepository
import tv.caffeine.app.repository.ProfileRepository
import tv.caffeine.app.session.FollowManager
import tv.caffeine.app.settings.ReleaseDesignConfig
import tv.caffeine.app.ui.CaffeineViewModel
import tv.caffeine.app.util.getHexColor
import javax.inject.Inject

private const val MESSAGE_EXPIRATION_CHECK_PERIOD = 3 * 1000L // milliseconds

@BindingAdapter("broadcasterUserName", "userProfile")
fun setSaySomethingText(textView: TextView, broadcasterUserName: String?, userProfile: UserProfile?) {
    userProfile?.let {
        if (userProfile.isMe) {
            textView.text = textView.context.getString(R.string.messages_will_appear_here)
        } else {
            val colorRes = when {
                userProfile.isFollowed -> R.color.caffeine_blue
                else -> R.color.white
            }
            val fontColor = textView.context.getHexColor(colorRes)
            val string =
                textView.context.getString(R.string.say_something_to_user, broadcasterUserName, fontColor)
            textView.text = HtmlCompat.fromHtml(
                string,
                HtmlCompat.FROM_HTML_MODE_LEGACY,
                null,
                null
            ) as Spannable
        }
    }
}

class ChatViewModel @Inject constructor(
    context: Context,
    private val tokenStore: TokenStore,
    private val getSignedUserDetailsUseCase: GetSignedUserDetailsUseCase,
    private val followManager: FollowManager,
    private val messageController: MessageController,
    private val chatRepository: ChatRepository,
    private val profileRepository: ProfileRepository,
    @VisibleForTesting
    val releaseDesignConfig: ReleaseDesignConfig
) : CaffeineViewModel() {
    private val latestMessages: MutableList<MessageWrapper> = mutableListOf()

    private val columns = context.resources.getInteger(R.integer.chat_column_count)
    private val rows = context.resources.getInteger(R.integer.chat_row_count)

    private val maxVisibleReactions = columns * rows
    private val preferredPositions = (0 until maxVisibleReactions).toList()
    private val _messages = MutableLiveData<List<Message>>()

    private var broadcasterUserName = ""

    private val _userProfile = MutableLiveData<UserProfile>()
    val userProfile: LiveData<UserProfile> = _userProfile.map { it }
    val messages: LiveData<List<Message>> = _messages.map { it }

    @Bindable
    fun getGiftButtonVisibility() = getButtonVisibility()

    @Bindable
    fun getFriendsWatchingButtonVisibility() =
        if (releaseDesignConfig.isReleaseDesignActive()) {
            View.GONE
        } else {
            getButtonVisibility()
        }

    @Bindable
    fun getSaySomethingTextVisibility() =
        if (messages.value?.all { it.type == Message.Type.dummy } ?: true) View.VISIBLE else View.GONE

    @Bindable
    fun getBroadcasterUserName() = broadcasterUserName

    fun load(stageIdentifier: String) {
        viewModelScope.launch {
            while (isActive) {
                displayMessages()
                delay(MESSAGE_EXPIRATION_CHECK_PERIOD)
            }
        }
        viewModelScope.launch {
            messageController.connect(stageIdentifier).collect {
                processMessage(it)
            }
        }
    }

    fun loadUserProfile(broadcasterUserName: String): LiveData<UserProfile> {
        this.broadcasterUserName = broadcasterUserName
        viewModelScope.launch {
            val result = profileRepository.getUserProfile(broadcasterUserName)
            _userProfile.value = result
            notifyChange()
        }
        return _userProfile
    }

    fun sendMessage(text: String, broadcaster: String) {
        viewModelScope.launch {
            val userDetails = followManager.userDetails(broadcaster) ?: return@launch
            val caid = tokenStore.caid ?: return@launch Timber.e("Not logged in")
            val publisher = when (val result = getSignedUserDetailsUseCase(caid)) {
                is CaffeineResult.Success -> result.value.token
                is CaffeineResult.Error -> return@launch Timber.e("Failed to get signed user details")
                is CaffeineResult.Failure -> return@launch Timber.e(result.throwable)
            }
            val stageId = userDetails.stageId
            val message = Reaction("reaction", publisher, Message.Body(text))
            when (val result = chatRepository.sendMessage(stageId, message)) {
                is CaffeineResult.Success -> Timber.d("Sent message $text with result $result")
                is CaffeineResult.Error -> Timber.e("Failed to send message")
                is CaffeineResult.Failure -> Timber.e(result.throwable)
            }
        }
    }

    fun endorseMessage(message: Message) {
        viewModelScope.launch {
            when (val result = chatRepository.endorseMessage(message.id)) {
                is CaffeineEmptyResult.Success -> Timber.d("Successfully endorsed a message")
                is CaffeineEmptyResult.Error -> Timber.e(result.error.toString())
                is CaffeineEmptyResult.Failure -> Timber.e(result.throwable)
            }
        }
    }

    private fun getButtonVisibility() = if (userProfile.value?.isMe == true) View.GONE else View.VISIBLE

    private val stageReducer = StageReducer()

    private fun processMessage(messageWrapper: MessageWrapper) {
        val message = messageWrapper.message
        when (message.type) {
            Message.Type.reaction, Message.Type.digital_item, Message.Type.share -> processReaction(messageWrapper)
            Message.Type.join, Message.Type.leave, Message.Type.follow -> processPresence(messageWrapper)
            Message.Type.rescind -> processRescind(messageWrapper)
            Message.Type.dummy -> Timber.e("Not possible")
        }
    }

    private fun processReaction(messageWrapper: MessageWrapper) {
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

    private fun processPresence(messageWrapper: MessageWrapper) {
        val message = messageWrapper.message
        Timber.d("Received presence (${message.type}) from ${message.publisher.username} (${message.publisher.name}): ${message.body.text}")
    }

    private fun processRescind(messageWrapper: MessageWrapper) {
        val message = messageWrapper.message
        Timber.d("Received rescind (${message.type}) from ${message.publisher.username} (${message.publisher.name}): ${message.body.text}")
        latestMessages.removeAll { it.message.publisher.caid == message.publisher.caid }
        displayMessages()
    }

    private val dummyPublisher = User("0", "", null, null, "", 0, 0, false, false, null, "", mapOf(), null, null, "", null, null, null, false, false, null, null, false, false)
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
        notifyPropertyChanged(BR.saySomethingTextVisibility)
        Timber.d("Chat Messages [$currentTime] - to display $messagesToShow")
    }

    override fun onCleared() {
        disconnect()
        super.onCleared()
    }

    fun disconnect() {
        viewModelScope.coroutineContext.cancelChildren()
    }
}
