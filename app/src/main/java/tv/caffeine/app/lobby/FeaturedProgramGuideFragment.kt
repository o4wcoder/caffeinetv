package tv.caffeine.app.lobby

import android.os.Bundle
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
import tv.caffeine.app.api.FeaturedGuide
import tv.caffeine.app.api.model.CaffeineResult
import tv.caffeine.app.api.model.awaitAndParseErrors
import tv.caffeine.app.databinding.FragmentFeaturedProgramGuideBinding
import tv.caffeine.app.di.ThemeFollowedExplore
import tv.caffeine.app.di.ThemeNotFollowedExplore
import tv.caffeine.app.session.FollowManager
import tv.caffeine.app.ui.CaffeineFragment
import tv.caffeine.app.ui.CaffeineViewModel
import tv.caffeine.app.util.DispatchConfig
import tv.caffeine.app.util.UserTheme
import tv.caffeine.app.util.configure
import tv.caffeine.app.util.safeNavigate
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
        viewModel.guides.observe(viewLifecycleOwner, Observer { guides ->
            binding.emptyMessageTextView.isVisible = guides.isEmpty()
            guideAdapter.submitList(guides)
        })
    }
}

class FeaturedProgramGuideViewModel(
        dispatchConfig: DispatchConfig,
        private val broadcastsService: BroadcastsService,
        private val gson: Gson
) : CaffeineViewModel(dispatchConfig) {

    private val _guides = MutableLiveData<List<FeaturedGuide>>()
    val guides: LiveData<List<FeaturedGuide>> = Transformations.map(_guides) { it }

    init {
        load()
    }

    private fun load() {
        launch {
            val result = broadcastsService.featuredGuide().awaitAndParseErrors(gson)
            when (result) {
                is CaffeineResult.Success -> _guides.value = prepareGuideTimestamp(result.value.listings)
                is CaffeineResult.Error -> Timber.e("Failed to fetch content guide ${result.error}")
                is CaffeineResult.Failure -> Timber.e(result.throwable)
            }
        }
    }

    private fun prepareGuideTimestamp(guides: List<FeaturedGuide>): List<FeaturedGuide> {
        guides.getOrNull(0)?.shouldShowTimestamp = true
        for (i in 1 until guides.size) {
            guides[i].shouldShowTimestamp = isDifferentDay(guides[i - 1], guides[i])
        }
        return guides
    }

    private fun isDifferentDay(guide1: FeaturedGuide, guide2: FeaturedGuide): Boolean {
        val calendar1 = Calendar.getInstance().apply {
            timeInMillis = TimeUnit.SECONDS.toMillis(guide1.startTimestamp)
        }
        val calendar2 = Calendar.getInstance().apply {
            timeInMillis = TimeUnit.SECONDS.toMillis(guide2.startTimestamp)
        }
        return calendar1.get(Calendar.DAY_OF_YEAR) != calendar2.get(Calendar.DAY_OF_YEAR)
                || calendar1.get(Calendar.YEAR) != calendar2.get(Calendar.YEAR)
    }
}

class GuideAdapter @Inject constructor(
        private val dispatchConfig: DispatchConfig,
        private val followManager: FollowManager,
        @ThemeFollowedExplore private val followedTheme: UserTheme,
        @ThemeNotFollowedExplore private val notFollowedTheme: UserTheme,
        private val picasso: Picasso
): ListAdapter<FeaturedGuide, GuideViewHolder>(
        object : DiffUtil.ItemCallback<FeaturedGuide>() {
            override fun areItemsTheSame(oldItem: FeaturedGuide, newItem: FeaturedGuide) = oldItem === newItem
            override fun areContentsTheSame(oldItem: FeaturedGuide, newItem: FeaturedGuide) = oldItem.id == newItem.id
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
        holder.bind(getItem(position), position)
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

    fun bind(guide: FeaturedGuide, position: Int) {
        job?.cancel()
        clear()

        detailContainer.isVisible = position == 0
        job = scope.launch {
            val user = followManager.userDetails(guide.caid) ?: return@launch
            user.configure(avatarImageView, usernameTextView, null, followManager, avatarImageSize = R.dimen.chat_avatar_size, followedTheme = followedTheme, notFollowedTheme = notFollowedTheme, picasso = picasso)
            usernamePlainTextView.text = user.username
            avatarImageView.setOnClickListener {
                Navigation.findNavController(itemView).safeNavigate(MainNavDirections.actionGlobalProfileFragment(guide.caid))
            }
        }
        dateTextView.isVisible = guide.shouldShowTimestamp
        dateTextView.text = getDateText(guide)
        timeTextView.text = getTimeText(guide)

        picasso.load(guide.eventImageUrl)
                .resizeDimen(R.dimen.featured_guide_event_image_size, R.dimen.featured_guide_event_image_size)
                .centerCrop()
                .into(eventImageView)
        categoryTextView.text = guide.category
        titleTextView.text = guide.title

        picasso.load(guide.detailImageUrl)
                .resizeDimen(R.dimen.featured_guide_detail_image_width, R.dimen.featured_guide_detail_image_height)
                .centerCrop()
                .into(detailImageView)
        descriptionTextView.text = guide.description
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
    private fun getDateText(guide: FeaturedGuide): String {
        return Calendar.getInstance().run {
            timeInMillis = TimeUnit.SECONDS.toMillis(guide.startTimestamp)
            SimpleDateFormat("EEEE, MMMM d", Locale.getDefault()).format(this.time)
        }
    }

    /**
     * TODO (AND-139): Localize the time format.
     */
    private fun getTimeText(guide: FeaturedGuide): String {
        return Calendar.getInstance().run {
            timeInMillis = TimeUnit.SECONDS.toMillis(guide.startTimestamp)
            SimpleDateFormat("h:mm a", Locale.getDefault()).format(this.time)
        }
    }
}

