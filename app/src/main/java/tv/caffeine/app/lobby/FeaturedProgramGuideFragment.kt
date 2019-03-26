package tv.caffeine.app.lobby

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.viewModels
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import androidx.lifecycle.Transformations
import com.google.gson.Gson
import kotlinx.coroutines.launch
import timber.log.Timber
import tv.caffeine.app.api.BroadcastsService
import tv.caffeine.app.api.FeaturedGuideListing
import tv.caffeine.app.api.model.CaffeineResult
import tv.caffeine.app.api.model.awaitAndParseErrors
import tv.caffeine.app.databinding.FragmentFeaturedProgramGuideBinding
import tv.caffeine.app.lobby.FeaturedGuideItem.ListingItem
import tv.caffeine.app.session.FollowManager
import tv.caffeine.app.ui.CaffeineFragment
import tv.caffeine.app.ui.CaffeineViewModel
import tv.caffeine.app.util.DispatchConfig
import java.util.*
import java.util.concurrent.TimeUnit
import javax.inject.Inject

class FeaturedProgramGuideFragment : CaffeineFragment() {

    @Inject lateinit var guideAdapter: FeaturedProgramGuideAdapter
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
        listingItems.getOrNull(0)?.let { timestampIndices.add(0) }
        for (i in 1 until listingItems.size) {
            if (isDifferentDay(listingItems[i - 1].listing, listingItems[i].listing)) {
                timestampIndices.add(i)
            }
        }
        val featuredGuideItems = LinkedList<FeaturedGuideItem>(listingItems)
        for (i in timestampIndices.reversed()) {
            featuredGuideItems.add(i, FeaturedGuideItem.DateHeader(listingItems[i].listing.startTimestamp))
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

