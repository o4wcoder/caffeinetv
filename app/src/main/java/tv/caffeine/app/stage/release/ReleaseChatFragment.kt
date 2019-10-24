package tv.caffeine.app.stage.release

import tv.caffeine.app.stage.ChatFragment

class ReleaseChatFragment : ChatFragment() {
    override fun setButtonLayout() {
        binding.reactButton?.setOnClickListener {
            showMessageDialog(isUserEmailVerified())
        }
    }

    override fun connectFriendsWatching(stageIdentifier: String) {}
}
