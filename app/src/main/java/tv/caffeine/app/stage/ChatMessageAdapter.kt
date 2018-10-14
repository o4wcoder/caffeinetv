package tv.caffeine.app.stage

import android.view.LayoutInflater
import android.view.ViewGroup
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
import tv.caffeine.app.di.ThemeFollowedChat
import tv.caffeine.app.di.ThemeNotFollowedChat
import tv.caffeine.app.session.FollowManager
import tv.caffeine.app.util.UserTheme
import tv.caffeine.app.util.configure
import javax.inject.Inject

class ChatMessageAdapter @Inject constructor(
        private val followManager: FollowManager,
        @ThemeFollowedChat private val followedTheme: UserTheme,
        @ThemeNotFollowedChat private val notFollowedTheme: UserTheme
): ListAdapter<Message, MessageViewHolder>(
        object : DiffUtil.ItemCallback<Message>() {
            override fun areItemsTheSame(oldItem: Message, newItem: Message) = oldItem.id == newItem.id

            override fun areContentsTheSame(oldItem: Message, newItem: Message) = oldItem == newItem
        }
) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MessageViewHolder {
        val binding = ChatMessageBubbleBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return MessageViewHolder(binding)
    }

    override fun onBindViewHolder(holder: MessageViewHolder, position: Int) {
        holder.bind(getItem(position), followManager, followedTheme, notFollowedTheme)
    }

}


class MessageViewHolder(val binding: ChatMessageBubbleBinding) : RecyclerView.ViewHolder(binding.root) {

    fun bind(message: Message, followManager: FollowManager, followedTheme: UserTheme, notFollowedTheme: UserTheme) {
        message.publisher.configure(binding.avatarImageView, binding.usernameTextView, null, followManager, false, R.dimen.avatar_size, followedTheme, notFollowedTheme)
        val caid = message.publisher.caid
        binding.avatarImageView.setOnClickListener { viewProfile(caid) }
        binding.usernameTextView.setOnClickListener { viewProfile(caid) }
        binding.speechBubbleTextView.text = message.body.text
        binding.endorsementCountTextView.text = if (message.endorsementCount > 0) message.endorsementCount.toString() else null
        binding.endorsementCountTextView.isVisible = message.endorsementCount > 0
        message.body.digitalItem?.let { digitalItem ->
            Picasso.get()
                    .load(digitalItem.staticImageUrl)
                    .into(binding.digitalItemImageView)
        } ?: binding.digitalItemImageView.setImageDrawable(null)
    }

    private fun viewProfile(caid: String) {
        val action = LobbyDirections.actionGlobalProfileFragment(caid)
        itemView.findNavController().navigate(action)
    }

}