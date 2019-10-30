package tv.caffeine.app.users

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.annotation.VisibleForTesting
import androidx.core.view.isVisible
import androidx.databinding.DataBindingUtil
import androidx.navigation.NavDirections
import androidx.navigation.findNavController
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import timber.log.Timber
import tv.caffeine.app.MainNavDirections
import tv.caffeine.app.R
import tv.caffeine.app.api.model.CAID
import tv.caffeine.app.api.model.CaidRecord
import tv.caffeine.app.databinding.CaidItemBinding
import tv.caffeine.app.repository.ProfileRepository
import tv.caffeine.app.session.FollowManager
import tv.caffeine.app.settings.ReleaseDesignConfig
import tv.caffeine.app.ui.FollowListAdapter
import tv.caffeine.app.ui.FollowStarViewModel
import tv.caffeine.app.ui.LiveStatusIndicatorViewModel
import tv.caffeine.app.ui.configureUserIcon
import tv.caffeine.app.ui.loadAvatar
import tv.caffeine.app.util.DispatchConfig
import tv.caffeine.app.util.ThemeColor
import tv.caffeine.app.util.UsernameTheming
import tv.caffeine.app.util.configure
import tv.caffeine.app.util.safeNavigate
import javax.inject.Inject
import kotlin.coroutines.CoroutineContext

class CaidListAdapter @Inject constructor(
    private val dispatchConfig: DispatchConfig,
    private val releaseDesignConfig: ReleaseDesignConfig,
    private val profileRepository: ProfileRepository
) : FollowListAdapter<CaidRecord, RecyclerView.ViewHolder>(
        object : DiffUtil.ItemCallback<CaidRecord>() {
            override fun areItemsTheSame(oldItem: CaidRecord, newItem: CaidRecord) = oldItem === newItem
            override fun areContentsTheSame(oldItem: CaidRecord, newItem: CaidRecord) = oldItem.caid == newItem.caid
        }
), CoroutineScope {

    private val job = SupervisorJob()
    private val exceptionHandler = CoroutineExceptionHandler { _, throwable ->
        Timber.e(throwable, "Coroutine throwable")
    }
    override val coroutineContext: CoroutineContext
        get() = dispatchConfig.main + job + exceptionHandler

    private var userNavigationCallback: UserNavigationCallback? = null
    private var usernameThemeColor: ThemeColor = ThemeColor.DARK

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder =
        if (releaseDesignConfig.isReleaseDesignActive()) {
            val context = parent.context
            val inflater = LayoutInflater.from(context)
            val binding = DataBindingUtil.inflate<CaidItemBinding>(inflater, R.layout.caid_item, parent, false)
            ReleaseCaidViewHolder(binding, this, usernameThemeColor, ::onFollowStarClick)
        } else {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.user_item_search, parent, false)
            ClassicCaidViewHolder(view, FollowManager.FollowHandler(fragmentManager, callback), this)
        }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val item = getItem(position)
        when (holder) {
            is ClassicCaidViewHolder -> holder.bind(item, followManager, userNavigationCallback)
            is ReleaseCaidViewHolder -> holder.bind(item, followManager, profileRepository, userNavigationCallback)
        }
    }

    override fun onDetachedFromRecyclerView(recyclerView: RecyclerView) {
        super.onDetachedFromRecyclerView(recyclerView)
        job.cancel()
    }

    fun setUserNavigationCallback(callback: UserNavigationCallback) {
        userNavigationCallback = callback
    }

    fun setUsernameFollowStarColor(color: ThemeColor) {
        this.usernameThemeColor = color
    }
}

class ClassicCaidViewHolder(itemView: View, private val followHandler: FollowManager.FollowHandler, private val scope: CoroutineScope) :
    RecyclerView.ViewHolder(itemView), UserNavigable {
    private lateinit var avatarImageView: ImageView
    private lateinit var usernameTextView: TextView
    private lateinit var followButton: Button

    var job: Job? = null

    fun bind(item: CaidRecord, followManager: FollowManager, userNavigationCallback: UserNavigationCallback?) {
        avatarImageView = itemView.findViewById(R.id.avatar_image_view)
        usernameTextView = itemView.findViewById(R.id.username_text_view)
        followButton = itemView.findViewById(R.id.follow_button)
        job?.cancel()
        clear()
        job = scope.launch {
            val user = followManager.userDetails(item.caid) ?: return@launch
            followButton.isVisible = item !is CaidRecord.IgnoreRecord
            val maybeFollowButton = if (item is CaidRecord.IgnoreRecord) null else followButton
            user.configure(avatarImageView, usernameTextView, maybeFollowButton, followManager, true, followHandler, R.dimen.avatar_size,
                    UsernameTheming.STANDARD)
        }
        itemView.setOnClickListener { view ->
            val action = MainNavDirections.actionGlobalProfileFragment(item.caid)
            performUserNavigation(action, userNavigationCallback, view)
        }
    }

    private fun clear() {
        avatarImageView.setImageResource(R.drawable.default_avatar_round)
        usernameTextView.text = null
        followButton.apply {
            isVisible = false
            setText(R.string.follow_button)
            setOnClickListener(null)
        }
        itemView.setOnClickListener(null)
    }
}

class ReleaseCaidViewHolder(
    private val binding: CaidItemBinding,
    private val scope: CoroutineScope,
    private val usernameThemeColor: ThemeColor,
    onFollowStarClick: (caid: CAID, isFollowing: Boolean) -> Unit
) :
    RecyclerView.ViewHolder(binding.root), UserNavigable {
    @VisibleForTesting
    var followButton: Button? = null

    init {
        binding.followStarViewModel = FollowStarViewModel(itemView.context, usernameThemeColor, onFollowStarClick)
        binding.liveStatusIndicatorViewModel = LiveStatusIndicatorViewModel()
    }

    var job: Job? = null

    /*
    If this adapter is being used in a BottomSheetDialogFragment, then it wont have a navController and the navController from the dialog's
    activity will have to be passed to this bind function.
     */
    fun bind(
        item: CaidRecord,
        followManager: FollowManager,
        profileRepository: ProfileRepository,
        userNavigationCallback: UserNavigationCallback?
    ) {
        job?.cancel()
        clear()
        job = scope.launch {
            val user = followManager.userDetails(item.caid) ?: return@launch
            val userProfile = profileRepository.getUserProfile(user.username)
            binding.avatarImageView.loadAvatar(
                user.avatarImageUrl,
                false,
                R.dimen.avatar_size,
                true
            )
            binding.usernameTextView.apply {
                text = user.username
                setTextColor(resources.getColor(usernameThemeColor.color, null))
                configureUserIcon(
                    when {
                        user.isVerified -> R.drawable.verified
                        user.isCaster -> R.drawable.caster
                        else -> 0
                    }
                )
            }

            userProfile?.let { binding.liveStatusIndicatorViewModel?.isUserLive = it.isLive }

            if (item !is CaidRecord.IgnoreRecord) {
                val isSelf = followManager.isSelf(user.caid)
                val isFollowing = followManager.isFollowing(user.caid)
                binding.followStarViewModel!!.bind(user.caid, isFollowing, isSelf)
                binding.executePendingBindings()
            }
            itemView.setOnClickListener { view ->
                userProfile?.username?.let { username ->
                    val action = MainNavDirections.actionGlobalStagePagerFragment(username)
                    performUserNavigation(action, userNavigationCallback, view)
                }
            }
        }
    }

    private fun clear() {
        binding.avatarImageView.setImageResource(R.drawable.default_avatar_round)
        binding.usernameTextView.text = null
        binding.followStarViewModel?.hide()
        binding.liveStatusIndicatorViewModel?.isUserLive = false
        binding.root.setOnClickListener(null)
    }
}

/**
 * Use the callback on screens where the nav controller is not supported.
 * E.g., [FriendsWatchingFragment] is a DialogFragment. Only [NavHostFragment] is supported.
 */
interface UserNavigationCallback {
    fun onUserNavigation(action: NavDirections) {
    }
}

/**
 * An interface to encapsulate the logic to a user stage or profile.
 * @see [UserNavigationCallback].
 */
interface UserNavigable {
    fun performUserNavigation(
        action: NavDirections,
        userNavigationCallback: UserNavigationCallback?,
        view: View
    ) {
        userNavigationCallback?.onUserNavigation(action) ?: view.findNavController().safeNavigate(action)
    }
}
