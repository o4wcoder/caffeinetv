package tv.caffeine.app.stage.classic

import android.util.TypedValue
import androidx.lifecycle.Observer
import androidx.navigation.fragment.findNavController
import tv.caffeine.app.R
import tv.caffeine.app.api.model.User
import tv.caffeine.app.stage.ChatFragment
import tv.caffeine.app.stage.StagePagerFragmentDirections
import tv.caffeine.app.util.CropBorderedCircleTransformation
import tv.caffeine.app.util.navigateToSendMessage
import tv.caffeine.app.util.safeNavigate
import tv.caffeine.app.util.transformToClassicUI

class ClassicChatFragment : ChatFragment() {

    override fun setButtonLayout() {
        binding.chatButtonLayout.transformToClassicUI()
        binding.chatButton?.setOnClickListener {
            fragmentManager?.navigateToSendMessage(
                this@ClassicChatFragment,
                isMe
            )
        }
    }

    override fun connectFriendsWatching(stageIdentifier: String) {
        val profileAvatarTransform = CropBorderedCircleTransformation(
            resources.getColor(R.color.caffeine_blue, null),
            TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 2f, resources.displayMetrics)
        )
        friendsWatchingViewModel.load(stageIdentifier)
        friendsWatchingViewModel.friendsWatching.observe(this, Observer { friendsWatching ->
            updateFriendsWatching(friendsWatching, profileAvatarTransform)
        })
        binding.friendsWatchingButton?.setOnClickListener {
            val action =
                StagePagerFragmentDirections.actionStagePagerFragmentToFriendsWatchingFragment(
                    stageIdentifier
                )
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
}