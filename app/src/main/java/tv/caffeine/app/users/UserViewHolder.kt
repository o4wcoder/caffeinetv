package tv.caffeine.app.users

import android.widget.Button
import androidx.annotation.VisibleForTesting
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.Job
import tv.caffeine.app.MainNavDirections
import tv.caffeine.app.R
import tv.caffeine.app.api.model.CAID
import tv.caffeine.app.api.model.User
import tv.caffeine.app.databinding.CaidItemBinding
import tv.caffeine.app.session.FollowManager
import tv.caffeine.app.ui.FollowStarViewModel
import tv.caffeine.app.ui.LiveStatusIndicatorViewModel
import tv.caffeine.app.ui.configureUserIcon
import tv.caffeine.app.ui.loadAvatar
import tv.caffeine.app.util.ThemeColor

class UserViewHolder(
    private val binding: CaidItemBinding,
    private val usernameThemeColor: ThemeColor,
    onFollowStarClick: (caid: CAID, isFollowing: Boolean) -> Unit
) :
    RecyclerView.ViewHolder(binding.root),
    UserNavigable {
    @VisibleForTesting
    var followButton: Button? = null

    init {
        binding.followStarViewModel = FollowStarViewModel(
            itemView.context,
            usernameThemeColor,
            onFollowStarClick
        )
        binding.liveStatusIndicatorViewModel = LiveStatusIndicatorViewModel()
    }

    var job: Job? = null

    //TODO: forbid following for Ignore lists
    val allowFollowing: Boolean = true

    /*
    If this adapter is being used in a BottomSheetDialogFragment, then it wont have a navController and the navController from the dialog's
    activity will have to be passed to this bind function.
     */
    fun bind(
        user: User,
        followManager: FollowManager,
        userNavigationCallback: UserNavigationCallback?
    ) {
        // val userProfile = profileRepository.getUserProfile(user.username)
        binding.avatarImageView.loadAvatar(
            user.avatarImageUrl,
            false,
            R.dimen.avatar_size,
            true
        )
        binding.usernameTextView.apply {
            text = user.username
            setTextColor(resources.getColor(usernameThemeColor.color, null))
            configureUserIcon(
                when {
                    user.isVerified -> R.drawable.verified
                    user.isCaster -> R.drawable.caster
                    else -> 0
                }
            )
        }

        // userProfile?.let { binding.liveStatusIndicatorViewModel?.isUserLive = it.isLive }

        if (allowFollowing) {
            val isSelf = followManager.isSelf(user.caid)
            val isFollowing = followManager.isFollowing(user.caid)
            binding.followStarViewModel!!.bind(user.caid, isFollowing, isSelf)
            binding.executePendingBindings()
        }
        itemView.setOnClickListener { view ->
            val action =
                MainNavDirections.actionGlobalStagePagerFragment(user.username)
            performUserNavigation(action, userNavigationCallback, view)
        }
    }
}
