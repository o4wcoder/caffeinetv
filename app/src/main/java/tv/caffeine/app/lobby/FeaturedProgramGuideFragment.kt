package tv.caffeine.app.lobby

import android.os.Bundle
import android.transition.TransitionManager
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.viewModels
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import androidx.lifecycle.Transformations
import androidx.navigation.Navigation
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.google.gson.Gson
import com.squareup.picasso.Picasso
import kotlinx.coroutines.*
import timber.log.Timber
import tv.caffeine.app.MainNavDirections
import tv.caffeine.app.R
import tv.caffeine.app.api.BroadcastsService
import tv.caffeine.app.api.FeaturedGuideListing
import tv.caffeine.app.api.model.CAID
import tv.caffeine.app.api.model.CaffeineResult
import tv.caffeine.app.api.model.User
import tv.caffeine.app.api.model.awaitAndParseErrors
import tv.caffeine.app.databinding.FeaturedGuideItemBinding
import tv.caffeine.app.databinding.FragmentFeaturedProgramGuideBinding
import tv.caffeine.app.di.ThemeFollowedExplore
import tv.caffeine.app.di.ThemeNotFollowedExplore
import tv.caffeine.app.lobby.FeaturedGuideItem.ListingItem
import tv.caffeine.app.session.FollowManager
import tv.caffeine.app.ui.CaffeineFragment
import tv.caffeine.app.ui.CaffeineViewModel
import tv.caffeine.app.util.*
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import kotlin.coroutines.CoroutineContext

class FeaturedProgramGuideFragment : CaffeineFragment() {

    @Inject lateinit var guideAdapter: GuideAdapter
    @Inject lateinit var gson: Gson
    private var binding: FragmentFeaturedProgramGuideBinding? = null
    private val viewModel: FeaturedProgramGuideViewModel by viewModels { viewModelFactory }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return FragmentFeaturedProgramGuideBinding.inflate(inflater, container, false).run {
            configure(this)
            binding = this
            root
        }
    }

    override fun onDestroyView() {
        binding?.guideRecyclerView?.adapter = null
        binding = null
        super.onDestroyView()
    }

    private fun configure(binding: FragmentFeaturedProgramGuideBinding) {
        binding.lifecycleOwner = viewLifecycleOwner
        binding.guideRecyclerView.adapter = guideAdapter
        viewModel.listings.observe(viewLifecycleOwner, Observer {
            binding.emptyMessageTextView.isVisible = it.isEmpty()
            guideAdapter.submitList(it)
        })
    }
}

class FeaturedProgramGuideViewModel(
        dispatchConfig: DispatchConfig,
        private val broadcastsService: BroadcastsService,
        private val followManager: FollowManager,
        private val gson: Gson
) : CaffeineViewModel(dispatchConfig) {

    private val _listings = MutableLiveData<List<ListingItem>>()
    val listings: LiveData<List<ListingItem>> = Transformations.map(_listings) { it }

    init {
        load()
    }

    private fun load() {
        launch {
            val result = broadcastsService.featuredGuide().awaitAndParseErrors(gson)
            when (result) {
                is CaffeineResult.Success -> {
                    // TODO [AND-164] Improve how FollowManager refreshes the followed users list
                    followManager.refreshFollowedUsers()
                    _listings.value = prepareList(result.value.listings)
                }
                is CaffeineResult.Error -> Timber.e("Failed to fetch content guide ${result.error}")
                is CaffeineResult.Failure -> Timber.e(result.throwable)
            }
        }
    }

    private fun prepareList(listings: List<FeaturedGuideListing>): List<ListingItem> {
        val listingItems = listings.map { ListingItem(it) }.apply {
            getOrNull(0)?.isExpanded = true
        }
        return prepareListingTimestamp(listingItems)
    }

    private fun prepareListingTimestamp(listingItems: List<ListingItem>): List<ListingItem> {
        listingItems.getOrNull(0)?.shouldShowTimestamp = true
        for (i in 1 until listingItems.size) {
            listingItems[i].shouldShowTimestamp = isDifferentDay(listingItems[i - 1].listing, listingItems[i].listing)
        }
        return listingItems
    }

    private fun isDifferentDay(listing1: FeaturedGuideListing, listing2: FeaturedGuideListing): Boolean {
        val calendar1 = Calendar.getInstance().apply {
            timeInMillis = TimeUnit.SECONDS.toMillis(listing1.startTimestamp)
        }
        val calendar2 = Calendar.getInstance().apply {
            timeInMillis = TimeUnit.SECONDS.toMillis(listing2.startTimestamp)
        }
        return calendar1.get(Calendar.DAY_OF_YEAR) != calendar2.get(Calendar.DAY_OF_YEAR)
                || calendar1.get(Calendar.YEAR) != calendar2.get(Calendar.YEAR)
    }
}

sealed class FeaturedGuideItem {
    data class ListingItem(
            var listing: FeaturedGuideListing,
            var shouldShowTimestamp: Boolean = false,
            var isExpanded: Boolean = false,
            var detailHeight: Int = 0) : FeaturedGuideItem()
    // TODO data class DateHeaderItem() : FeaturedGuideItem()
}

class GuideAdapter @Inject constructor(
        private val dispatchConfig: DispatchConfig,
        private val followManager: FollowManager,
        @ThemeFollowedExplore private val followedTheme: UserTheme,
        @ThemeNotFollowedExplore private val notFollowedTheme: UserTheme,
        private val picasso: Picasso
): ListAdapter<ListingItem, GuideViewHolder>(
        object : DiffUtil.ItemCallback<ListingItem>() {
            override fun areItemsTheSame(oldItem: ListingItem, newItem: ListingItem) = oldItem === newItem
            override fun areContentsTheSame(oldItem: ListingItem, newItem: ListingItem) = oldItem == newItem
        }
), CoroutineScope {

    private val job = SupervisorJob()
    private val exceptionHandler = CoroutineExceptionHandler { _, throwable ->
        Timber.e(throwable, "Coroutine throwable")
    }
    override val coroutineContext: CoroutineContext
        get() = dispatchConfig.main + job + exceptionHandler

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): GuideViewHolder {
        val binding = FeaturedGuideItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return GuideViewHolder(binding, this, followManager, followedTheme, notFollowedTheme, picasso)
    }

    override fun onBindViewHolder(holder: GuideViewHolder, position: Int) {
        holder.bind(getItem(position)) { clickedPosition, isExpanded ->
            getItem(clickedPosition).isExpanded = isExpanded
        }
    }

    override fun onDetachedFromRecyclerView(recyclerView: RecyclerView) {
        job.cancelChildren()
        super.onDetachedFromRecyclerView(recyclerView)
    }
}

class GuideViewHolder(
        private val binding: FeaturedGuideItemBinding,
        private val scope: CoroutineScope,
        private val followManager: FollowManager,
        private val followedTheme: UserTheme,
        private val notFollowedTheme: UserTheme,
        private val picasso: Picasso
) : RecyclerView.ViewHolder(binding.root) {

    var job: Job? = null

    fun bind(listingItem: ListingItem, callback: (clickedPosition: Int, isExpanded: Boolean) -> Unit) {
        job?.cancel()
        clear()

        binding.included.detailContainer.isVisible = listingItem.isExpanded
        job = scope.launch {
            val user = followManager.userDetails(listingItem.listing.caid) ?: return@launch
            configureUser(user, createFollowHandler(user))
            binding.usernamePlainTextView.text = user.username
            binding.usOnlyLabelTextView.isVisible = listingItem.listing.isUsOnly
            View.OnClickListener {
                Navigation.findNavController(itemView).safeNavigate(MainNavDirections.actionGlobalProfileFragment(listingItem.listing.caid))
            }.let {
                binding.included.avatarImageView.setOnClickListener(it)
                binding.included.usernameTextView.setOnClickListener(it)
            }
        }
        binding.dateTextView.isVisible = listingItem.shouldShowTimestamp
        binding.dateTextView.text = getDateText(listingItem)
        binding.timeTextView.text = getTimeText(listingItem)

        picasso.load(listingItem.listing.eventImageUrl)
                .resizeDimen(R.dimen.featured_guide_event_image_size, R.dimen.featured_guide_event_image_size)
                .centerCrop()
                .into(binding.eventImageView)
        binding.categoryTextView.text = listingItem.listing.category
        binding.titleTextView.text = listingItem.listing.title

        picasso.load(listingItem.listing.detailImageUrl)
                .resizeDimen(R.dimen.featured_guide_detail_image_width, R.dimen.featured_guide_detail_image_height)
                .centerCrop()
                .into(binding.included.detailImageView)
        binding.included.descriptionTextView.text = listingItem.listing.description
        binding.included.usOnlyDescriptionTextView.isVisible = listingItem.listing.isUsOnly

        // Click listeners
        itemView.setOnClickListener { animateDetailView(listingItem, callback) }
        binding.included.detailContainer.setOnClickListener { animateDetailView(listingItem, callback) }
    }

    private fun createFollowHandler(user: User): FollowManager.FollowHandler {
        return FollowManager.FollowHandler(null, object: FollowManager.Callback() {
            override fun follow(caid: CAID) {
                scope.launch {
                    followManager.followUser(caid, object: FollowManager.FollowCompletedCallback {
                        override fun onUserFollowed() {
                            configureUser(user, null)
                        }
                    })
                }
            }

            override fun unfollow(caid: CAID) { // can't unfollow in FPG
            }
        })
    }

    private fun configureUser(user: User, followHandler: FollowManager.FollowHandler?) {
        user.configure(binding.included.avatarImageView, binding.included.usernameTextView,
                binding.included.followButton, followManager, followHandler = followHandler,
                avatarImageSize = R.dimen.chat_avatar_size, followedTheme = followedTheme,
                notFollowedTheme = notFollowedTheme, picasso = picasso)
    }

    private fun animateDetailView(listingItem: ListingItem, callback: (clickedPosition: Int, isExpanded: Boolean) -> Unit) {
        /**
         * The animation listeners are unreliable that we can't trust them to reset the height
         * if the animation has been interrupted. We initialize it here at the data layer since
         * the view must have been measured when it's clicked.
         */
        if (listingItem.detailHeight == 0) {
            listingItem.detailHeight = binding.included.detailContainer.height
        }
        if (listingItem.isExpanded) {
            binding.included.detailContainer.animateSlideUpAndHide(listingItem.detailHeight)
        } else {
            binding.included.detailContainer.isVisible = true
            TransitionManager.beginDelayedTransition(itemView as ViewGroup)
        }
        /**
         * Due to the visual imperfectness of the default animation, we build our own animation.
         * The callback only updates the isExpanded field so it's correct at the data layer
         * in preparation for the next bind() call. The callback doesn't trigger a bind() call.
         */
        callback(adapterPosition, !listingItem.isExpanded)
    }

    private fun clear() {
        binding.dateTextView.text = null
        binding.timeTextView.text = null

        binding.eventImageView.setImageDrawable(null)
        binding.categoryTextView.text = null
        binding.titleTextView.text = null
        binding.usernamePlainTextView.text = null
        binding.usOnlyLabelTextView.isVisible = false

        binding.included.detailContainer.isVisible = false
        binding.included.detailImageView.setImageDrawable(null)
        binding.included.descriptionTextView.text = null
        binding.included.usOnlyDescriptionTextView.isVisible = false
        binding.included.avatarImageView.setImageDrawable(null)
        binding.included.avatarImageView.setOnClickListener(null)
        binding.included.usernameTextView.text = null
        binding.included.followButton.isVisible = false
    }

    /**
     * TODO (AND-139): Localize the date format.
     */
    private fun getDateText(listingItem: ListingItem): String {
        return Calendar.getInstance().run {
            timeInMillis = TimeUnit.SECONDS.toMillis(listingItem.listing.startTimestamp)
            SimpleDateFormat("EEEE, MMMM d", Locale.getDefault()).format(this.time)
        }
    }

    /**
     * TODO (AND-139): Localize the time format.
     */
    private fun getTimeText(listingItem: ListingItem): String {
        return Calendar.getInstance().run {
            timeInMillis = TimeUnit.SECONDS.toMillis(listingItem.listing.startTimestamp)
            SimpleDateFormat("h:mm a", Locale.getDefault()).format(this.time)
        }
    }
}

