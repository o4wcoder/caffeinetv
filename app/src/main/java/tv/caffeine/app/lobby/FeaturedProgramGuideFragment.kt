package tv.caffeine.app.lobby

import android.os.Bundle
import android.transition.TransitionManager
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
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
import tv.caffeine.app.api.model.CaffeineResult
import tv.caffeine.app.api.model.awaitAndParseErrors
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
                is CaffeineResult.Success -> _listings.value = prepareList(result.value.listings)
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
        val view = LayoutInflater.from(parent.context).inflate(R.layout.featured_guide_item, parent, false)
        return GuideViewHolder(view, this, followManager, followedTheme, notFollowedTheme, picasso)
    }

    override fun onBindViewHolder(holder: GuideViewHolder, position: Int) {
        holder.bind(getItem(position), position) { clickedPosition, isExpanded ->
            getItem(clickedPosition).isExpanded = isExpanded
        }
    }

    override fun onDetachedFromRecyclerView(recyclerView: RecyclerView) {
        job.cancelChildren()
        super.onDetachedFromRecyclerView(recyclerView)
    }
}

class GuideViewHolder(
        itemView: View,
        private val scope: CoroutineScope,
        private val followManager: FollowManager,
        private val followedTheme: UserTheme,
        private val notFollowedTheme: UserTheme,
        private val picasso: Picasso
) : RecyclerView.ViewHolder(itemView) {

    private val dateTextView: TextView = itemView.findViewById(R.id.date_text_view)
    private val eventImageView: ImageView = itemView.findViewById(R.id.event_image_view)
    private val categoryTextView: TextView = itemView.findViewById(R.id.category_text_view)
    private val timeTextView: TextView = itemView.findViewById(R.id.time_text_view)
    private val titleTextView: TextView = itemView.findViewById(R.id.title_text_view)
    private val usernamePlainTextView: TextView = itemView.findViewById(R.id.username_plain_text_view)
    private val detailImageView: ImageView = itemView.findViewById(R.id.detail_image_view)
    private val descriptionTextView: TextView = itemView.findViewById(R.id.description_text_view)
    private val avatarImageView: ImageView = itemView.findViewById(R.id.avatar_image_view)
    private val usernameTextView: TextView = itemView.findViewById(R.id.username_text_view)
    private val detailContainer: ViewGroup = itemView.findViewById(R.id.featured_guide_detail_container)

    var job: Job? = null

    fun bind(listingItem: ListingItem, position: Int, callback: (clickedPosition: Int, isExpanded: Boolean) -> Unit) {
        job?.cancel()
        clear()

        detailContainer.isVisible = listingItem.isExpanded
        job = scope.launch {
            val user = followManager.userDetails(listingItem.listing.caid) ?: return@launch
            user.configure(avatarImageView, usernameTextView, null, followManager, avatarImageSize = R.dimen.chat_avatar_size, followedTheme = followedTheme, notFollowedTheme = notFollowedTheme, picasso = picasso)
            usernamePlainTextView.text = user.username
            avatarImageView.setOnClickListener {
                Navigation.findNavController(itemView).safeNavigate(MainNavDirections.actionGlobalProfileFragment(listingItem.listing.caid))
            }
        }
        dateTextView.isVisible = listingItem.shouldShowTimestamp
        dateTextView.text = getDateText(listingItem)
        timeTextView.text = getTimeText(listingItem)

        picasso.load(listingItem.listing.eventImageUrl)
                .resizeDimen(R.dimen.featured_guide_event_image_size, R.dimen.featured_guide_event_image_size)
                .centerCrop()
                .into(eventImageView)
        categoryTextView.text = listingItem.listing.category
        titleTextView.text = listingItem.listing.title

        picasso.load(listingItem.listing.detailImageUrl)
                .resizeDimen(R.dimen.featured_guide_detail_image_width, R.dimen.featured_guide_detail_image_height)
                .centerCrop()
                .into(detailImageView)
        descriptionTextView.text = listingItem.listing.description

        // Click listeners
        itemView.setOnClickListener { animateDetailView(listingItem, callback) }
        detailContainer.setOnClickListener { animateDetailView(listingItem, callback) }
    }

    private fun animateDetailView(listingItem: ListingItem, callback: (clickedPosition: Int, isExpanded: Boolean) -> Unit) {
        /**
         * The animation listeners are unreliable that we can't trust them to reset the height
         * if the animation has been interrupted. We initialize it here at the data layer since
         * the view must have been measured when it's clicked.
         */
        if (listingItem.detailHeight == 0) {
            listingItem.detailHeight = detailContainer.height
        }
        if (listingItem.isExpanded) {
            detailContainer.animateSlideUpAndHide(listingItem.detailHeight)
        } else {
            detailContainer.isVisible = true
            TransitionManager.beginDelayedTransition(itemView as ViewGroup)
        }
        /**
         * Due to the visual imperfectness of the default animation, we build our own animation.
         * The callback only updates the isExpanded field so it's correct at the data layer
         * in preparation for the next bind() call. The callback doesn't trigger a bind() call.
         */
        callback(position, !listingItem.isExpanded)
    }

    private fun clear() {
        dateTextView.text = null
        timeTextView.text = null

        eventImageView.setImageResource(R.color.light_gray)
        categoryTextView.text = null
        titleTextView.text = null
        usernamePlainTextView.text = null

        detailContainer.isVisible = false
        detailImageView.setImageResource(R.color.light_gray)
        descriptionTextView.text = null
        avatarImageView.setImageResource(R.drawable.default_avatar_round)
        avatarImageView.setOnClickListener(null)
        usernameTextView.text = null
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

