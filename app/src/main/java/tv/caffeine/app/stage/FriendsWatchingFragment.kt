package tv.caffeine.app.stage


import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.core.view.isVisible
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.navigation.findNavController
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import timber.log.Timber
import tv.caffeine.app.MainNavDirections
import tv.caffeine.app.R
import tv.caffeine.app.api.model.User
import tv.caffeine.app.databinding.FragmentFriendsWatchingBinding
import tv.caffeine.app.di.ThemeFollowedExplore
import tv.caffeine.app.di.ThemeNotFollowedExplore
import tv.caffeine.app.session.FollowManager
import tv.caffeine.app.ui.CaffeineBottomSheetDialogFragment
import tv.caffeine.app.util.DispatchConfig
import tv.caffeine.app.util.UserTheme
import tv.caffeine.app.util.configure
import tv.caffeine.app.util.safeNavigate
import javax.inject.Inject
import kotlin.coroutines.CoroutineContext

class FriendsWatchingFragment : CaffeineBottomSheetDialogFragment() {
    @Inject lateinit var followManager: FollowManager
    @Inject lateinit var usersAdapter: FriendsWatchingAdapter
    private val args by navArgs<FriendsWatchingFragmentArgs>()
    private val friendsWatchingViewModel: FriendsWatchingViewModel by viewModels { viewModelFactory }

    override fun getTheme() = R.style.DarkBottomSheetDialog

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        val binding = FragmentFriendsWatchingBinding.inflate(inflater, container, false)
        binding.usersRecyclerView.adapter = usersAdapter
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        usersAdapter.callback = { caid ->
            val navController = activity?.findNavController(R.id.activity_main)
            val action = MainNavDirections.actionGlobalProfileFragment(caid)
            navController?.safeNavigate(action)
            dismiss()
        }
        val stageIdentifier = args.stageIdentifier
        friendsWatchingViewModel.load(stageIdentifier)
        friendsWatchingViewModel.friendsWatching.observe(viewLifecycleOwner, Observer {
            usersAdapter.submitList(it)
        })
    }

}

class FriendsWatchingAdapter @Inject constructor(
        private val dispatchConfig: DispatchConfig,
        private val followManager: FollowManager,
        @ThemeFollowedExplore private val followedTheme: UserTheme,
        @ThemeNotFollowedExplore private val notFollowedTheme: UserTheme
) : ListAdapter<User, FriendWatchingViewHolder>(
        object : DiffUtil.ItemCallback<User?>() {
            override fun areItemsTheSame(oldItem: User, newItem: User) = oldItem === newItem
            override fun areContentsTheSame(oldItem: User, newItem: User) = oldItem.caid == newItem.caid
        }
), CoroutineScope {

    var callback: ((String) -> Unit)? = null

    private val job = SupervisorJob()
    private val exceptionHandler = CoroutineExceptionHandler { _, throwable ->
        Timber.e(throwable, "Coroutine throwable")
    }
    override val coroutineContext: CoroutineContext
        get() = dispatchConfig.main + job + exceptionHandler

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FriendWatchingViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.user_item_search, parent, false)
        return FriendWatchingViewHolder(view, followManager, followedTheme, notFollowedTheme) { caid ->
            callback?.invoke(caid)
        }
    }

    override fun onBindViewHolder(holder: FriendWatchingViewHolder, position: Int) {
        val item = getItem(position)
        holder.bind(item)
    }

    override fun onDetachedFromRecyclerView(recyclerView: RecyclerView) {
        super.onDetachedFromRecyclerView(recyclerView)
        job.cancel()
    }
}

class FriendWatchingViewHolder(
        itemView: View,
        private val followManager: FollowManager,
        private val followedTheme: UserTheme,
        private val notFollowedTheme: UserTheme,
        private val callback: (String) -> Unit
) : RecyclerView.ViewHolder(itemView) {

    private val avatarImageView: ImageView = itemView.findViewById(R.id.avatar_image_view)
    private val usernameTextView: TextView = itemView.findViewById(R.id.username_text_view)
    private val followButton: Button = itemView.findViewById(R.id.follow_button)

    fun bind(user: User) {
        followButton.isVisible = false
        user.configure(avatarImageView, usernameTextView, null, followManager, false, null, R.dimen.avatar_size,
                followedTheme, notFollowedTheme)
        itemView.setOnClickListener { callback(user.caid) }
    }

}
