package tv.caffeine.app.chat

import android.text.Spannable
import android.text.SpannableString
import android.text.Spanned
import android.text.style.CharacterStyle
import androidx.annotation.ColorRes
import androidx.annotation.StyleRes
import tv.caffeine.app.R
import tv.caffeine.app.api.model.Message
import tv.caffeine.app.session.FollowManager

/**
 * Highlight usernames using the span created by the spanFactory
 * Example usage:
 * ```
 * highlightUsernames(text) {
 *   ForegroundColorSpan(ContextCompat.getColor(context, R.color.username_highlight))
 * }
 * ```
 */
fun highlightUsernames(
        text: String,
        spanFactory: () -> CharacterStyle
): Spannable {
    val spannable = SpannableString(text)

    val regex = Regex("(?<=\\s|^)(@([\\w_-]{3,40}))")
    for(match in regex.findAll(text)) {
        val color = spanFactory()
        spannable.setSpan(color, match.range.start, match.range.endInclusive + 1, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
    }

    return spannable
}

fun String.mentionsUsername(username: String): Boolean {
    val regex = Regex("(?<=\\s|^)@$username")
    return regex.containsMatchIn(this)
}

@ColorRes
fun messageBackground(isSelf: Boolean, mentionsSelf: Boolean, isFollowing: Boolean) = when {
    !isSelf && mentionsSelf -> R.color.chat_bubble_orange
    isFollowing || isSelf -> R.color.chat_bubble_blue
    else -> R.color.chat_bubble_gray
}

@StyleRes
fun userReferenceStyle(isSelf: Boolean, mentionsSelf: Boolean, isFollowing: Boolean) = when {
    !isSelf && mentionsSelf -> R.style.ChatMessageText_CurrentUserReference
    isFollowing || isSelf -> R.style.ChatMessageText_FollowedUserReference
    else -> R.style.ChatMessageText_DefaultUserReference
}

val Message.endorsementTextColorResId get() = when(endorsementCount) {
    in 0..4 -> R.color.endorsement_4_text
    in 5..9 -> R.color.endorsement_5_text
    in 10..14 -> R.color.endorsement_6_text
    in 15..19 -> R.color.endorsement_7_text
    in 20..24 -> R.color.endorsement_8_text
    else -> R.color.endorsement_9_text
}

val Message.endorsementCountBackgroundResId get() = when(endorsementCount) {
    in 0..4 -> R.drawable.polygon_4_sides
    in 5..9 -> R.drawable.polygon_5_sides
    in 10..14 -> R.drawable.polygon_6_sides
    in 15..19 -> R.drawable.polygon_7_sides
    in 20..24 -> R.drawable.polygon_8_sides
    else -> R.drawable.polygon_9_sides
}

data class MessageContent(val isSelf: Boolean, val mentionsSelf: Boolean, val isFollowing: Boolean)

fun Message.classify(followManager: FollowManager): MessageContent {
    val currentUser = followManager.currentUserDetails()
    val mentionsSelf = currentUser?.let { body.text.mentionsUsername(it.username) } ?: false
    val isSelf = followManager.isSelf(publisher.caid)
    val isFollowing = followManager.isFollowing(publisher.caid)
    return MessageContent(isSelf, mentionsSelf, isFollowing)
}

@ColorRes
fun Message.chatBubbleBackground(followManager: FollowManager): Int {
    val (isSelf, mentionsSelf, isFollowing) = classify(followManager)
    return messageBackground(isSelf, mentionsSelf, isFollowing)
}

@StyleRes
fun Message.userReferenceStyle(followManager: FollowManager): Int {
    val (isSelf, mentionsSelf, isFollowing) = classify(followManager)
    return userReferenceStyle(isSelf, mentionsSelf, isFollowing)
}

