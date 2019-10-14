package tv.caffeine.app.stage.release

import tv.caffeine.app.stage.ChatFragment
import tv.caffeine.app.util.navigateToSendMessage

class ReleaseChatFragment : ChatFragment() {
    override fun setButtonLayout() {
        binding.reactButton?.setOnClickListener {
            if (isUserEmailVerified()) {
                fragmentManager?.navigateToSendMessage(
                    this@ReleaseChatFragment,
                    isMe
                )
            } else {
                showVerifyEmailDialog()
            }
        }
    }

    override fun connectFriendsWatching(stageIdentifier: String) {}
}
