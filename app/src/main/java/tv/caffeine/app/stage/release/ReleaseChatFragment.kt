package tv.caffeine.app.stage.release

import tv.caffeine.app.stage.ChatFragment
import tv.caffeine.app.util.navigateToSendMessage

class ReleaseChatFragment : ChatFragment() {
    override fun setButtonLayout() {
        binding.reactButton?.setOnClickListener {
            fragmentManager?.navigateToSendMessage(
                this@ReleaseChatFragment,
                isMe
            )
        }
    }

    override fun connectFriendsWatching(stageIdentifier: String) {}
}
