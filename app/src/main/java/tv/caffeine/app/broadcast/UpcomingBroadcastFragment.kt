package tv.caffeine.app.broadcast

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.fragment.app.viewModels
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import androidx.lifecycle.Transformations
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.google.gson.Gson
import com.squareup.picasso.Picasso
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import timber.log.Timber
import tv.caffeine.app.R
import tv.caffeine.app.api.BroadcastsService
import tv.caffeine.app.api.Guide
import tv.caffeine.app.api.model.CaffeineResult
import tv.caffeine.app.api.model.awaitAndParseErrors
import tv.caffeine.app.databinding.FragmentUpcomingBroadcastBinding
import tv.caffeine.app.di.ThemeFollowedExplore
import tv.caffeine.app.di.ThemeNotFollowedExploreDark
import tv.caffeine.app.session.FollowManager
import tv.caffeine.app.ui.CaffeineBottomSheetDialogFragment
import tv.caffeine.app.ui.CaffeineViewModel
import tv.caffeine.app.util.DispatchConfig
import tv.caffeine.app.util.UserTheme
import tv.caffeine.app.util.configure
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import kotlin.coroutines.CoroutineContext

class UpcomingBroadcastFragment : CaffeineBottomSheetDialogFragment() {

    @Inject lateinit var guideAdapter: GuideAdapter
    @Inject lateinit var gson: Gson
    private var binding: FragmentUpcomingBroadcastBinding? = null
    private val viewModel: GuideViewModel by viewModels { viewModelFactory }

    override fun getTheme() = R.style.DarkBottomSheetDialog

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return FragmentUpcomingBroadcastBinding.inflate(inflater, container, false).run {
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

    private fun configure(binding: FragmentUpcomingBroadcastBinding) {
        binding.actionBar.apply {
            applyDarkMode()
            setTitle(R.string.upcoming_broadcasts_dialog_title)
            setDismissListener { dismiss() }
        }
        binding.lifecycleOwner = viewLifecycleOwner
        binding.guideRecyclerView.apply {
            adapter = guideAdapter
            ContextCompat.getDrawable(context, R.drawable.black_top_divider)?.let { drawable ->
                addItemDecoration(DividerItemDecoration(context, DividerItemDecoration.VERTICAL).apply {
                    setDrawable(drawable)
                })
            }
        }
        viewModel.guides.observe(viewLifecycleOwner, Observer { guides ->
            binding.emptyMessageTextView.isVisible = guides.isEmpty()
            guideAdapter.submitList(guides)
        })
    }
}

class GuideViewModel @Inject constructor(
        dispatchConfig: DispatchConfig,
        private val broadcastsService: BroadcastsService,
        private val gson: Gson
) : CaffeineViewModel(dispatchConfig) {

    private val _guides = MutableLiveData<List<Guide>>()
    val guides: LiveData<List<Guide>> = Transformations.map(_guides) { it }

    init {
        load()
    }

    private fun load() {
        launch {
            val result = broadcastsService.guide().awaitAndParseErrors(gson)
            when (result) {
                is CaffeineResult.Success -> _guides.value = prepareGuideTimestamp(result.value.listings)
                is CaffeineResult.Error -> Timber.e("Failed to fetch content guide ${result.error}")
                is CaffeineResult.Failure -> Timber.e(result.throwable)
            }
        }
    }

    private fun prepareGuideTimestamp(guides: List<Guide>): List<Guide> {
        guides.getOrNull(0)?.shouldShowTimestamp = true
        for (i in 1 until guides.size) {
            guides[i].shouldShowTimestamp = isDifferentDay(guides[i - 1], guides[i])
        }
        return guides
    }

    private fun isDifferentDay(guide1: Guide, guide2: Guide): Boolean {
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
        @ThemeNotFollowedExploreDark private val notFollowedTheme: UserTheme,
        private val picasso: Picasso
): ListAdapter<Guide, GuideViewHolder>(
        object : DiffUtil.ItemCallback<Guide>() {
            override fun areItemsTheSame(oldItem: Guide, newItem: Guide) = oldItem === newItem
            override fun areContentsTheSame(oldItem: Guide, newItem: Guide) = oldItem.id == newItem.id
        }
), CoroutineScope {

    private val job = SupervisorJob()
    private val exceptionHandler = CoroutineExceptionHandler { _, throwable ->
        Timber.e(throwable, "Coroutine throwable")
    }
    override val coroutineContext: CoroutineContext
        get() = dispatchConfig.main + job + exceptionHandler

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): GuideViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.content_guide_item, parent, false)
        return GuideViewHolder(view, this, followManager, followedTheme, notFollowedTheme, picasso)
    }

    override fun onBindViewHolder(holder: GuideViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    override fun onDetachedFromRecyclerView(recyclerView: RecyclerView) {
        job.cancel()
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

    private val avatarImageView: ImageView = itemView.findViewById(R.id.avatar_image_view)
    private val dateTextView: TextView = itemView.findViewById(R.id.date_text_view)
    private val timeTextView: TextView = itemView.findViewById(R.id.time_text_view)
    private val titleTextView: TextView = itemView.findViewById(R.id.title_text_view)
    private val usernameTextView: TextView = itemView.findViewById(R.id.username_text_view)

    var job: Job? = null

    fun bind(guide: Guide) {
        job?.cancel()
        clear()
        job = scope.launch {
            val user = followManager.userDetails(guide.caid) ?: return@launch
            user.configure(avatarImageView, usernameTextView, null, followManager, followedTheme = followedTheme, notFollowedTheme = notFollowedTheme)
        }
        dateTextView.isVisible = guide.shouldShowTimestamp
        dateTextView.text = getDateText(guide)
        timeTextView.text = getTimeText(guide)
        titleTextView.text = guide.title
    }

    private fun clear() {
        avatarImageView.setImageResource(R.drawable.default_avatar_round)
        dateTextView.text = null
        timeTextView.text = null
        titleTextView.text = null
        usernameTextView.text = null
    }

    /**
     * TODO (i18n): Localize the date format.
     */
    private fun getDateText(guide: Guide): String {
        return Calendar.getInstance().run {
            timeInMillis = TimeUnit.SECONDS.toMillis(guide.startTimestamp)
            SimpleDateFormat("EEEE, MMMM d", Locale.getDefault()).format(this.time)
        }
    }

    /**
     * TODO (i18n): Localize the time format.
     */
    private fun getTimeText(guide: Guide): String {
        val startTime = Calendar.getInstance().run {
            timeInMillis = TimeUnit.SECONDS.toMillis(guide.startTimestamp)
            SimpleDateFormat("h:mm a", Locale.getDefault()).format(this.time)
        }
        val endTime = Calendar.getInstance().run {
            timeInMillis = TimeUnit.SECONDS.toMillis(guide.endTimestamp)
            SimpleDateFormat("h:mm a", Locale.getDefault()).format(this.time)
        }
        return "$startTime - $endTime"
    }
}

