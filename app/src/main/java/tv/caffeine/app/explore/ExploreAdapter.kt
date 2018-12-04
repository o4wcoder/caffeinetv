package tv.caffeine.app.explore

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.core.text.HtmlCompat
import androidx.navigation.Navigation
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import tv.caffeine.app.LobbyDirections
import tv.caffeine.app.R
import tv.caffeine.app.api.SearchUserItem
import tv.caffeine.app.di.ThemeFollowedExplore
import tv.caffeine.app.di.ThemeNotFollowedExplore
import tv.caffeine.app.session.FollowManager
import tv.caffeine.app.util.UserTheme
import tv.caffeine.app.util.compactNumberFormat
import tv.caffeine.app.util.configure
import javax.inject.Inject

abstract class UsersAdapter(
        private val followManager: FollowManager,
        private val followedTheme: UserTheme,
        private val notFollowedTheme: UserTheme
) : ListAdapter<SearchUserItem, UserViewHolder>(
        object : DiffUtil.ItemCallback<SearchUserItem?>() {
            override fun areItemsTheSame(oldItem: SearchUserItem, newItem: SearchUserItem) = oldItem === newItem
            override fun areContentsTheSame(oldItem: SearchUserItem, newItem: SearchUserItem) = oldItem.id == newItem.id
        }
) {
    abstract val userItemLayout: Int

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): UserViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(userItemLayout, parent, false)
        return UserViewHolder(view)
    }

    override fun onBindViewHolder(holder: UserViewHolder, position: Int) {
        val item = getItem(position)
        holder.bind(item, followManager, followedTheme, notFollowedTheme)
    }

}

class SearchUsersAdapter @Inject constructor(followManager: FollowManager, @ThemeFollowedExplore followedTheme: UserTheme, @ThemeNotFollowedExplore notFollowedTheme: UserTheme) : UsersAdapter(followManager, followedTheme, notFollowedTheme) {
    override val userItemLayout = R.layout.user_item_search
}

class ExploreAdapter @Inject constructor(followManager: FollowManager, @ThemeFollowedExplore followedTheme: UserTheme, @ThemeNotFollowedExplore notFollowedTheme: UserTheme) : UsersAdapter(followManager, followedTheme, notFollowedTheme) {
    override val userItemLayout = R.layout.user_item_explore
}

class UserViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
    private val avatarImageView: ImageView = itemView.findViewById(R.id.avatar_image_view)
    private val usernameTextView: TextView = itemView.findViewById(R.id.username_text_view)
    private val followButton: Button = itemView.findViewById(R.id.follow_button)
    private val numberOfFollowersTextView: TextView? = itemView.findViewById(R.id.number_of_followers_text_view)

    fun bind(item: SearchUserItem, followManager: FollowManager, followedTheme: UserTheme, notFollowedTheme: UserTheme) {
        item.user.configure(avatarImageView, usernameTextView, followButton, followManager, false, R.dimen.explore_avatar_size, followedTheme, notFollowedTheme)
        numberOfFollowersTextView?.apply {
            val followersCount = item.user.followersCount
            val compactFollowersCount = compactNumberFormat(followersCount)
            val string = itemView.resources.getQuantityString(R.plurals.number_of_followers, followersCount, compactFollowersCount)
            text = HtmlCompat.fromHtml(string, HtmlCompat.FROM_HTML_MODE_LEGACY)
        }
        itemView.setOnClickListener {
            val action = LobbyDirections.actionGlobalProfileFragment(item.user.caid)
            Navigation.findNavController(itemView).navigate(action)
        }
    }
}
