package tv.caffeine.app.lobby

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.navigation.Navigation
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import androidx.transition.TransitionManager
import com.squareup.picasso.Picasso
import kotlinx.coroutines.*
import timber.log.Timber
import tv.caffeine.app.MainNavDirections
import tv.caffeine.app.R
import tv.caffeine.app.api.FeaturedGuideListing
import tv.caffeine.app.api.model.CAID
import tv.caffeine.app.api.model.User
import tv.caffeine.app.databinding.FeaturedGuideDateHeaderBinding
import tv.caffeine.app.databinding.FeaturedGuideListingItemBinding
import tv.caffeine.app.di.ThemeFollowedExplore
import tv.caffeine.app.di.ThemeNotFollowedExplore
import tv.caffeine.app.lobby.FeaturedGuideItem.DateHeader
import tv.caffeine.app.lobby.FeaturedGuideItem.ListingItem
import tv.caffeine.app.session.FollowManager
import tv.caffeine.app.util.*
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import kotlin.coroutines.CoroutineContext


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

    data class DateHeader(val startTimestamp: Long) : FeaturedGuideItem() {
        override fun getItemType() = FeaturedGuideItem.Type.DATE_HEADER_ITEM
    }
}

class FeaturedProgramGuideAdapter @Inject constructor(
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
                    oldItem is DateHeader && newItem is DateHeader -> oldItem == newItem
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
                val binding = FeaturedGuideListingItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
                ListingItemViewHolder(binding, this, followManager, followedTheme, notFollowedTheme, picasso)
            }
            FeaturedGuideItem.Type.DATE_HEADER_ITEM -> {
                val binding = FeaturedGuideDateHeaderBinding.inflate(LayoutInflater.from(parent.context), parent, false)
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
                (holder as DateHeaderViewHolder).bind(getItem(position) as DateHeader)
            }
        }
    }

    override fun onDetachedFromRecyclerView(recyclerView: RecyclerView) {
        job.cancelChildren()
        super.onDetachedFromRecyclerView(recyclerView)
    }
}

sealed class GuideViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView)

class DateHeaderViewHolder(private val binding: FeaturedGuideDateHeaderBinding) : GuideViewHolder(binding.root) {

    fun bind(dateHeader: DateHeader) {
        binding.dateTextView.text = getDateText(dateHeader)
    }

    /**
     * TODO (AND-139): Localize the date format.
     */
    private fun getDateText(dateHeader: DateHeader): String {
        return Calendar.getInstance().run {
            timeInMillis = TimeUnit.SECONDS.toMillis(dateHeader.startTimestamp)
            SimpleDateFormat("EEEE, MMMM d", Locale.getDefault()).format(this.time)
        }
    }
}

class ListingItemViewHolder(
        private val binding: FeaturedGuideListingItemBinding,
        private val scope: CoroutineScope,
        private val followManager: FollowManager,
        private val followedTheme: UserTheme,
        private val notFollowedTheme: UserTheme,
        private val picasso: Picasso
) : GuideViewHolder(binding.root) {

    var job: Job? = null

    fun bind(listingItem: ListingItem, callback: (clickedPosition: Int, isExpanded: Boolean) -> Unit) {
        job?.cancel()
        clear()

        binding.included.detailContainer.isVisible = listingItem.isExpanded
        job = scope.launch {
            val user = followManager.userDetails(listingItem.listing.caid) ?: return@launch
            configureUser(user, createFollowHandler(user))
            binding.usOnlyLabelTextView.isVisible = listingItem.listing.isUsOnly
            View.OnClickListener {
                Navigation.findNavController(itemView).safeNavigate(MainNavDirections.actionGlobalProfileFragment(listingItem.listing.caid))
            }.let {
                binding.avatarImageView.setOnClickListener(it)
                binding.usernameTextView.setOnClickListener(it)
            }
        }
        binding.timeTextView.text = getTimeText(listingItem)
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
        user.configure(binding.avatarImageView, binding.usernameTextView, binding.followButton, followManager,
                followHandler = followHandler, avatarImageSize = R.dimen.avatar_featured_guide,
                followedTheme = followedTheme, notFollowedTheme = notFollowedTheme, picasso = picasso)
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
        binding.categoryTextView.text = null
        binding.titleTextView.text = null
        binding.usOnlyLabelTextView.isVisible = false
        binding.avatarImageView.setImageDrawable(null)
        binding.avatarImageView.setOnClickListener(null)
        binding.usernameTextView.text = null
        binding.followButton.isVisible = false

        binding.included.detailContainer.isVisible = false
        binding.included.detailImageView.setImageDrawable(null)
        binding.included.descriptionTextView.text = null
        binding.included.usOnlyDescriptionTextView.isVisible = false
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

