package tv.caffeine.app.lobby

import android.content.Context
import android.transition.TransitionManager
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.databinding.BindingAdapter
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.Observer
import androidx.navigation.Navigation
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.squareup.inject.assisted.Assisted
import com.squareup.inject.assisted.AssistedInject
import com.squareup.picasso.Picasso
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.launch
import org.threeten.bp.Instant
import org.threeten.bp.LocalDate
import org.threeten.bp.ZoneId
import org.threeten.bp.ZonedDateTime
import timber.log.Timber
import tv.caffeine.app.MainNavDirections
import tv.caffeine.app.R
import tv.caffeine.app.api.FeaturedGuideListing
import tv.caffeine.app.api.model.CAID
import tv.caffeine.app.api.model.User
import tv.caffeine.app.databinding.FeaturedGuideDateHeaderBinding
import tv.caffeine.app.databinding.FeaturedGuideListingItemBinding
import tv.caffeine.app.databinding.FeaturedGuideReleaseDateHeaderBinding
import tv.caffeine.app.databinding.FeaturedGuideReleaseListingItemBinding
import tv.caffeine.app.lobby.FeaturedGuideItem.DateHeader
import tv.caffeine.app.lobby.FeaturedGuideItem.ListingItem
import tv.caffeine.app.lobby.release.FPGBroadcaster
import tv.caffeine.app.lobby.release.NavigationCommand
import tv.caffeine.app.lobby.release.observeEvents
import tv.caffeine.app.session.FollowManager
import tv.caffeine.app.settings.ReleaseDesignConfig
import tv.caffeine.app.util.DispatchConfig
import tv.caffeine.app.util.UsernameTheming
import tv.caffeine.app.util.animateSlideUpAndHide
import tv.caffeine.app.util.configure
import tv.caffeine.app.util.safeNavigate
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.concurrent.TimeUnit
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
        var isExpanded: Boolean = false
    ) : FeaturedGuideItem() {
        override fun getItemType() = FeaturedGuideItem.Type.LISTING_ITEM
    }

    data class DateHeader(val startTimestamp: Long) : FeaturedGuideItem() {
        override fun getItemType() = FeaturedGuideItem.Type.DATE_HEADER_ITEM
    }
}

class FeaturedProgramGuideAdapter @AssistedInject constructor(
    private val dispatchConfig: DispatchConfig,
    private val followManager: FollowManager,
    private val picasso: Picasso,
    private val releaseDesignConfig: ReleaseDesignConfig,
    @Assisted private val lifecycleOwner: LifecycleOwner
) : ListAdapter<FeaturedGuideItem, GuideViewHolder>(
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

    @AssistedInject.Factory
    interface Factory {
        fun create(lifecycleOwner: LifecycleOwner): FeaturedProgramGuideAdapter
    }

    private val job = SupervisorJob()
    private val exceptionHandler = CoroutineExceptionHandler { _, throwable ->
        Timber.e(throwable, "Coroutine throwable")
    }
    override val coroutineContext: CoroutineContext
        get() = dispatchConfig.main + job + exceptionHandler

    override fun getItemViewType(position: Int) = getItem(position).getViewType()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): GuideViewHolder {
        return when (FeaturedGuideItem.Type.ofViewType(viewType)) {
            FeaturedGuideItem.Type.LISTING_ITEM -> {
                if (releaseDesignConfig.isReleaseDesignActive()) {
                    val binding = FeaturedGuideReleaseListingItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
                    ReleaseListingItemViewHolder(binding, this, followManager, lifecycleOwner)
                } else {
                    val binding = FeaturedGuideListingItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
                    ListingItemViewHolder(binding, this, followManager, picasso)
                }
            }
            FeaturedGuideItem.Type.DATE_HEADER_ITEM -> {
                if (releaseDesignConfig.isReleaseDesignActive()) {
                    val binding = FeaturedGuideReleaseDateHeaderBinding.inflate(LayoutInflater.from(parent.context), parent, false)
                    ReleaseDateHeaderViewHolder(binding)
                } else {
                    val binding = FeaturedGuideDateHeaderBinding.inflate(LayoutInflater.from(parent.context), parent, false)
                    DateHeaderViewHolder(binding)
                }
            }
        }
    }

    override fun onBindViewHolder(holder: GuideViewHolder, position: Int) {
        return when (getItem(position).getItemType()) {
            FeaturedGuideItem.Type.LISTING_ITEM -> {
                if (releaseDesignConfig.isReleaseDesignActive()) {
                    (holder as ReleaseListingItemViewHolder).bind(getItem(position) as ListingItem)
                } else {
                    (holder as ListingItemViewHolder).bind(getItem(position) as ListingItem)
                }
            }
            FeaturedGuideItem.Type.DATE_HEADER_ITEM -> {
                if (releaseDesignConfig.isReleaseDesignActive()) {
                    (holder as ReleaseDateHeaderViewHolder).bind(getItem(position) as DateHeader)
                } else {
                    (holder as DateHeaderViewHolder).bind(getItem(position) as DateHeader)
                }
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
        binding.dateTextView.text = dateHeader.getDateText(itemView.context)
    }
}

class ReleaseDateHeaderViewHolder(private val binding: FeaturedGuideReleaseDateHeaderBinding) :
    GuideViewHolder(binding.root) {
    fun bind(dateHeader: DateHeader) {
        binding.dateTextView.text = dateHeader.getDateText(itemView.context)
    }
}

/**
 * TODO (AND-139): Localize the date format.
 */
fun DateHeader.getDateText(context: Context): String {
    return Calendar.getInstance().run {
        val seconds = startTimestamp
        timeInMillis = TimeUnit.SECONDS.toMillis(seconds)
        val instant = Instant.ofEpochSecond(seconds)
        val dateTime = ZonedDateTime.ofInstant(instant, ZoneId.systemDefault())
        val localDate = dateTime.toLocalDate()
        val today = LocalDate.now()
        val tomorrow = today.plusDays(1)
        when (localDate) {
            today -> context.getString(R.string.today)
            tomorrow -> context.getString(R.string.tomorrow)
            else -> SimpleDateFormat("EEEE, MMMM d", Locale.getDefault()).format(this.time)
        }
    }
}

class ListingItemViewHolder(
    private val binding: FeaturedGuideListingItemBinding,
    private val scope: CoroutineScope,
    private val followManager: FollowManager,
    private val picasso: Picasso
) : GuideViewHolder(binding.root) {

    var job: Job? = null

    fun bind(listingItem: ListingItem) {
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

        val hasDetailImage = listingItem.listing.detailImageUrl != null
        binding.included.detailImageView.isVisible = hasDetailImage
        binding.included.divider.isVisible = !hasDetailImage
        listingItem.listing.detailImageUrl?.let {
            picasso.load(it)
                    .into(binding.included.detailImageView)
        }
        binding.included.descriptionTextView.text = listingItem.listing.description

        // Click listeners
        itemView.setOnClickListener { animateDetailView(listingItem) }
        binding.included.detailContainer.setOnClickListener { animateDetailView(listingItem) }
    }

    private fun createFollowHandler(user: User): FollowManager.FollowHandler {
        return FollowManager.FollowHandler(null, object : FollowManager.Callback() {
            override fun follow(caid: CAID) {
                scope.launch {
                    followManager.followUser(caid, object : FollowManager.FollowCompletedCallback {
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
                theme = UsernameTheming.STANDARD)
    }

    private fun animateDetailView(listingItem: ListingItem) {
        if (listingItem.isExpanded) {
            binding.included.detailContainer.animateSlideUpAndHide()
        } else {
            binding.included.detailContainer.isVisible = true
            TransitionManager.beginDelayedTransition(itemView as ViewGroup)
        }
        listingItem.isExpanded = !listingItem.isExpanded
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
        binding.included.divider.isVisible = false
        binding.included.detailImageView.setImageDrawable(null)
        binding.included.descriptionTextView.text = null
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

class ReleaseListingItemViewHolder(
    private val binding: FeaturedGuideReleaseListingItemBinding,
    private val scope: CoroutineScope,
    private val followManager: FollowManager,
    private val lifecycleOwner: LifecycleOwner
) : GuideViewHolder(binding.root) {
    var job: Job? = null

    fun bind(listingItem: ListingItem) {
        job?.cancel()
        clear()

        binding.listing = listingItem.listing
        job = scope.launch {
            val user = followManager.userDetails(listingItem.listing.caid) ?: return@launch
            val fpgBroadcaster = FPGBroadcaster(followManager, user, scope)
            binding.user = fpgBroadcaster

            fpgBroadcaster.navigationCommands.observeEvents(lifecycleOwner) {
                when (it) {
                    is NavigationCommand.To -> Navigation.findNavController(itemView).safeNavigate(it.directions)
                }
            }
            fpgBroadcaster.isFollowing.observe(lifecycleOwner, Observer {
                binding.followButtonLayout.followButton.apply {
                    val drawableId = if (it == true) R.drawable.star_filled else R.drawable.star_outline
                    setImageDrawable(ContextCompat.getDrawable(context, drawableId))
                }
            })
        }
    }

    private fun clear() {
        binding.detailImageView.setImageDrawable(null)
        binding.timeTextView.text = null
        binding.usOnlyLabelTextView.isVisible = false
        binding.titleTextView.text = null
        binding.descriptionTextView.text = null
        binding.avatarImageView.setImageDrawable(null)
        binding.usernameTextView.text = null
    }
}

@BindingAdapter("fpgTime")
fun TextView.setFPGTime(startTimestamp: Long) {
    val calendar = Calendar.getInstance()
    calendar.timeInMillis = TimeUnit.SECONDS.toMillis(startTimestamp)
    text = SimpleDateFormat("h:mm a", Locale.getDefault()).format(calendar.time)
}
