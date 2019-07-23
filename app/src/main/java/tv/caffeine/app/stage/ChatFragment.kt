package tv.caffeine.app.stage

import android.os.Bundle
import android.text.Spannable
import android.util.TypedValue
import android.view.View
import androidx.annotation.VisibleForTesting
import androidx.core.text.HtmlCompat
import androidx.core.view.isVisible
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.squareup.picasso.Picasso
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import org.threeten.bp.Clock
import tv.caffeine.app.R
import tv.caffeine.app.api.DigitalItem
import tv.caffeine.app.api.model.Message
import tv.caffeine.app.api.model.User
import tv.caffeine.app.databinding.FragmentChatBinding
import tv.caffeine.app.profile.ProfileViewModel
import tv.caffeine.app.profile.UserProfile
import tv.caffeine.app.session.FollowManager
import tv.caffeine.app.ui.CaffeineFragment
import tv.caffeine.app.util.CropBorderedCircleTransformation
import tv.caffeine.app.util.getHexColor
import tv.caffeine.app.util.navigateToDigitalItemWithMessage
import tv.caffeine.app.util.navigateToSendMessage
import tv.caffeine.app.util.safeNavigate
import javax.inject.Inject

private const val ARG_BROADCAST_USERNAME = "broadcastUsername"
class ChatFragment : CaffeineFragment(R.layout.fragment_chat), SendMessageFragment.Callback, DICatalogFragment.Callback {

    @Inject lateinit var chatMessageAdapter: ChatMessageAdapter
    @Inject lateinit var followManager: FollowManager
    @Inject lateinit var picasso: Picasso
    @Inject lateinit var clock: Clock

    private lateinit var binding: FragmentChatBinding
    private val friendsWatchingViewModel: FriendsWatchingViewModel by viewModels { viewModelFactory }
    private val chatViewModel: ChatViewModel by viewModels { viewModelFactory }
    private val profileViewModel: ProfileViewModel by viewModels { viewModelFactory }
    private val args by navArgs<ChatFragmentArgs>()
    private var isMe = false
    private var broadcasterUsername = ""
    private var chatJob: Job? = null

    companion object {
        fun newInstance(broadcasterUsername: String): ChatFragment {
            val fragment = ChatFragment()
            val args = Bundle()
            args.putString(ARG_BROADCAST_USERNAME, broadcasterUsername)
            fragment.arguments = args
            return fragment
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        binding = FragmentChatBinding.bind(view)
        broadcasterUsername = args.broadcastUsername
        profileViewModel.load(broadcasterUsername)
        profileViewModel.userProfile.observe(viewLifecycleOwner, Observer { userProfile ->
            isMe = userProfile.isMe
            updateViewsOnMyChatVisibity()
            updateSaySomethingText(userProfile)

            binding.shareButton?.setOnClickListener {
                val sharerId = followManager.currentUserDetails()?.caid
                startActivity(StageShareIntentBuilder(userProfile, sharerId, resources, clock).build())
            }
        })

        binding.messagesRecyclerView?.adapter = chatMessageAdapter

        chatMessageAdapter.callback = object : ChatMessageAdapter.Callback {
            override fun replyClicked(message: Message) {
                val usernameMessage = getString(R.string.username_prepopulated_reply, message.publisher.username)
                fragmentManager?.navigateToSendMessage(this@ChatFragment, isMe, usernameMessage)
            }
            override fun upvoteClicked(message: Message) {
                chatViewModel.endorseMessage(message)
            }
        }

        configureButtons()
    }

    override fun onResume() {
        super.onResume()
        connectMessages()
    }

    override fun onPause() {
        if (!isChangingConfigurations()) disconnectMessages()
        super.onPause()
    }

    override fun onDestroy() {
        disconnectMessages()
        super.onDestroy()
    }

    private fun connectMessages() {
        if (chatJob == null) {
            chatJob = launch {
                val userDetails = followManager.userDetails(broadcasterUsername) ?: return@launch
                launch(dispatchConfig.main) {
                    connectFriendsWatching(userDetails.stageId)
                    chatViewModel.load(userDetails.stageId)
                    chatViewModel.messages.observe(viewLifecycleOwner, Observer { messages ->
                        chatMessageAdapter.submitList(messages)
                        binding.saySomethingTextView?.isVisible = messages.all { it.type == Message.Type.dummy }
                    })
                }
            }
        }
    }

    private fun disconnectMessages() {
        chatJob?.cancel()
        chatJob = null
        chatViewModel.disconnect()
        friendsWatchingViewModel.disconnect()
    }

    @VisibleForTesting
    fun configureButtons() {
        binding.chatButton?.setOnClickListener { fragmentManager?.navigateToSendMessage(this@ChatFragment, isMe) }
        binding.giftButton?.setOnClickListener {
            sendDigitalItemWithMessage(null)
        }
    }

    private fun updateSaySomethingText(userProfile: UserProfile) {
        binding.saySomethingTextView?.text = if (isMe) {
            getString(R.string.messages_will_appear_here)
        } else {
            saySomethingToBroadcasterText(userProfile)
        }
    }

    private fun updateViewsOnMyChatVisibity() {
        listOf(
            binding.giftButton,
            binding.friendsWatchingButton
        ).forEach {
            it?.isVisible = !isMe
        }
    }

    private fun connectFriendsWatching(stageIdentifier: String) {
        val profileAvatarTransform = CropBorderedCircleTransformation(
            resources.getColor(R.color.caffeine_blue, null),
            TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 2f, resources.displayMetrics)
        )
        friendsWatchingViewModel.load(stageIdentifier)
        friendsWatchingViewModel.friendsWatching.observe(this, Observer { friendsWatching ->
            updateFriendsWatching(friendsWatching, profileAvatarTransform)
        })
        binding.friendsWatchingButton?.setOnClickListener {
            val action = StagePagerFragmentDirections.actionStagePagerFragmentToFriendsWatchingFragment(stageIdentifier)
            findNavController().safeNavigate(action)
        }
    }

    private fun updateFriendsWatching(
        friendsWatching: List<User>,
        profileAvatarTransform: CropBorderedCircleTransformation
    ) {
        if (binding.friendsWatchingButton == null) return
        val friendAvatarImageUrl = friendsWatching.firstOrNull()?.avatarImageUrl
        if (friendAvatarImageUrl == null) {
            binding.friendsWatchingButton?.isEnabled = false
            binding.friendsWatchingButton?.setImageDrawable(null)
        } else {
            binding.friendsWatchingButton?.isEnabled = true
            binding.friendsWatchingButton?.imageTintList = null
            picasso.load(friendAvatarImageUrl)
                .resizeDimen(R.dimen.avatar_friends_watching, R.dimen.avatar_friends_watching)
                .placeholder(R.drawable.ic_profile)
                .transform(profileAvatarTransform)
                .into(binding.friendsWatchingButton)
        }
    }

    private fun saySomethingToBroadcasterText(userProfile: UserProfile): Spannable {
        val colorRes = when {
            userProfile.isFollowed -> R.color.caffeine_blue
            else -> R.color.white
        }
        val fontColor = context?.getHexColor(colorRes)
        val string = getString(R.string.say_something_to_user, broadcasterUsername, fontColor)
        return HtmlCompat.fromHtml(string, HtmlCompat.FROM_HTML_MODE_LEGACY, null, null) as Spannable
    }

    override fun sendDigitalItemWithMessage(message: String?) {
        fragmentManager?.navigateToDigitalItemWithMessage(this@ChatFragment, picasso, broadcasterUsername, message)
    }

    override fun sendMessage(message: String?) {
        val text = message ?: return
        chatViewModel.sendMessage(text, broadcasterUsername)
    }

    override fun digitalItemSelected(digitalItem: DigitalItem, message: String?) {
        launch {
            val userDetails = followManager.userDetails(broadcasterUsername) ?: return@launch
            val action = StagePagerFragmentDirections.actionStagePagerFragmentToSendDigitalItemFragment(digitalItem.id, userDetails.caid, message)
            findNavController().safeNavigate(action)
        }
    }
}
