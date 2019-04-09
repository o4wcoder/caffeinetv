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
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import tv.caffeine.app.databinding.FragmentFeaturedProgramGuideBinding
import tv.caffeine.app.ui.CaffeineFragment
import tv.caffeine.app.ui.CaffeineViewModel
import tv.caffeine.app.util.DispatchConfig
import javax.inject.Inject

class FeaturedProgramGuideFragment : CaffeineFragment() {

    @Inject lateinit var guideAdapter: FeaturedProgramGuideAdapter
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
        private val loadFeaturedProgramGuideUseCase: LoadFeaturedProgramGuideUseCase
) : CaffeineViewModel(dispatchConfig) {

    private val _listings = MutableLiveData<List<FeaturedGuideItem>>()
    val listings: LiveData<List<FeaturedGuideItem>> = Transformations.map(_listings) { it }

    private var refreshJob: Job? = null

    init {
        load()
    }

    fun load() {
        refreshJob?.cancel()
        refreshJob = launch {
            _listings.value = loadFeaturedProgramGuideUseCase()
        }
    }
}

