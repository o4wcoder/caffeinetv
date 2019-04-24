package tv.caffeine.app.explore

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.annotation.VisibleForTesting
import androidx.core.text.HtmlCompat
import androidx.core.view.isVisible
import androidx.fragment.app.FragmentManager
import androidx.navigation.Navigation
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import timber.log.Timber
import tv.caffeine.app.MainNavDirections
import tv.caffeine.app.R
import tv.caffeine.app.api.SearchUserItem
import tv.caffeine.app.api.isMustVerifyEmailError
import tv.caffeine.app.api.model.CAID
import tv.caffeine.app.api.model.CaffeineEmptyResult
import tv.caffeine.app.di.ThemeFollowedExplore
import tv.caffeine.app.di.ThemeNotFollowedExplore
import tv.caffeine.app.session.FollowManager
import tv.caffeine.app.ui.AlertDialogFragment
import tv.caffeine.app.util.*
import javax.inject.Inject
import kotlin.coroutines.CoroutineContext

abstract class UsersAdapter(
        private val dispatchConfig: DispatchConfig,
        private val followManager: FollowManager,
        private val followedTheme: UserTheme,
        private val notFollowedTheme: UserTheme
) : ListAdapter<SearchUserItem, UserViewHolder>(
        object : DiffUtil.ItemCallback<SearchUserItem?>() {
            override fun areItemsTheSame(oldItem: SearchUserItem, newItem: SearchUserItem) = oldItem === newItem
            override fun areContentsTheSame(oldItem: SearchUserItem, newItem: SearchUserItem) = oldItem.id == newItem.id
        }
), CoroutineScope {
    abstract val userItemLayout: Int

    private val job = SupervisorJob()
    private val exceptionHandler = CoroutineExceptionHandler { _, throwable ->
        Timber.e(throwable, "Coroutine throwable")
    }
    override val coroutineContext: CoroutineContext
        get() = dispatchConfig.main + job + exceptionHandler

    var fragmentManager: FragmentManager? = null
    val callback = object: FollowManager.Callback() {
        override fun follow(caid: CAID) {
            launch {
                val result = followManager.followUser(caid)
                when (result) {
                    is CaffeineEmptyResult.Success -> updateItem(caid)
                    is CaffeineEmptyResult.Error -> {
                        if (result.error.isMustVerifyEmailError()) {
                            val fragment = AlertDialogFragment.withMessage(R.string.verify_email_to_follow_more_users)
                            fragment.maybeShow(fragmentManager, "verifyEmail")
                        } else {
                            Timber.e("Couldn't follow user ${result.error}")
                        }
                    }
                    is CaffeineEmptyResult.Failure -> Timber.e(result.throwable)
                }
            }
        }
        override fun unfollow(caid: CAID) {
            launch {
                if (followManager.unfollowUser(caid) is CaffeineEmptyResult.Success) {
                    updateItem(caid)
                }
            }
        }

        private fun updateItem(caid: CAID) {
            for (i in 0 until itemCount) {
                if ((getItem(i) as SearchUserItem).user.caid == caid) {
                    notifyItemChanged(i)
                }
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): UserViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(userItemLayout, parent, false)
        return UserViewHolder(view, FollowManager.FollowHandler(fragmentManager, callback))
    }

    override fun onBindViewHolder(holder: UserViewHolder, position: Int) {
        val item = getItem(position)
        holder.bind(item, followManager, followedTheme, notFollowedTheme)
    }
}

class SearchUsersAdapter @Inject constructor(
        dispatchConfig: DispatchConfig,
        followManager: FollowManager,
        @ThemeFollowedExplore followedTheme: UserTheme,
        @ThemeNotFollowedExplore notFollowedTheme: UserTheme)
    : UsersAdapter(dispatchConfig, followManager, followedTheme, notFollowedTheme) {
    override val userItemLayout = R.layout.user_item_search
}

class ExploreAdapter @Inject constructor(
        dispatchConfig: DispatchConfig,
        followManager: FollowManager,
        @ThemeFollowedExplore followedTheme: UserTheme,
        @ThemeNotFollowedExplore notFollowedTheme: UserTheme)
    : UsersAdapter(dispatchConfig, followManager, followedTheme, notFollowedTheme) {
    override val userItemLayout = R.layout.user_item_explore
}

class UserViewHolder(
        itemView: View,
        private val followHandler: FollowManager.FollowHandler
) : RecyclerView.ViewHolder(itemView) {
    private val avatarImageView: ImageView = itemView.findViewById(R.id.avatar_image_view)
    private val usernameTextView: TextView = itemView.findViewById(R.id.username_text_view)
    @VisibleForTesting val followButton: Button = itemView.findViewById(R.id.follow_button)
    private val numberOfFollowersTextView: TextView? = itemView.findViewById(R.id.number_of_followers_text_view)

    fun bind(item: SearchUserItem, followManager: FollowManager, followedTheme: UserTheme, notFollowedTheme: UserTheme) {
        item.user.configure(avatarImageView, usernameTextView, followButton, followManager, true, followHandler,
                R.dimen.avatar_explore, followedTheme, notFollowedTheme)
        followButton.isVisible = !followManager.isSelf(item.id)
        numberOfFollowersTextView?.apply {
            val followersCount = item.user.followersCount
            val compactFollowersCount = compactNumberFormat(followersCount)
            val string = itemView.resources.getQuantityString(R.plurals.number_of_followers, followersCount, compactFollowersCount)
            text = HtmlCompat.fromHtml(string, HtmlCompat.FROM_HTML_MODE_LEGACY)
        }
        itemView.setOnClickListener {
            val action = MainNavDirections.actionGlobalProfileFragment(item.user.caid)
            Navigation.findNavController(itemView).safeNavigate(action)
        }
    }
}
