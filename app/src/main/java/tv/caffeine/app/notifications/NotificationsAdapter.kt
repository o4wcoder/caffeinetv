package tv.caffeine.app.notifications

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.core.view.isVisible
import androidx.databinding.DataBindingUtil
import androidx.navigation.findNavController
import androidx.recyclerview.widget.DiffUtil
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
import tv.caffeine.app.api.model.CaidRecord
import tv.caffeine.app.api.model.User
import tv.caffeine.app.databinding.NotificationNewFollowerBinding
import tv.caffeine.app.databinding.NotificationReceivedDigitalItemBinding
import tv.caffeine.app.repository.ProfileRepository
import tv.caffeine.app.session.FollowManager
import tv.caffeine.app.settings.ReleaseDesignConfig
import tv.caffeine.app.ui.FollowButtonDecorator
import tv.caffeine.app.ui.FollowButtonDecorator.Style
import tv.caffeine.app.ui.FollowListAdapter
import tv.caffeine.app.ui.FollowStarViewModel
import tv.caffeine.app.ui.LiveStatusIndicatorViewModel
import tv.caffeine.app.ui.configureUserIcon
import tv.caffeine.app.ui.loadAvatar
import tv.caffeine.app.util.DispatchConfig
import tv.caffeine.app.util.FollowStarColor
import tv.caffeine.app.util.UsernameTheming
import tv.caffeine.app.util.configure
import tv.caffeine.app.util.safeNavigate
import javax.inject.Inject
import kotlin.coroutines.CoroutineContext

class NotificationsAdapter @Inject constructor(
    private val dispatchConfig: DispatchConfig,
    isReleaseDesignConfig: ReleaseDesignConfig,
    private val profileRepository: ProfileRepository,
    private val picasso: Picasso
) : FollowListAdapter<CaffeineNotification, NotificationViewHolder>(
        object : DiffUtil.ItemCallback<CaffeineNotification>() {
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

    override fun getItemViewType(position: Int): Int {
        return when (getItem(position)) {
            is FollowNotification -> CellType.FOLLOW.ordinal
            is ReceivedDigitalItemNotification -> CellType.RECEIVED_DIGITAL_ITEM.ordinal
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NotificationViewHolder {
        val context = parent.context
        val inflater = LayoutInflater.from(context)
        return when (viewType) {
            CellType.FOLLOW.ordinal -> {
                if (isReleaseDesign) {
                    val binding = DataBindingUtil.inflate<NotificationNewFollowerBinding>(inflater, R.layout.notification_new_follower, parent, false)
                    FollowNotificationViewHolder(binding, this, ::onFollowStarClick)
                } else {
                    val view = LayoutInflater.from(parent.context).inflate(R.layout.notification_new_follower_classic, parent, false)
                    ClassicFollowNotificationViewHolder(view, FollowManager.FollowHandler(fragmentManager, callback), this)
                }
            }
            else -> {
                val binding = DataBindingUtil.inflate<NotificationReceivedDigitalItemBinding>(inflater, R.layout.notification_received_digital_item, parent, false)
                ReceivedDigitalItemNotificationViewHolder(binding, this)
            }
        }
    }

    override fun onBindViewHolder(holder: NotificationViewHolder, position: Int) {
        val item = getItem(position)
        when {
            holder is FollowNotificationViewHolder && item is FollowNotification ->
                holder.bind(item, followManager, profileRepository)
            holder is ClassicFollowNotificationViewHolder && item is FollowNotification ->
                holder.bind(item, followManager)
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

    fun bind(item: FollowNotification, followManager: FollowManager) {
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
                UsernameTheming.STANDARD)
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
    private val binding: NotificationNewFollowerBinding,
    private val scope: CoroutineScope,
    onFollowStarClick: (user: User, isFollowing: Boolean) -> Unit
) : NotificationViewHolder(binding.root) {
    var job: Job? = null

    init {
        binding.followStarViewModel = FollowStarViewModel(itemView.context, FollowStarColor.BLACK, onFollowStarClick)
        binding.liveStatusIndicatorViewModel = LiveStatusIndicatorViewModel()
    }

    fun bind(item: FollowNotification, followManager: FollowManager, profileRepository: ProfileRepository) {
        job?.cancel()
        clear()

        val caidRecord = item.caid
        binding.newNotificationIndicator.contentDescription = itemView.context.getString(
                if (item.isNew) R.string.unread_notification_badge_content_description
                else R.string.read_notification_badge_content_description)
        binding.newNotificationIndicator.isVisible = item.isNew
        job = scope.launch {
            val user = followManager.userDetails(caidRecord.caid) ?: return@launch
            val userProfile = profileRepository.getUserProfile(user.username)
            binding.avatarImageView.loadAvatar(user.avatarImageUrl, false, R.dimen.avatar_size)
            binding.usernameTextView.apply {
                text = user.username
                configureUserIcon(when {
                    user.isVerified -> R.drawable.verified
                    user.isCaster -> R.drawable.caster
                    else -> 0
                })
            }
            userProfile?.let { binding.liveStatusIndicatorViewModel?.isUserLive = it.isLive }

            if (caidRecord !is CaidRecord.IgnoreRecord) {
                val isFollowing = followManager.isFollowing(user.caid)
                binding.followStarViewModel!!.bind(user, isFollowing, false)
                binding.executePendingBindings()
            }
        }
        itemView.setOnClickListener {
            val action = MainNavDirections.actionGlobalProfileFragment(caidRecord.caid)
            itemView.findNavController().safeNavigate(action)
        }
    }

    private fun clear() {
        binding.avatarImageView.setImageResource(R.drawable.default_avatar_round)
        binding.usernameTextView.text = null
        itemView.setOnClickListener(null)
    }
}

class ReceivedDigitalItemNotificationViewHolder(
    private val binding: NotificationReceivedDigitalItemBinding,
    private val scope: CoroutineScope
) : NotificationViewHolder(binding.root) {
    var job: Job? = null

    init {
        binding.liveStatusIndicatorViewModel = LiveStatusIndicatorViewModel()
    }

    fun bind(item: ReceivedDigitalItemNotification, followManager: FollowManager, picasso: Picasso, profileRepository: ProfileRepository) {
        job?.cancel()
        clear()
        binding.newNotificationIndicator.contentDescription = itemView.context.getString(
            if (item.isNew) R.string.unread_notification_badge_content_description
            else R.string.read_notification_badge_content_description)

        val caid = item.digitalItem.sender
        binding.newNotificationIndicator.isVisible = item.isNew

        job = scope.launch {
            val user = followManager.userDetails(caid) ?: return@launch
            val userProfile = profileRepository.getUserProfile(user.username)
            binding.avatarImageView.loadAvatar(user.avatarImageUrl, false, R.dimen.avatar_size)
            binding.usernameTextView.apply {
                text = user.username
                configureUserIcon(when {
                    user.isVerified -> R.drawable.verified
                    user.isCaster -> R.drawable.caster
                    else -> 0
                })
            }
            userProfile?.let { binding.liveStatusIndicatorViewModel?.isUserLive = it.isLive }

            val digitalItemName = if (item.digitalItem.quantity == 1) item.digitalItem.name else item.digitalItem.pluralName
            binding.sentYouTextView.text = itemView.context.getString(R.string.received_digital_item_notification_subtitle, item.digitalItem.quantity, digitalItemName)
            picasso.load(item.digitalItem.digitalItemStaticImageUrl).into(binding.digitalItemImageView)
            binding.creditsAvailableTextView.text = "${item.digitalItem.value}"

            itemView.setOnClickListener {
                val action = MainNavDirections.actionGlobalProfileFragment(caid)
                itemView.findNavController().safeNavigate(action)
            }
        }
    }

    private fun clear() {
        binding.avatarImageView.setImageResource(R.drawable.default_avatar_round)
        binding.usernameTextView.text = null
        itemView.setOnClickListener(null)
    }
}
