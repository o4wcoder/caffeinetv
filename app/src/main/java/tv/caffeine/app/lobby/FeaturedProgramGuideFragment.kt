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
import jp.wasabeef.picasso.transformations.RoundedCornersTransformation
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
import tv.caffeine.app.databinding.FeaturedGuideDateHeaderItemBinding
import tv.caffeine.app.databinding.FeaturedGuideItemBinding
import tv.caffeine.app.databinding.FragmentFeaturedProgramGuideBinding
import tv.caffeine.app.di.ThemeFollowedExplore
import tv.caffeine.app.di.ThemeNotFollowedExplore
import tv.caffeine.app.lobby.FeaturedGuideItem.DateHeaderItem
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
        binding.guideSwipeRefreshLayout.setOnRefreshListener { viewModel.load() }
        viewModel.listings.observe(viewLifecycleOwner, Observer {
            binding.guideSwipeRefreshLayout.isRefreshing = false
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

    private val _listings = MutableLiveData<List<FeaturedGuideItem>>()
    val listings: LiveData<List<FeaturedGuideItem>> = Transformations.map(_listings) { it }

    init {
        load()
    }

    fun load() {
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

    private fun prepareList(listings: List<FeaturedGuideListing>): List<FeaturedGuideItem> {
        val listingItems = listings.map { ListingItem(it) }.apply {
            getOrNull(0)?.isExpanded = true
        }
        return prepareListingTimestamp(listingItems)
    }

    private fun prepareListingTimestamp(listingItems: List<ListingItem>): List<FeaturedGuideItem> {
        val timestampIndices = mutableListOf<Int>()
        listingItems.getOrNull(0)?.let { timestampIndices.add(0)}
        for (i in 1 until listingItems.size) {
            if (isDifferentDay(listingItems[i - 1].listing, listingItems[i].listing)) {
                timestampIndices.add(i)
            }
        }
        val featuredGuideItems = LinkedList<FeaturedGuideItem>(listingItems)
        for (i in timestampIndices.reversed()) {
            featuredGuideItems.add(i, FeaturedGuideItem.DateHeaderItem(listingItems[i].listing.startTimestamp))
        }
        return featuredGuideItems
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

    enum class Type {
        LISTING_ITEM, DATE_HEADER_ITEM;

        companion object {
            fun ofViewType(viewType: Int) = Type.values()[viewType]
        }
    }

    abstract fun getItemType(): FeaturedGuideItem.Type
    fun getViewType() = getItemType().ordinal

    data class ListingItem(
            var listing: FeaturedGuideListing,
            var isExpanded: Boolean = false,
            var detailHeight: Int = 0) : FeaturedGuideItem() {
        override fun getItemType() = FeaturedGuideItem.Type.LISTING_ITEM
    }

    data class DateHeaderItem(val startTimestamp: Long) : FeaturedGuideItem() {
        override fun getItemType() = FeaturedGuideItem.Type.DATE_HEADER_ITEM
    }
}

class GuideAdapter @Inject constructor(
        private val dispatchConfig: DispatchConfig,
        private val followManager: FollowManager,
        @ThemeFollowedExplore private val followedTheme: UserTheme,
        @ThemeNotFollowedExplore private val notFollowedTheme: UserTheme,
        private val picasso: Picasso
): ListAdapter<FeaturedGuideItem, GuideViewHolder>(
        object : DiffUtil.ItemCallback<FeaturedGuideItem>() {
            override fun areItemsTheSame(oldItem: FeaturedGuideItem, newItem: FeaturedGuideItem) = oldItem === newItem
            override fun areContentsTheSame(oldItem: FeaturedGuideItem, newItem: FeaturedGuideItem): Boolean {
                return when {
                    oldItem is ListingItem && newItem is ListingItem -> oldItem == newItem
                    oldItem is DateHeaderItem && newItem is DateHeaderItem -> oldItem == newItem
                    else -> false
                }
            }
        }
), CoroutineScope {

    private val job = SupervisorJob()
    private val exceptionHandler = CoroutineExceptionHandler { _, throwable ->
        Timber.e(throwable, "Coroutine throwable")
    }
    override val coroutineContext: CoroutineContext
        get() = dispatchConfig.main + job + exceptionHandler

    override fun getItemViewType(position: Int) = getItem(position).getViewType()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): GuideViewHolder {
        return when(FeaturedGuideItem.Type.ofViewType(viewType)) {
            FeaturedGuideItem.Type.LISTING_ITEM -> {
                val binding = FeaturedGuideItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
                ListingItemViewHolder(binding, this, followManager, followedTheme, notFollowedTheme, picasso)
            }
            FeaturedGuideItem.Type.DATE_HEADER_ITEM -> {
                val binding = FeaturedGuideDateHeaderItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
                DateHeaderViewHolder(binding)
            }
        }
    }

    override fun onBindViewHolder(holder: GuideViewHolder, position: Int) {
        return when(getItem(position).getItemType()) {
            FeaturedGuideItem.Type.LISTING_ITEM -> {
                (holder as ListingItemViewHolder).bind(getItem(position) as ListingItem) { clickedPosition, isExpanded ->
                    getItem(clickedPosition).let {
                        if (it is ListingItem) {
                            it.isExpanded = isExpanded
                        }
                    }
                }
            }
            FeaturedGuideItem.Type.DATE_HEADER_ITEM -> {
                (holder as DateHeaderViewHolder).bind(getItem(position) as DateHeaderItem)
            }
        }
    }

    override fun onDetachedFromRecyclerView(recyclerView: RecyclerView) {
        job.cancelChildren()
        super.onDetachedFromRecyclerView(recyclerView)
    }
}

sealed class GuideViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView)

class DateHeaderViewHolder(private val binding: FeaturedGuideDateHeaderItemBinding) : GuideViewHolder(binding.root) {

    fun bind(dateHeaderItem: DateHeaderItem) {
        binding.dateTextView.text = getDateText(dateHeaderItem)
    }

    /**
     * TODO (AND-139): Localize the date format.
     */
    private fun getDateText(dateHeaderItem: DateHeaderItem): String {
        return Calendar.getInstance().run {
            timeInMillis = TimeUnit.SECONDS.toMillis(dateHeaderItem.startTimestamp)
            SimpleDateFormat("EEEE, MMMM d", Locale.getDefault()).format(this.time)
        }
    }
}

class ListingItemViewHolder(
        private val binding: FeaturedGuideItemBinding,
        private val scope: CoroutineScope,
        private val followManager: FollowManager,
        private val followedTheme: UserTheme,
        private val notFollowedTheme: UserTheme,
        private val picasso: Picasso
) : GuideViewHolder(binding.root) {

    var job: Job? = null

    private val roundedCornersTransformation by lazy { RoundedCornersTransformation(itemView.resources.getDimensionPixelSize(R.dimen.lobby_card_pip_radius), 0) }

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
        binding.timeTextView.text = getTimeText(listingItem)

        picasso.load(listingItem.listing.eventImageUrl)
                .resizeDimen(R.dimen.featured_guide_event_image_size, R.dimen.featured_guide_event_image_size)
                .centerCrop()
                .transform(roundedCornersTransformation)
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
     * TODO (AND-139): Localize the time format.
     */
    private fun getTimeText(listingItem: ListingItem): String {
        return Calendar.getInstance().run {
            timeInMillis = TimeUnit.SECONDS.toMillis(listingItem.listing.startTimestamp)
            SimpleDateFormat("h:mm a", Locale.getDefault()).format(this.time)
        }
    }
}

