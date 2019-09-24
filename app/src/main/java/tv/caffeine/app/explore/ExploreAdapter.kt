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
import androidx.databinding.DataBindingUtil
import androidx.navigation.Navigation
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import timber.log.Timber
import tv.caffeine.app.MainNavDirections
import tv.caffeine.app.R
import tv.caffeine.app.api.SearchUserItem
import tv.caffeine.app.api.model.CAID
import tv.caffeine.app.databinding.UserItemSearchReleaseBinding
import tv.caffeine.app.session.FollowManager
import tv.caffeine.app.settings.ReleaseDesignConfig
import tv.caffeine.app.ui.FollowListAdapter
import tv.caffeine.app.ui.FollowStarViewModel
import tv.caffeine.app.ui.LiveStatusIndicatorViewModel
import tv.caffeine.app.util.DispatchConfig
import tv.caffeine.app.util.ThemeColor
import tv.caffeine.app.util.UsernameTheming
import tv.caffeine.app.util.compactNumberFormat
import tv.caffeine.app.util.configure
import tv.caffeine.app.util.safeNavigate
import javax.inject.Inject
import kotlin.coroutines.CoroutineContext

abstract class UsersAdapter(
    private val dispatchConfig: DispatchConfig,
    protected val releaseDesignConfig: ReleaseDesignConfig
) : FollowListAdapter<SearchUserItem, UserViewHolder>(
        object : DiffUtil.ItemCallback<SearchUserItem>() {
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

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): UserViewHolder =
        if (releaseDesignConfig.isReleaseDesignActive()) {
            val context = parent.context
            val inflater = LayoutInflater.from(context)
            val binding = DataBindingUtil.inflate<UserItemSearchReleaseBinding>(inflater, R.layout.user_item_search_release, parent, false)
            ReleaseUserViewHolder(binding, FollowManager.FollowHandler(fragmentManager, callback), ::onFollowStarClick)
        } else {
            val view = LayoutInflater.from(parent.context).inflate(userItemLayout, parent, false)
            ClassicUserViewHolder(view, FollowManager.FollowHandler(fragmentManager, callback))
        }

    override fun onBindViewHolder(holder: UserViewHolder, position: Int) {
        val item = getItem(position)
        holder.bind(item, followManager)
    }
}

class SearchUsersAdapter @Inject constructor(
    dispatchConfig: DispatchConfig,
    releaseDesignConfig: ReleaseDesignConfig
) :
    UsersAdapter(dispatchConfig, releaseDesignConfig) {
    override val userItemLayout = R.layout.user_item_search
}

class ExploreAdapter @Inject constructor(
    dispatchConfig: DispatchConfig,
    releaseDesignConfig: ReleaseDesignConfig
) :
    UsersAdapter(dispatchConfig, releaseDesignConfig) {
    override val userItemLayout = R.layout.user_item_explore
}

abstract class UserViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
    abstract fun bind(
        item: SearchUserItem,
        followManager: FollowManager
    )
}

class ClassicUserViewHolder(
    itemView: View,
    private val followHandler: FollowManager.FollowHandler
) : UserViewHolder(itemView) {
    private val avatarImageView: ImageView = itemView.findViewById(R.id.avatar_image_view)
    private val usernameTextView: TextView = itemView.findViewById(R.id.username_text_view)
    @VisibleForTesting
    val followButton: Button = itemView.findViewById(R.id.follow_button)
    private val numberOfFollowersTextView: TextView? =
        itemView.findViewById(R.id.number_of_followers_text_view)

    override fun bind(item: SearchUserItem, followManager: FollowManager) {
        item.user.configure(
            avatarImageView, usernameTextView, followButton, followManager, true, followHandler,
            R.dimen.avatar_explore, UsernameTheming.STANDARD)
        followButton.isVisible = !followManager.isSelf(item.id)
        numberOfFollowersTextView?.apply {
            val followersCount = item.user.followersCount
            val compactFollowersCount = compactNumberFormat(followersCount)
            val string = itemView.resources.getQuantityString(
                R.plurals.number_of_followers,
                followersCount,
                compactFollowersCount
            )
            text = HtmlCompat.fromHtml(string, HtmlCompat.FROM_HTML_MODE_LEGACY)
        }
        itemView.setOnClickListener {
            val action = MainNavDirections.actionGlobalProfileFragment(item.user.caid)
            Navigation.findNavController(itemView).safeNavigate(action)
        }
    }
}

class ReleaseUserViewHolder(
    private val binding: UserItemSearchReleaseBinding,
    private val followHandler: FollowManager.FollowHandler,
    onFollowStarClick: (caid: CAID, isFollowing: Boolean) -> Unit
) : UserViewHolder(binding.root) {
    @VisibleForTesting
    var followButton: Button? = null

    init {
        binding.followStarViewModel = FollowStarViewModel(itemView.context, ThemeColor.LIGHT, onFollowStarClick)
        binding.liveStatusIndicatorViewModel = LiveStatusIndicatorViewModel()
    }

    override fun bind(item: SearchUserItem, followManager: FollowManager) {

        val isFollowing = followManager.isFollowing(item.user.caid)
        val isSelf = followManager.isSelf(item.id)
        binding.followStarViewModel!!.bind(item.user.caid, isFollowing, isSelf)
        binding.executePendingBindings()

        item.user.configure(
            binding.avatarImageView, binding.usernameTextView, followButton, followManager, true, followHandler,
            R.dimen.avatar_explore, UsernameTheming.STANDARD)

        item.user.isBroadcasting?.let { binding.liveStatusIndicatorViewModel?.isUserLive = it }
        itemView.setOnClickListener {
            val action = MainNavDirections.actionGlobalStagePagerFragment(item.user.username)
            Navigation.findNavController(itemView).safeNavigate(action)
        }
    }
}
