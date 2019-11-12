package tv.caffeine.app.stage

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.annotation.VisibleForTesting
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.squareup.picasso.Picasso
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import org.threeten.bp.Clock
import tv.caffeine.app.MainNavDirections
import tv.caffeine.app.R
import tv.caffeine.app.api.DigitalItem
import tv.caffeine.app.api.model.Message
import tv.caffeine.app.api.model.isCAID
import tv.caffeine.app.databinding.FragmentChatBinding
import tv.caffeine.app.session.FollowManager
import tv.caffeine.app.settings.ReleaseDesignConfig
import tv.caffeine.app.stage.classic.ClassicChatFragment
import tv.caffeine.app.stage.release.ReleaseChatFragment
import tv.caffeine.app.ui.AlertDialogViewModel
import tv.caffeine.app.ui.CaffeineFragment
import tv.caffeine.app.util.navigateToDigitalItemWithMessage
import tv.caffeine.app.util.navigateToSendMessage
import tv.caffeine.app.util.safeNavigate
import tv.caffeine.app.util.setImmersiveMode
import javax.inject.Inject

private const val ARG_BROADCAST_USERNAME = "broadcastUsername"
private const val ARG_CHAT_ACTION = "chatAction"
private const val REQUEST_SHARE_BROADCAST = 100

abstract class ChatFragment : CaffeineFragment(R.layout.fragment_chat),
    SendMessageFragment.Callback, DICatalogFragment.Callback {

    @Inject lateinit var chatMessageAdapter: ChatMessageAdapter
    @Inject lateinit var followManager: FollowManager
    @Inject lateinit var picasso: Picasso
    @Inject lateinit var clock: Clock
    @Inject lateinit var releaseDesignConfig: ReleaseDesignConfig

    @VisibleForTesting
    lateinit var binding: FragmentChatBinding
    protected var isMe = false
    protected val friendsWatchingViewModel: FriendsWatchingViewModel by viewModels { viewModelFactory }
    private val chatViewModel: ChatViewModel by viewModels { viewModelFactory }
    private val args by navArgs<ChatFragmentArgs>()

    private var broadcasterUsername = ""
    private var chatJob: Job? = null

    abstract fun setButtonLayout()

    abstract fun connectFriendsWatching(stageIdentifier: String)

    companion object {
        fun newInstance(broadcasterUsername: String, isRelease: Boolean, chatAction: ChatAction? = null): ChatFragment {
            val fragment = if (isRelease) ReleaseChatFragment() else ClassicChatFragment()
            val args = Bundle()
            args.putString(ARG_BROADCAST_USERNAME, broadcasterUsername)
            args.putSerializable(ARG_CHAT_ACTION, chatAction)
            fragment.arguments = args
            return fragment
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        binding = FragmentChatBinding.bind(view)
        binding.lifecycleOwner = viewLifecycleOwner
        binding.viewModel = chatViewModel
        broadcasterUsername = args.broadcastUsername
        chatViewModel.loadUserProfile(broadcasterUsername)
            .observe(viewLifecycleOwner, Observer { userProfile ->
                userProfile?.let {
                    isMe = userProfile.isMe
                    binding.shareButton?.setOnClickListener {
                        val sharerId = followManager.currentUserDetails()?.caid
                        startActivityForResult(
                            StageShareIntentBuilder(
                                userProfile,
                                sharerId,
                                resources,
                                clock
                            ).build(), REQUEST_SHARE_BROADCAST
                        )
                    }
                }
            })

        binding.messagesRecyclerView?.adapter = chatMessageAdapter
        chatMessageAdapter.callback = object : ChatMessageAdapter.Callback {
            override fun replyClicked(message: Message) {
                val usernameMessage =
                    getString(R.string.username_prepopulated_reply, message.publisher.username)
                fragmentManager?.navigateToSendMessage(this@ChatFragment, isMe, usernameMessage)
            }

            override fun upvoteClicked(message: Message) {
                chatViewModel.endorseMessage(message)
            }

            override fun usernameClicked(userHandle: String) {
                if (userHandle.isCAID()) {
                    findNavController().safeNavigate(MainNavDirections.actionGlobalProfileFragment(userHandle))
                } else {
                    findNavController().safeNavigate(MainNavDirections.actionGlobalStagePagerFragment(userHandle))
                }
            }
        }

        binding.giftButton?.setOnClickListener {
            sendDigitalItemWithMessage(null)
        }

        setButtonLayout()

        (arguments?.getSerializable(ARG_CHAT_ACTION) as? ChatAction)?.let {
            when (it) {
                ChatAction.DIGITAL_ITEM -> sendDigitalItemWithMessage(null)
                ChatAction.MESSAGE -> showMessageDialog(isUserEmailVerified())
            }
        }
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

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == REQUEST_SHARE_BROADCAST) {
            resetStageImmersiveMode()
        }
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

    override fun sendDigitalItemWithMessage(message: String?) {
        fragmentManager?.navigateToDigitalItemWithMessage(
            this@ChatFragment,
            picasso,
            broadcasterUsername,
            message,
            releaseDesignConfig
        )
    }

    override fun sendMessage(message: String?) {
        val text = message ?: return
        chatViewModel.sendMessage(text, broadcasterUsername)
    }

    override fun digitalItemSelected(digitalItem: DigitalItem, message: String?) {
        launch {
            val userDetails = followManager.userDetails(broadcasterUsername) ?: return@launch
            val action =
                StagePagerFragmentDirections.actionStagePagerFragmentToSendDigitalItemFragment(
                    digitalItem.id,
                    userDetails.caid,
                    message
                )
            findNavController().safeNavigate(action)
        }
    }

    override fun onDismissMessageDialog() {
        resetStageImmersiveMode()
    }

    private fun resetStageImmersiveMode() = activity?.apply { setImmersiveMode() }

    fun showMessageDialog(isEmailVerified: Boolean) {
        if (isEmailVerified) {
            fragmentManager?.navigateToSendMessage(
                this@ChatFragment,
                isMe
            )
        } else {
            showVerifyEmailDialog(AlertDialogViewModel.VerificationType.React)
        }
    }
}
