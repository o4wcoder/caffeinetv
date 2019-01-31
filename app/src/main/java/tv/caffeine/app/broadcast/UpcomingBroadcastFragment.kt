package tv.caffeine.app.broadcast

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.google.gson.Gson
import kotlinx.coroutines.*
import timber.log.Timber
import tv.caffeine.app.R
import tv.caffeine.app.api.BroadcastsService
import tv.caffeine.app.api.Guide
import tv.caffeine.app.api.model.CaffeineResult
import tv.caffeine.app.api.model.awaitAndParseErrors
import tv.caffeine.app.databinding.FragmentUpcomingBroadcastBinding
import tv.caffeine.app.session.FollowManager
import tv.caffeine.app.ui.CaffeineBottomSheetDialogFragment
import tv.caffeine.app.util.DispatchConfig
import javax.inject.Inject
import kotlin.coroutines.CoroutineContext

class UpcomingBroadcastFragment : CaffeineBottomSheetDialogFragment() {

    @Inject lateinit var guideAdapter: GuideAdapter
    @Inject lateinit var broadcastService: BroadcastsService
    @Inject lateinit var gson: Gson
    private var binding: FragmentUpcomingBroadcastBinding? = null

    override fun getTheme() = R.style.DarkBottomSheetDialog

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return FragmentUpcomingBroadcastBinding.inflate(inflater, container, false).run {
            configure(this)
            binding = this
            root
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        launch {
            val result = broadcastService.guide().awaitAndParseErrors(gson)
            when (result) {
                is CaffeineResult.Success ->  {
                    result.value.listings.isEmpty().let { isEmpty ->
                        binding?.emptyMessageTextView?.isVisible = isEmpty
                        if (!isEmpty) {
                            guideAdapter.submitList(result.value.listings)
                        }
                    }
                }
                is CaffeineResult.Error -> Timber.e("Failed to fetch content guide ${result.error}")
                is CaffeineResult.Failure -> Timber.e(result.throwable)
            }
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
        binding.guideRecyclerView.adapter = guideAdapter
    }
}

class GuideAdapter @Inject constructor(
        private val dispatchConfig: DispatchConfig,
        private val followManager: FollowManager
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
        return GuideViewHolder(view, this, followManager)
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
        private val followManager: FollowManager
) : RecyclerView.ViewHolder(itemView) {

    private val titleTextView: TextView = itemView.findViewById(R.id.title_text_view)

    var job: Job? = null

    fun bind(item: Guide) {
        job?.cancel()
        clear()
        job = scope.launch {
            val user = followManager.userDetails(item.caid) ?: return@launch
            // TODO (David): render the avatar and user name
        }
        titleTextView.text = item.title
    }

    private fun clear() {
        titleTextView.text = null
    }
}