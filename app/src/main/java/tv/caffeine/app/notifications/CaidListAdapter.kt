package tv.caffeine.app.notifications

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.core.view.isVisible
import androidx.navigation.Navigation
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.*
import tv.caffeine.app.R
import tv.caffeine.app.api.model.CaidRecord
import tv.caffeine.app.di.ThemeFollowedExplore
import tv.caffeine.app.di.ThemeNotFollowedExplore
import tv.caffeine.app.session.FollowManager
import tv.caffeine.app.util.UserTheme
import tv.caffeine.app.util.configure
import javax.inject.Inject
import kotlin.coroutines.CoroutineContext

class CaidListAdapter @Inject constructor(
        private val followManager: FollowManager,
        @ThemeFollowedExplore private val followedTheme: UserTheme,
        @ThemeNotFollowedExplore private val notFollowedTheme: UserTheme
) : ListAdapter<CaidRecord, CaidViewHolder>(
        object : DiffUtil.ItemCallback<CaidRecord?>() {
            override fun areItemsTheSame(oldItem: CaidRecord, newItem: CaidRecord) = oldItem === newItem
            override fun areContentsTheSame(oldItem: CaidRecord, newItem: CaidRecord) = oldItem.caid == newItem.caid
        }
), CoroutineScope {
    private val job = Job()
    override val coroutineContext: CoroutineContext
        get() = Dispatchers.Main + job

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CaidViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.notification_new_follower, parent, false)
        return CaidViewHolder(view, this)
    }

    override fun onBindViewHolder(holder: CaidViewHolder, position: Int) {
        val item = getItem(position)
        holder.bind(item, followManager, followedTheme, notFollowedTheme)
    }

    override fun onDetachedFromRecyclerView(recyclerView: RecyclerView) {
        super.onDetachedFromRecyclerView(recyclerView)
        job.cancel()
    }
}

class CaidViewHolder(itemView: View, private val scope: CoroutineScope) : RecyclerView.ViewHolder(itemView) {
    private val avatarImageView: ImageView = itemView.findViewById(R.id.avatar_image_view)
    private val usernameTextView: TextView = itemView.findViewById(R.id.username_text_view)
    private val followButton: Button = itemView.findViewById(R.id.follow_button)
    private val followedYouTextView: TextView = itemView.findViewById(R.id.followed_you_text_view)

    var job: Job? = null

    fun bind(item: CaidRecord, followManager: FollowManager, followedTheme: UserTheme, notFollowedTheme: UserTheme) {
        job?.cancel()
        clear()
        job = scope.launch {
            val user = followManager.userDetails(item.caid)
            withContext(Dispatchers.Main) {
                followedYouTextView.isVisible = item is CaidRecord.FollowRecord
                user.configure(avatarImageView, usernameTextView, followButton, followManager, true, R.dimen.avatar_size, followedTheme, notFollowedTheme)
            }
        }
        itemView.setOnClickListener {
            val action = NotificationsFragmentDirections.actionNotificationsFragmentToProfileFragment(item.caid)
            Navigation.findNavController(itemView).navigate(action)
        }
    }

    private fun clear() {
        followedYouTextView.isVisible = false
        avatarImageView.setImageResource(R.drawable.default_avatar_round)
        usernameTextView.text = null
        usernameTextView.setCompoundDrawablesRelativeWithIntrinsicBounds(0, 0, 0, 0)
        followButton.setText(R.string.follow_button)
        followButton.setOnClickListener(null)
        itemView.setOnClickListener(null)
    }
}
