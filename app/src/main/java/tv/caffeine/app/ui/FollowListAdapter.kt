package tv.caffeine.app.ui

import androidx.fragment.app.FragmentManager
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import timber.log.Timber
import tv.caffeine.app.api.SearchUserItem
import tv.caffeine.app.api.model.CAID
import tv.caffeine.app.api.model.CaffeineEmptyResult
import tv.caffeine.app.api.model.CaidRecord
import tv.caffeine.app.notifications.FollowNotification
import tv.caffeine.app.session.FollowManager
import javax.inject.Inject

abstract class FollowListAdapter<T, VH : RecyclerView.ViewHolder> (diffCallback: DiffUtil.ItemCallback<T>) : ListAdapter<T, VH>(diffCallback),
    CoroutineScope {

    @Inject lateinit var followManager: FollowManager

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
                when (caid) {
                    (getItem(i) as? CaidRecord)?.caid -> notifyItemChanged(i)
                    (getItem(i) as? SearchUserItem)?.user?.caid -> notifyItemChanged(i)
                    (getItem(i) as? FollowNotification)?.caid?.caid -> notifyItemChanged(i)
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