package tv.caffeine.app.users

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
import tv.caffeine.app.LobbyDirections
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
        val view = LayoutInflater.from(parent.context).inflate(R.layout.user_item_search, parent, false)
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

    var job: Job? = null

    fun bind(item: CaidRecord, followManager: FollowManager, followedTheme: UserTheme, notFollowedTheme: UserTheme) {
        job?.cancel()
        clear()
        job = scope.launch {
            val user = followManager.userDetails(item.caid)
            withContext(Dispatchers.Main) {
                followButton.isVisible = item !is CaidRecord.IgnoreRecord
                val maybeFollowButton = if (item is CaidRecord.IgnoreRecord) null else followButton
                user.configure(avatarImageView, usernameTextView, maybeFollowButton, followManager, true, R.dimen.avatar_size, followedTheme, notFollowedTheme)
            }
        }
        itemView.setOnClickListener {
            val action = LobbyDirections.actionGlobalProfileFragment(item.caid)
            Navigation.findNavController(itemView).navigate(action)
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
