package tv.caffeine.app.stage

import android.text.style.TextAppearanceSpan
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.navigation.findNavController
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.squareup.picasso.Picasso
import tv.caffeine.app.MainNavDirections
import tv.caffeine.app.R
import tv.caffeine.app.api.model.CAID
import tv.caffeine.app.api.model.Message
import tv.caffeine.app.chat.chatBubbleBackground
import tv.caffeine.app.chat.chatMessageTextColor
import tv.caffeine.app.chat.endorsementCountBackgroundResId
import tv.caffeine.app.chat.endorsementTextColorResId
import tv.caffeine.app.chat.highlightUsernames
import tv.caffeine.app.chat.userReferenceStyle
import tv.caffeine.app.databinding.ChatMessageBubbleBinding
import tv.caffeine.app.databinding.ChatMessageBubbleReleaseBinding
import tv.caffeine.app.databinding.ChatMessageDigitalItemBinding
import tv.caffeine.app.databinding.ChatMessageDigitalItemReleaseBinding
import tv.caffeine.app.databinding.ChatMessageDummyBinding
import tv.caffeine.app.session.FollowManager
import tv.caffeine.app.settings.ReleaseDesignConfig
import tv.caffeine.app.util.UsernameTheming
import tv.caffeine.app.util.configure
import tv.caffeine.app.util.safeNavigate
import java.text.NumberFormat
import javax.inject.Inject

class ChatMessageAdapter @Inject constructor(
    private val followManager: FollowManager,
    private val picasso: Picasso,
    private val releaseDesignConfig: ReleaseDesignConfig
) : ListAdapter<Message, ChatMessageViewHolder>(
        object : DiffUtil.ItemCallback<Message>() {
            override fun areItemsTheSame(oldItem: Message, newItem: Message) = oldItem.type == newItem.type && oldItem.id == newItem.id

            override fun areContentsTheSame(oldItem: Message, newItem: Message) = oldItem == newItem
        }
) {

    interface Callback {
        fun replyClicked(message: Message)
        fun upvoteClicked(message: Message)
        fun usernameClicked(userHandle: String)
    }

    var callback: Callback? = null

    override fun getItemViewType(position: Int): Int {
        return getItem(position).type.ordinal
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ChatMessageViewHolder {
        val type = Message.Type.values()[viewType]
        val layoutInflater = LayoutInflater.from(parent.context)
        return when (type) {
            Message.Type.dummy -> DummyMessageViewHolder(ChatMessageDummyBinding.inflate(layoutInflater, parent, false))
            Message.Type.digital_item -> {
                if (releaseDesignConfig.isReleaseDesignActive()) {
                    ReleaseDigitalItemViewHolder(
                        ChatMessageDigitalItemReleaseBinding.inflate(layoutInflater, parent, false),
                        followManager, callback)
                } else {
                    ChatDigitalItemViewHolder(ChatMessageDigitalItemBinding.inflate(layoutInflater, parent, false),
                        picasso, callback, releaseDesignConfig)
                }
            }
            else -> {
                if (releaseDesignConfig.isReleaseDesignActive()) {
                    ReleaseMessageViewHolder(ChatMessageBubbleReleaseBinding.inflate(layoutInflater, parent, false),
                        followManager, callback)
                } else {
                    MessageViewHolder(ChatMessageBubbleBinding.inflate(layoutInflater, parent, false),
                        picasso, callback, releaseDesignConfig)
                }
            }
        }
    }

    override fun onBindViewHolder(holder: ChatMessageViewHolder, position: Int) {
        holder.bind(getItem(position), followManager)
    }
}

sealed class ChatMessageViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
    abstract fun bind(message: Message, followManager: FollowManager)
}

private fun View.toggleVisibility() {
    isVisible = !isVisible
}

class MessageViewHolder(
    val binding: ChatMessageBubbleBinding,
    val picasso: Picasso,
    val callback: ChatMessageAdapter.Callback?,
    val releaseDesignConfig: ReleaseDesignConfig
) : ChatMessageViewHolder(binding.root) {

    private val numberFormat = NumberFormat.getInstance()

    private fun toggleInteractionOverlayVisibility() {
        binding.interactionOverlay.toggleVisibility()
        binding.replyTextView.toggleVisibility()
        binding.upvoteTextView.toggleVisibility()
    }

    private fun hideInteractionOverlay() {
        binding.interactionOverlay.isVisible = false
        binding.replyTextView.isVisible = false
        binding.upvoteTextView.isVisible = false
    }

    override fun bind(message: Message, followManager: FollowManager) {
        val isRelease = releaseDesignConfig.isReleaseDesignActive()

        if (followManager.isSelf(message.publisher.caid)) {
            itemView.setOnClickListener(null)
        } else {
            itemView.setOnClickListener { toggleInteractionOverlayVisibility() }
        }
        hideInteractionOverlay()

        val userNameTheme = UsernameTheming.getChatTheme(isRelease)
        message.publisher.configure(binding.avatarImageView, binding.usernameTextView, null, followManager, false, null,
                R.dimen.avatar_size, userNameTheme)
        val caid = message.publisher.caid
        binding.avatarImageView.setOnClickListener { viewProfile(caid) }
        binding.usernameTextView.setOnClickListener { viewProfile(caid) }

        val chatMessageTextColor = ContextCompat.getColor(itemView.context, message.chatMessageTextColor(followManager, isRelease))
        binding.speechBubbleTextView.setTextColor(chatMessageTextColor)
        val userReferenceStyle = message.userReferenceStyle(followManager, isRelease)
        binding.speechBubbleTextView.text = highlightUsernames(message.body.text) {
            TextAppearanceSpan(itemView.context, userReferenceStyle)
        }

        val background = message.chatBubbleBackground(followManager, isRelease)
        val tintList = ContextCompat.getColorStateList(itemView.context, background)
        binding.speechBubbleTextView.backgroundTintList = tintList
        binding.speechBubbleTriangle.imageTintList = tintList
        binding.endorsementCountTextView.text = if (message.endorsementCount > 0) numberFormat.format(message.endorsementCount) else null
        binding.endorsementCountTextView.isVisible = message.endorsementCount > 0
        val endorsementTextColor = ContextCompat.getColor(itemView.context, message.endorsementTextColorResId)
        binding.endorsementCountTextView.setTextColor(endorsementTextColor)
        binding.endorsementCountTextView.setBackgroundResource(message.endorsementCountBackgroundResId)
        binding.replyTextView.setOnClickListener {
            hideInteractionOverlay()
            callback?.replyClicked(message)
        }
        binding.upvoteTextView.setOnClickListener {
            hideInteractionOverlay()
            callback?.upvoteClicked(message)
        }
    }

    private fun viewProfile(caid: CAID) {
        val action = MainNavDirections.actionGlobalProfileFragment(caid)
        itemView.findNavController().safeNavigate(action)
    }
}

class ReleaseMessageViewHolder(
    val binding: ChatMessageBubbleReleaseBinding,
    val followManager: FollowManager,
    val callback: ChatMessageAdapter.Callback?
) : ChatMessageViewHolder(binding.root) {

    private val viewModel = MessageViewModel(itemView.context, followManager, callback)

    init {
        binding.viewModel = viewModel
    }

    // TODO (david): Remove the FollowManager param.
    override fun bind(message: Message, followManager: FollowManager) {
        viewModel.updateMessage(message)
        binding.executePendingBindings()
    }
}

class ReleaseDigitalItemViewHolder(
    val binding: ChatMessageDigitalItemReleaseBinding,
    val followManager: FollowManager,
    val callback: ChatMessageAdapter.Callback?
) : ChatMessageViewHolder(binding.root) {
    private val viewModel = MessageViewModel(itemView.context, followManager, callback)

    init {
        binding.viewModel = viewModel
    }

    // TODO (david): Remove the FollowManager param.
    override fun bind(message: Message, followManager: FollowManager) {
        viewModel.updateMessage(message)
        binding.executePendingBindings()
    }
}

class ChatDigitalItemViewHolder(val binding: ChatMessageDigitalItemBinding, val picasso: Picasso, val callback: ChatMessageAdapter.Callback?, val releaseDesignConfig: ReleaseDesignConfig) : ChatMessageViewHolder(binding.root) {

    private val numberFormat = NumberFormat.getInstance()

    private fun toggleInteractionOverlayVisibility() {
        binding.interactionOverlay.toggleVisibility()
        binding.replyTextView.toggleVisibility()
        binding.upvoteTextView.toggleVisibility()
    }

    private fun hideInteractionOverlay() {
        binding.interactionOverlay.isVisible = false
        binding.replyTextView.isVisible = false
        binding.upvoteTextView.isVisible = false
    }

    override fun bind(message: Message, followManager: FollowManager) {
        val isRelease = releaseDesignConfig.isReleaseDesignActive()

        if (followManager.isSelf(message.publisher.caid)) {
            itemView.setOnClickListener(null)
        } else {
            itemView.setOnClickListener { toggleInteractionOverlayVisibility() }
        }
        hideInteractionOverlay()
        val userNameTheme = UsernameTheming.getChatTheme(isRelease)
        message.publisher.configure(binding.avatarImageView, binding.usernameTextView, null, followManager, false, null,
                R.dimen.avatar_size, userNameTheme)
        val caid = message.publisher.caid
        binding.avatarImageView.setOnClickListener { callback?.usernameClicked(caid) }
        binding.usernameTextView.setOnClickListener { callback?.usernameClicked(caid) }
        val userReferenceStyle = message.userReferenceStyle(followManager, isRelease)
        binding.speechBubbleTextView.text = highlightUsernames(message.body.text) {
            TextAppearanceSpan(itemView.context, userReferenceStyle)
        }
        binding.endorsementCountTextView.text = if (message.endorsementCount > 0) numberFormat.format(message.endorsementCount) else null
        binding.endorsementCountTextView.isVisible = message.endorsementCount > 0
        val endorsementTextColor = ContextCompat.getColor(itemView.context, message.endorsementTextColorResId)
        binding.endorsementCountTextView.setTextColor(endorsementTextColor)
        binding.endorsementCountTextView.setBackgroundResource(message.endorsementCountBackgroundResId)
        when (val digitalItem = message.body.digitalItem) {
            null -> {
                binding.digitalItemImageView.setImageDrawable(null)
                binding.quantityTextView.text = null
                binding.quantityTextView.isVisible = false
            }
            else -> {
                picasso.load(digitalItem.previewImageUrl).into(binding.digitalItemImageView)
                if (digitalItem.count > 1) {
                    binding.quantityTextView.text = itemView.resources.getString(R.string.digital_item_quantity, digitalItem.count)
                    binding.quantityTextView.isVisible = true
                } else {
                    binding.quantityTextView.text = null
                    binding.quantityTextView.isVisible = false
                }
            }
        }
        binding.replyTextView.setOnClickListener {
            hideInteractionOverlay()
            callback?.replyClicked(message)
        }
        binding.upvoteTextView.setOnClickListener {
            hideInteractionOverlay()
            callback?.upvoteClicked(message)
        }
    }
}

class DummyMessageViewHolder(binding: ChatMessageDummyBinding) : ChatMessageViewHolder(binding.root) {
    override fun bind(message: Message, followManager: FollowManager) {
    }
}
