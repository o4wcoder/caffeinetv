package tv.caffeine.app.users

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.FragmentManager
import androidx.paging.PagedListAdapter
import androidx.recyclerview.widget.DiffUtil
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import timber.log.Timber
import tv.caffeine.app.R
import tv.caffeine.app.api.model.CAID
import tv.caffeine.app.api.model.CaffeineEmptyResult
import tv.caffeine.app.api.model.User
import tv.caffeine.app.databinding.CaidItemBinding
import tv.caffeine.app.session.FollowManager
import tv.caffeine.app.util.DispatchConfig
import tv.caffeine.app.util.ThemeColor
import javax.inject.Inject
import kotlin.coroutines.CoroutineContext

class UserListAdapter @Inject constructor(
    private val dispatchConfig: DispatchConfig,
    private val followManager: FollowManager
) : PagedListAdapter<User, UserViewHolder>(
    object : DiffUtil.ItemCallback<User>() {
        override fun areItemsTheSame(oldItem: User, newItem: User) = oldItem.caid === newItem.caid
        override fun areContentsTheSame(oldItem: User, newItem: User) = oldItem == newItem
    }
), CoroutineScope {

    private val job = SupervisorJob()
    private val exceptionHandler = CoroutineExceptionHandler { _, throwable ->
        Timber.e(throwable, "Coroutine throwable")
    }
    override val coroutineContext: CoroutineContext
        get() = dispatchConfig.main + job + exceptionHandler

    var usernameThemeColor = ThemeColor.LIGHT
    var userNavigationCallback: UserNavigationCallback? = null

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): UserViewHolder {
        val context = parent.context
        val inflater = LayoutInflater.from(context)
        val binding = DataBindingUtil.inflate<CaidItemBinding>(inflater, R.layout.caid_item, parent, false)
        return UserViewHolder(binding, usernameThemeColor, ::onFollowStarClick)
    }

    override fun onBindViewHolder(holder: UserViewHolder, position: Int) {
        val item = getItem(position)
        if (item != null) {
            holder.bind(item, followManager, userNavigationCallback)
        }
    }

    var fragmentManager: FragmentManager? = null
    val callback = object : FollowManager.Callback() {
        override fun follow(caid: CAID) {
            updateItem(caid)
            launch {
                val result = followManager.followUser(caid)
                if (result !is CaffeineEmptyResult.Success) {
                    updateItem(caid)
                    when (result) {
                        is CaffeineEmptyResult.Error -> Timber.e("Couldn't follow user ${result.error}")
                        is CaffeineEmptyResult.Failure -> Timber.e(result.throwable)
                    }
                }
            }
        }
        override fun unfollow(caid: CAID) {
            updateItem(caid)
            launch {
                val result = followManager.unfollowUser(caid)
                if (result !is CaffeineEmptyResult.Success) updateItem(caid)
            }
        }

        private fun updateItem(caid: CAID) {
            for (i in 0 until itemCount) {
                if (getItem(i)?.caid == caid) {
                    notifyItemChanged(i)
                }
            }
        }
    }

    fun onFollowStarClick(caid: CAID, isFollowing: Boolean) {
        if (followManager.followersLoaded()) {
            val handler = FollowManager.FollowHandler(fragmentManager, callback)
            if (isFollowing) {
                handler.callback.unfollow(caid)
            } else {
                handler.callback.follow(caid)
            }
        }
    }
}

