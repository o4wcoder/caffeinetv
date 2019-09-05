package tv.caffeine.app.ui

import androidx.fragment.app.FragmentManager
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import timber.log.Timber
import tv.caffeine.app.R
import tv.caffeine.app.api.SearchUserItem
import tv.caffeine.app.api.isMustVerifyEmailError
import tv.caffeine.app.api.model.CAID
import tv.caffeine.app.api.model.CaffeineEmptyResult
import tv.caffeine.app.api.model.CaidRecord
import tv.caffeine.app.notifications.FollowNotification
import tv.caffeine.app.session.FollowManager
import tv.caffeine.app.util.maybeShow
import javax.inject.Inject

abstract class FollowListAdapter<T, VH : RecyclerView.ViewHolder> (diffCallback: DiffUtil.ItemCallback<T>) : ListAdapter<T, VH>(diffCallback),
    CoroutineScope {

    @Inject lateinit var followManager: FollowManager

    var fragmentManager: FragmentManager? = null
    val callback = object : FollowManager.Callback() {
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
                if ((getItem(i) as? CaidRecord)?.caid == caid) {
                    notifyItemChanged(i)
                } else if ((getItem(i) as? SearchUserItem)?.user?.caid == caid) {
                    notifyItemChanged(i)
                } else if ((getItem(i) as? FollowNotification)?.caid?.caid == caid) {
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