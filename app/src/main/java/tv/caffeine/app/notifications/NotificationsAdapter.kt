package tv.caffeine.app.notifications

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.core.view.isVisible
import androidx.navigation.findNavController
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import timber.log.Timber
import tv.caffeine.app.LobbyDirections
import tv.caffeine.app.R
import tv.caffeine.app.api.model.CaidRecord
import tv.caffeine.app.di.ThemeFollowedExplore
import tv.caffeine.app.di.ThemeNotFollowedExplore
import tv.caffeine.app.session.FollowManager
import tv.caffeine.app.util.DispatchConfig
import tv.caffeine.app.util.UserTheme
import tv.caffeine.app.util.configure
import tv.caffeine.app.util.safeNavigate
import javax.inject.Inject
import kotlin.coroutines.CoroutineContext

class NotificationsAdapter @Inject constructor(
        private val dispatchConfig: DispatchConfig,
        private val followManager: FollowManager,
        @ThemeFollowedExplore private val followedTheme: UserTheme,
        @ThemeNotFollowedExplore private val notFollowedTheme: UserTheme
) : ListAdapter<CaffeineNotification, NotificationViewHolder>(
        object : DiffUtil.ItemCallback<CaffeineNotification?>() {
            override fun areItemsTheSame(oldItem: CaffeineNotification, newItem: CaffeineNotification) = oldItem === newItem
            override fun areContentsTheSame(oldItem: CaffeineNotification, newItem: CaffeineNotification) = when {
                oldItem is FollowNotification && newItem is FollowNotification -> oldItem.caid == newItem.caid
                else -> false
            }
        }
), CoroutineScope {

    private val job = Job()
    private val exceptionHandler = CoroutineExceptionHandler { coroutineContext, throwable ->
        Timber.e(throwable, "Coroutine throwable")
    }
    override val coroutineContext: CoroutineContext
        get() = dispatchConfig.main + job + exceptionHandler

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NotificationViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.notification_new_follower, parent, false)
        return FollowNotificationViewHolder(view, this)
    }

    override fun onBindViewHolder(holder: NotificationViewHolder, position: Int) {
        val item = getItem(position)
        when {
            holder is FollowNotificationViewHolder && item is FollowNotification ->
                holder.bind(item, followManager, followedTheme, notFollowedTheme)
            else -> TODO()
        }
    }

    override fun onDetachedFromRecyclerView(recyclerView: RecyclerView) {
        super.onDetachedFromRecyclerView(recyclerView)
        job.cancel()
    }
}

sealed class NotificationViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView)

class FollowNotificationViewHolder(itemView: View, private val scope: CoroutineScope) : NotificationViewHolder(itemView) {
    private val avatarImageView: ImageView = itemView.findViewById(R.id.avatar_image_view)
    private val usernameTextView: TextView = itemView.findViewById(R.id.username_text_view)
    private val followButton: Button = itemView.findViewById(R.id.follow_button)
    private val notificationStatusImageView: ImageView = itemView.findViewById(R.id.notification_status_image_view)

    var job: Job? = null

    fun bind(item: FollowNotification, followManager: FollowManager, followedTheme: UserTheme, notFollowedTheme: UserTheme) {
        job?.cancel()
        clear()
        val caidRecord = item.caid
        notificationStatusImageView.setImageResource(if (item.isNew) R.drawable.blue_coin else R.drawable.gray_coin)
        job = scope.launch {
            val user = followManager.userDetails(caidRecord.caid) ?: return@launch
            followButton.isVisible = caidRecord !is CaidRecord.IgnoreRecord
            val maybeFollowButton = if (caidRecord is CaidRecord.IgnoreRecord) null else followButton
            user.configure(avatarImageView, usernameTextView, maybeFollowButton, followManager, true, R.dimen.avatar_size, followedTheme, notFollowedTheme)
        }
        itemView.setOnClickListener {
            val action = LobbyDirections.actionGlobalProfileFragment(caidRecord.caid)
            itemView.findNavController().safeNavigate(action)
        }
    }

    private fun clear() {
        avatarImageView.setImageResource(R.drawable.default_avatar_round)
        usernameTextView.text = null
        usernameTextView.setCompoundDrawablesRelativeWithIntrinsicBounds(0, 0, 0, 0)
        followButton.apply {
            isVisible = false
            setText(R.string.follow_button)
            setOnClickListener(null)
        }
        itemView.setOnClickListener(null)
    }
}
