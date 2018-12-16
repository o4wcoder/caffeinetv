package tv.caffeine.app.stage

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
import tv.caffeine.app.LobbyDirections
import tv.caffeine.app.R
import tv.caffeine.app.api.model.Message
import tv.caffeine.app.databinding.ChatMessageBubbleBinding
import tv.caffeine.app.databinding.ChatMessageDigitalItemBinding
import tv.caffeine.app.databinding.ChatMessageDummyBinding
import tv.caffeine.app.di.ThemeFollowedChat
import tv.caffeine.app.di.ThemeNotFollowedChat
import tv.caffeine.app.session.FollowManager
import tv.caffeine.app.util.UserTheme
import tv.caffeine.app.util.configure
import tv.caffeine.app.util.safeNavigate
import javax.inject.Inject

class ChatMessageAdapter @Inject constructor(
        private val followManager: FollowManager,
        @ThemeFollowedChat private val followedTheme: UserTheme,
        @ThemeNotFollowedChat private val notFollowedTheme: UserTheme
): ListAdapter<Message, ChatMessageViewHolder>(
        object : DiffUtil.ItemCallback<Message>() {
            override fun areItemsTheSame(oldItem: Message, newItem: Message) = oldItem.type == newItem.type && oldItem.id == newItem.id

            override fun areContentsTheSame(oldItem: Message, newItem: Message) = oldItem == newItem
        }
) {

    interface Callback {
        fun replyClicked(message: Message)
        fun upvoteClicked(message: Message)
    }

    var callback: Callback? = null

    override fun getItemViewType(position: Int): Int {
        return getItem(position).type.ordinal
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ChatMessageViewHolder {
        val type = Message.Type.values()[viewType]
        val layoutInflater = LayoutInflater.from(parent.context)
        return when(type) {
            Message.Type.dummy -> DummyMessageViewHolder(ChatMessageDummyBinding.inflate(layoutInflater, parent, false))
            Message.Type.digital_item -> ChatDigitalItemViewHolder(ChatMessageDigitalItemBinding.inflate(layoutInflater, parent, false), callback)
            else -> MessageViewHolder(ChatMessageBubbleBinding.inflate(layoutInflater, parent, false), callback)
        }
    }

    override fun onBindViewHolder(holder: ChatMessageViewHolder, position: Int) {
        holder.bind(getItem(position), followManager, followedTheme, notFollowedTheme)
    }

}

sealed class ChatMessageViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
    abstract fun bind(message: Message, followManager: FollowManager, followedTheme: UserTheme, notFollowedTheme: UserTheme)
}

private val Message.endorsementTextColorResId get() = when(endorsementCount) {
    in 0..4 -> R.color.endorsement_4_text
    in 5..9 -> R.color.endorsement_5_text
    in 10..14 -> R.color.endorsement_6_text
    in 15..19 -> R.color.endorsement_7_text
    in 20..24 -> R.color.endorsement_8_text
    else -> R.color.endorsement_9_text
}

private val Message.endorsementCountBackgroundResId get() = when(endorsementCount) {
    in 0..4 -> R.drawable.polygon_4_sides
    in 5..9 -> R.drawable.polygon_5_sides
    in 10..14 -> R.drawable.polygon_6_sides
    in 15..19 -> R.drawable.polygon_7_sides
    in 20..24 -> R.drawable.polygon_8_sides
    else -> R.drawable.polygon_9_sides
}

private fun View.toggleVisibility() {
    isVisible = !isVisible
}

class MessageViewHolder(val binding: ChatMessageBubbleBinding, val callback: ChatMessageAdapter.Callback?) : ChatMessageViewHolder(binding.root) {

    init {
        itemView.setOnClickListener { toggleInteractionOverlayVisibility() }
    }

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

    override fun bind(message: Message, followManager: FollowManager, followedTheme: UserTheme, notFollowedTheme: UserTheme) {
        hideInteractionOverlay()
        message.publisher.configure(binding.avatarImageView, binding.usernameTextView, null, followManager, false, R.dimen.avatar_size, followedTheme, notFollowedTheme)
        val caid = message.publisher.caid
        binding.avatarImageView.setOnClickListener { viewProfile(caid) }
        binding.usernameTextView.setOnClickListener { viewProfile(caid) }
        binding.speechBubbleTextView.text = message.body.text
        binding.endorsementCountTextView.text = if (message.endorsementCount > 0) message.endorsementCount.toString() else null
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

    private fun viewProfile(caid: String) {
        val action = LobbyDirections.actionGlobalProfileFragment(caid)
        itemView.findNavController().safeNavigate(action)
    }

}

class ChatDigitalItemViewHolder(val binding: ChatMessageDigitalItemBinding, val callback: ChatMessageAdapter.Callback?) : ChatMessageViewHolder(binding.root) {

    init {
        itemView.setOnClickListener { toggleInteractionOverlayVisibility() }
    }

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

    override fun bind(message: Message, followManager: FollowManager, followedTheme: UserTheme, notFollowedTheme: UserTheme) {
        hideInteractionOverlay()
        message.publisher.configure(binding.avatarImageView, binding.usernameTextView, null, followManager, false, R.dimen.avatar_size, followedTheme, notFollowedTheme)
        val caid = message.publisher.caid
        binding.avatarImageView.setOnClickListener { viewProfile(caid) }
        binding.usernameTextView.setOnClickListener { viewProfile(caid) }
        binding.speechBubbleTextView.text = message.body.text
        binding.endorsementCountTextView.text = if (message.endorsementCount > 0) message.endorsementCount.toString() else null
        binding.endorsementCountTextView.isVisible = message.endorsementCount > 0
        val endorsementTextColor = ContextCompat.getColor(itemView.context, message.endorsementTextColorResId)
        binding.endorsementCountTextView.setTextColor(endorsementTextColor)
        binding.endorsementCountTextView.setBackgroundResource(message.endorsementCountBackgroundResId)
        when(val digitalItem = message.body.digitalItem) {
            null -> binding.digitalItemImageView.setImageDrawable(null)
            else -> Picasso.get().load(digitalItem.previewImageUrl).into(binding.digitalItemImageView)
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

    private fun viewProfile(caid: String) {
        val action = LobbyDirections.actionGlobalProfileFragment(caid)
        itemView.findNavController().safeNavigate(action)
    }

}

class DummyMessageViewHolder(binding: ChatMessageDummyBinding) : ChatMessageViewHolder(binding.root) {
    override fun bind(message: Message, followManager: FollowManager, followedTheme: UserTheme, notFollowedTheme: UserTheme) {
    }
}
