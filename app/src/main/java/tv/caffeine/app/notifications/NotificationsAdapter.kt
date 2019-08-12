package tv.caffeine.app.notifications

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.core.view.isVisible
import androidx.fragment.app.FragmentManager
import androidx.navigation.findNavController
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.squareup.picasso.Picasso
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.launch
import timber.log.Timber
import tv.caffeine.app.MainNavDirections
import tv.caffeine.app.R
import tv.caffeine.app.api.digitalItemStaticImageUrl
import tv.caffeine.app.api.isMustVerifyEmailError
import tv.caffeine.app.api.model.CAID
import tv.caffeine.app.api.model.CaffeineEmptyResult
import tv.caffeine.app.api.model.CaidRecord
import tv.caffeine.app.api.model.User
import tv.caffeine.app.di.ThemeFollowedExplore
import tv.caffeine.app.di.ThemeNotFollowedExplore
import tv.caffeine.app.repository.ProfileRepository
import tv.caffeine.app.session.FollowManager
import tv.caffeine.app.settings.ReleaseDesignConfig
import tv.caffeine.app.ui.AlertDialogFragment
import tv.caffeine.app.ui.FollowButtonDecorator
import tv.caffeine.app.ui.FollowButtonDecorator.Style
import tv.caffeine.app.ui.configureUserIcon
import tv.caffeine.app.ui.loadAvatar
import tv.caffeine.app.util.DispatchConfig
import tv.caffeine.app.util.UserTheme
import tv.caffeine.app.util.configure
import tv.caffeine.app.util.maybeShow
import tv.caffeine.app.util.navigateToUnfollowUserDialog
import tv.caffeine.app.util.safeNavigate
import javax.inject.Inject
import kotlin.coroutines.CoroutineContext

class NotificationsAdapter @Inject constructor(
    private val dispatchConfig: DispatchConfig,
    private val followManager: FollowManager,
    private val isReleaseDesignConfig: ReleaseDesignConfig,
    private val profileRepository: ProfileRepository,
    @ThemeFollowedExplore private val followedTheme: UserTheme,
    @ThemeNotFollowedExplore private val notFollowedTheme: UserTheme,
    private val picasso: Picasso
) : ListAdapter<CaffeineNotification, NotificationViewHolder>(
        object : DiffUtil.ItemCallback<CaffeineNotification?>() {
            override fun areItemsTheSame(oldItem: CaffeineNotification, newItem: CaffeineNotification) = oldItem === newItem
            override fun areContentsTheSame(oldItem: CaffeineNotification, newItem: CaffeineNotification) = when {
                oldItem is FollowNotification && newItem is FollowNotification -> oldItem.caid == newItem.caid
                else -> false
            }
        }
), CoroutineScope {

    private val job = SupervisorJob()
    private val exceptionHandler = CoroutineExceptionHandler { _, throwable ->
        Timber.e(throwable, "Coroutine throwable")
    }
    override val coroutineContext: CoroutineContext
        get() = dispatchConfig.main + job + exceptionHandler

    var isReleaseDesign = isReleaseDesignConfig.isReleaseDesignActive()

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
                val notification = getItem(i) as CaffeineNotification
                if (notification is FollowNotification && notification.caid.caid == caid) {
                    notifyItemChanged(i)
                }
            }
        }
    }

    override fun getItemViewType(position: Int): Int {
        return when (getItem(position)) {
            is FollowNotification -> CellType.FOLLOW.ordinal
            is ReceivedDigitalItemNotification -> CellType.RECEIVED_DIGITAL_ITEM.ordinal
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NotificationViewHolder {
        return when (viewType) {
            CellType.FOLLOW.ordinal -> {
                val layout = if (isReleaseDesign) R.layout.notification_new_follower else R.layout.notification_new_follower_classic
                val view = LayoutInflater.from(parent.context).inflate(layout, parent, false)
                if (isReleaseDesign) FollowNotificationViewHolder(view, FollowManager.FollowHandler(fragmentManager, callback), this) else
                    ClassicFollowNotificationViewHolder(view, FollowManager.FollowHandler(fragmentManager, callback), this)
            }
            else -> {
                val view = LayoutInflater.from(parent.context).inflate(R.layout.notification_received_digital_item, parent, false)
                return ReceivedDigitalItemNotificationViewHolder(view, this)
            }
        }
    }

    override fun onBindViewHolder(holder: NotificationViewHolder, position: Int) {
        val item = getItem(position)
        when {
            holder is FollowNotificationViewHolder && item is FollowNotification ->
                holder.bind(item, followManager, profileRepository)
            holder is ClassicFollowNotificationViewHolder && item is FollowNotification ->
                holder.bind(item, followManager, followedTheme, notFollowedTheme)
            holder is ReceivedDigitalItemNotificationViewHolder && item is ReceivedDigitalItemNotification ->
                holder.bind(item, followManager, picasso, profileRepository)
            else -> TODO()
        }
    }

    override fun onDetachedFromRecyclerView(recyclerView: RecyclerView) {
        super.onDetachedFromRecyclerView(recyclerView)
        job.cancelChildren()
    }

    enum class CellType {
        FOLLOW,
        RECEIVED_DIGITAL_ITEM
    }
}

sealed class NotificationViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView)

class ClassicFollowNotificationViewHolder(
    itemView: View,
    private val followHandler: FollowManager.FollowHandler,
    private val scope: CoroutineScope
) : NotificationViewHolder(itemView) {
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
        notificationStatusImageView.contentDescription = itemView.context.getString(
            if (item.isNew) R.string.unread_notification_badge_content_description
            else R.string.read_notification_badge_content_description)
        job = scope.launch {
            val user = followManager.userDetails(caidRecord.caid) ?: return@launch
            followButton.isVisible = caidRecord !is CaidRecord.IgnoreRecord
            val maybeFollowButton = if (caidRecord is CaidRecord.IgnoreRecord) null else followButton
            user.configure(avatarImageView, usernameTextView, maybeFollowButton, followManager, true, followHandler, R.dimen.avatar_size,
                followedTheme, notFollowedTheme)
        }
        itemView.setOnClickListener {
            val action = MainNavDirections.actionGlobalProfileFragment(caidRecord.caid)
            itemView.findNavController().safeNavigate(action)
        }
    }

    private fun clear() {
        avatarImageView.setImageResource(R.drawable.default_avatar_round)
        usernameTextView.text = null
        followButton.apply {
            isVisible = false
            FollowButtonDecorator(Style.FOLLOW).decorate(this)
            setOnClickListener(null)
        }
        itemView.setOnClickListener(null)
    }
}

class FollowNotificationViewHolder(
    itemView: View,
    private val followHandler: FollowManager.FollowHandler,
    private val scope: CoroutineScope
) : NotificationViewHolder(itemView) {
    private val userIsLiveStatusView: ImageView = itemView.findViewById(R.id.user_live_status_view)
    private val avatarImageView: ImageView = itemView.findViewById(R.id.avatar_image_view)
    private val usernameTextView: TextView = itemView.findViewById(R.id.username_text_view)
    private val newNotificationIndicatorImageView: ImageView = itemView.findViewById(R.id.new_notification_indicator)
    private val followStar: ImageView = itemView.findViewById(R.id.follow_star)

    var job: Job? = null

    fun bind(item: FollowNotification, followManager: FollowManager, profileRepository: ProfileRepository) {
        job?.cancel()
        clear()

        val caidRecord = item.caid
        newNotificationIndicatorImageView.contentDescription = itemView.context.getString(
                if (item.isNew) R.string.unread_notification_badge_content_description
                else R.string.read_notification_badge_content_description)
        newNotificationIndicatorImageView.isVisible = item.isNew
        job = scope.launch {
            val user = followManager.userDetails(caidRecord.caid) ?: return@launch
            val userProfile = profileRepository.getUserProfile(user.username)
            avatarImageView.loadAvatar(user.avatarImageUrl, false, R.dimen.avatar_size)
            usernameTextView.apply {
                text = user.username
                configureUserIcon(when {
                    user.isVerified -> R.drawable.verified
                    user.isCaster -> R.drawable.caster
                    else -> 0
                })
            }

            userProfile?.let { userIsLiveStatusView.isVisible = it.isLive }

            if (caidRecord !is CaidRecord.IgnoreRecord) {
                configureFollowStar(followManager, user)
            }
        }
        itemView.setOnClickListener {
            val action = MainNavDirections.actionGlobalProfileFragment(caidRecord.caid)
            itemView.findNavController().safeNavigate(action)
        }
    }

    private fun configureFollowStar(followManager: FollowManager, user: User) {
        val isFollowing = followManager.isFollowing(user.caid)
        if (followManager.followersLoaded() && !isFollowing) {
            followStar.setImageResource(R.drawable.star_outline_black)
            followStar.setOnClickListener {
                if (followHandler != null) {
                    followHandler.callback.follow(user.caid)
                } else {
                    followStar.setImageResource(R.drawable.star_filled_black)
                    scope.launch {
                        followManager.followUser(user.caid)
                    }
                }
            }
        } else if (followManager.followersLoaded() && isFollowing) {
            followStar.setImageResource(R.drawable.star_filled_black)
            followStar.setOnClickListener {
                followHandler?.let { handler ->
                    handler.fragmentManager?.navigateToUnfollowUserDialog(user.caid, user.username, handler.callback)
                }
            }
        }
    }

    private fun clear() {
        avatarImageView.setImageResource(R.drawable.default_avatar_round)
        usernameTextView.text = null
        itemView.setOnClickListener(null)
    }
}

class ReceivedDigitalItemNotificationViewHolder(
    itemView: View,
    private val scope: CoroutineScope
) : NotificationViewHolder(itemView) {
    private val userIsLiveStatusView: ImageView = itemView.findViewById(R.id.user_live_status_view)
    private val avatarImageView: ImageView = itemView.findViewById(R.id.avatar_image_view)
    private val usernameTextView: TextView = itemView.findViewById(R.id.username_text_view)
    private val sentYouTextView: TextView = itemView.findViewById(R.id.sent_you_text_view)
    private val digitalItemImageView: ImageView = itemView.findViewById(R.id.digital_item_image_view)
    private val newNotificationIndicatorImageView: ImageView = itemView.findViewById(R.id.new_notification_indicator)
    private val creditsAvailableTextView: TextView = itemView.findViewById(R.id.credits_available_text_view)

    var job: Job? = null

    fun bind(item: ReceivedDigitalItemNotification, followManager: FollowManager, picasso: Picasso, profileRepository: ProfileRepository) {
        job?.cancel()
        clear()
        newNotificationIndicatorImageView.contentDescription = itemView.context.getString(
            if (item.isNew) R.string.unread_notification_badge_content_description
            else R.string.read_notification_badge_content_description)

        val caid = item.digitalItem.sender
        newNotificationIndicatorImageView.isVisible = item.isNew

        job = scope.launch {
            val user = followManager.userDetails(caid) ?: return@launch
            val userProfile = profileRepository.getUserProfile(user.username)
            avatarImageView.loadAvatar(user.avatarImageUrl, false, R.dimen.avatar_size)
            usernameTextView.apply {
                text = user.username
                configureUserIcon(when {
                    user.isVerified -> R.drawable.verified
                    user.isCaster -> R.drawable.caster
                    else -> 0
                })
            }
            val digitalItemName = if (item.digitalItem.quantity == 1) item.digitalItem.name else item.digitalItem.pluralName
            sentYouTextView.text = itemView.context.getString(R.string.received_digital_item_notification_subtitle, item.digitalItem.quantity, digitalItemName)
            picasso.load(item.digitalItem.digitalItemStaticImageUrl).into(digitalItemImageView)
            creditsAvailableTextView.text = "${item.digitalItem.value}"

            userProfile?.let { userIsLiveStatusView.isVisible = it.isLive }

            itemView.setOnClickListener {
                val action = MainNavDirections.actionGlobalProfileFragment(caid)
                itemView.findNavController().safeNavigate(action)
            }
        }
    }

    private fun clear() {
        avatarImageView.setImageResource(R.drawable.default_avatar_round)
        usernameTextView.text = null
        itemView.setOnClickListener(null)
    }
}
