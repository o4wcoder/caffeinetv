package tv.caffeine.app.stage

import android.content.Context
import android.text.Spannable
import android.text.style.TextAppearanceSpan
import android.view.View
import androidx.annotation.ColorRes
import androidx.core.content.ContextCompat
import tv.caffeine.app.R
import tv.caffeine.app.api.model.Message
import tv.caffeine.app.chat.chatBubbleBackground
import tv.caffeine.app.chat.chatMessageTextColor
import tv.caffeine.app.chat.classify
import tv.caffeine.app.chat.highlightUsernames
import tv.caffeine.app.chat.insertLineBreakAfterFirstMention
import tv.caffeine.app.chat.userReferenceStyle
import tv.caffeine.app.chat.usernameTextColor
import tv.caffeine.app.session.FollowManager
import tv.caffeine.app.ui.CaffeineViewModel
import java.text.NumberFormat

class MessageViewModel(
    val context: Context,
    val followManager: FollowManager,
    val callback: ChatMessageAdapter.Callback?
) : CaffeineViewModel() {

    var rawMessageText: String? = null; private set
    var messageText: Spannable? = null; private set
    var messageTextColor = getColor(R.color.white); private set
    var messageBackgroundColor = getColor(R.color.chat_bubble_dark_gray_not_follow); private set
    var username = ""; private set
    var usernameTextColor = getColor(R.color.chat_username_not_follow); private set
    var avatarImageUrl = ""; private set
    var upvoteText = ""; private set
    var upvoteTextViewVisibility = View.GONE; private set
    var upvoteHighlightVisibility = View.GONE; private set
    var upvoteBackground = getColor(R.color.chat_bubble_upvote_0_to_9); private set
    var avatarImageViewVisibility = View.VISIBLE; private set
    var highlightVisibility = View.GONE; private set
    var mentionSelfDecorationImageViewVisibility = View.GONE; private set
    var digitalItemImageUrl = ""; private set
    var digitalItemCountText: String? = null; private set
    var digitalItemCountTextVisibility = View.GONE; private set
    private var isUpvotedByMe = false

    private var isHighlightMode = false
    private var message: Message? = null

    fun updateMessage(message: Message) {
        this.message = message
        isHighlightMode = false
        isUpvotedByMe = false
        message.publisher.let {
            username = it.username
            avatarImageUrl = it.avatarImageUrl
        }
        message.body.digitalItem?.let {
            digitalItemImageUrl = it.previewImageUrl
            if (it.count > 1) {
                digitalItemCountText = context.getString(R.string.digital_item_quantity, it.count)
                digitalItemCountTextVisibility = View.VISIBLE
            } else {
                digitalItemCountText = null
                digitalItemCountTextVisibility = View.GONE
            }
        }
        rawMessageText = message.body.text
        messageText = highlightUsernames(message.body.text.insertLineBreakAfterFirstMention()) {
            TextAppearanceSpan(context, message.userReferenceStyle(followManager, true))
        }
        messageTextColor = getColor(message.chatMessageTextColor(followManager, true))
        messageBackgroundColor = getColor(message.chatBubbleBackground(followManager, true))
        usernameTextColor = getColor(message.usernameTextColor(followManager))
        mentionSelfDecorationImageViewVisibility = getVisibility(
            message.classify(followManager).let {
                it.mentionsSelf && it.isFollowing
            })

        updateUpvoteUi(message.endorsementCount)
        notifyChange()
    }

    fun onMessageClicked() {
        toggleHighlightMode()
    }

    fun onReplyClicked() {
        toggleHighlightMode()
        message?.let { callback?.replyClicked(it) }
    }

    fun onUpvoteClicked() {
        if (isHighlightMode) {
            toggleHighlightMode()
        }
        message?.let {
            callback?.upvoteClicked(it)
            isUpvotedByMe = true
        }
    }

    fun onUsernameClicked() {
        message?.publisher?.let {
            if (!followManager.isSelf(it.caid)) {
                callback?.usernameClicked(it.username)
            }
        }
    }

    private fun updateUpvoteUi(upvoteCount: Int) {
        updateHighlightMode()
        upvoteTextViewVisibility = getVisibility(upvoteCount > 0)
        upvoteText = NumberFormat.getInstance().format(upvoteCount)
        upvoteBackground = getColor(
            when (upvoteCount) {
                in 0..9 -> R.color.chat_bubble_upvote_0_to_9
                in 10..99 -> R.color.chat_bubble_upvote_10_to_99
                in 100..999 -> R.color.chat_bubble_upvote_100_to_999
                else -> R.color.chat_bubble_upvote_1000_and_above
            }
        )
    }

    private fun toggleHighlightMode() {
        isHighlightMode = !isHighlightMode
        updateHighlightMode()
    }

    private fun updateHighlightMode() {
        avatarImageViewVisibility = getVisibility(!isHighlightMode, View.INVISIBLE)
        highlightVisibility = getVisibility(isHighlightMode)

        /*
        TODO:
        We can't afford to call v2/reaper/messages/:messageIdentifier/endorsements for each message
        to check if the message is upvoted by me. We keep a temporary variable locally as a workaround
        so the user doesn't have an option to upvote the second time during a single session that the
        bubble is visible. We should remove this workaround once the new chat system includes this
        boolean via web socket.
         */
        val shouldShowUpvoteTextView = message?.endorsementCount ?: 0 > 0 &&
            (isHighlightMode && isUpvotedByMe || !isHighlightMode)
        upvoteTextViewVisibility = getVisibility(shouldShowUpvoteTextView)
        upvoteHighlightVisibility = getVisibility(isHighlightMode && !shouldShowUpvoteTextView)
        notifyChange()
    }

    private fun getVisibility(isVisible: Boolean, hideVisibility: Int = View.GONE): Int {
        return if (isVisible) View.VISIBLE else hideVisibility
    }

    private fun getColor(@ColorRes colorRes: Int) = ContextCompat.getColor(context, colorRes)
}