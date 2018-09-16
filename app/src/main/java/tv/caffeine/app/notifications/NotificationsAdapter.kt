package tv.caffeine.app.notifications

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.navigation.Navigation
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.experimental.Job
import kotlinx.coroutines.experimental.android.UI
import kotlinx.coroutines.experimental.launch
import tv.caffeine.app.R
import tv.caffeine.app.api.FollowRecord
import tv.caffeine.app.di.ThemeFollowedExplore
import tv.caffeine.app.di.ThemeNotFollowedExplore
import tv.caffeine.app.session.FollowManager
import tv.caffeine.app.util.UserTheme
import tv.caffeine.app.util.configure
import javax.inject.Inject

private val diffCallback = object : DiffUtil.ItemCallback<FollowRecord?>() {
    override fun areItemsTheSame(oldItem: FollowRecord, newItem: FollowRecord) = oldItem === newItem
    override fun areContentsTheSame(oldItem: FollowRecord, newItem: FollowRecord) = oldItem.caid == newItem.caid
}

class NotificationsAdapter @Inject constructor(
        private val followManager: FollowManager,
        @ThemeFollowedExplore private val followedTheme: UserTheme,
        @ThemeNotFollowedExplore private val notFollowedTheme: UserTheme
) : ListAdapter<FollowRecord, NotificationViewHolder>(diffCallback) {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NotificationViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.notification_new_follower, parent, false)
        return NotificationViewHolder(view)
    }

    override fun onBindViewHolder(holder: NotificationViewHolder, position: Int) {
        val item = getItem(position)
        holder.bind(item, followManager, followedTheme, notFollowedTheme)
    }

}

class NotificationViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
    private val avatarImageView: ImageView = itemView.findViewById(R.id.avatar_image_view)
    private val usernameTextView: TextView = itemView.findViewById(R.id.username_text_view)
    private val followButton: Button = itemView.findViewById(R.id.follow_button)

    private var job: Job? = null

    fun bind(item: FollowRecord, followManager: FollowManager, followedTheme: UserTheme, notFollowedTheme: UserTheme) {
        job?.cancel()
        job = launch {
            val user = followManager.userDetails(item.caid)
            launch(UI) {
                user.configure(avatarImageView, usernameTextView, followButton, followManager, true, R.dimen.avatar_size, followedTheme, notFollowedTheme)
            }
        }
        itemView.setOnClickListener {
            val action = NotificationsFragmentDirections.actionNotificationsFragmentToProfileFragment(item.caid)
            Navigation.findNavController(itemView).navigate(action)
        }
    }
}
