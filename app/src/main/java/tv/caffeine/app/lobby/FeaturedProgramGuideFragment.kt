package tv.caffeine.app.lobby

import android.os.Bundle
import android.view.View
import androidx.core.view.isVisible
import androidx.fragment.app.viewModels
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import androidx.lifecycle.Transformations
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import tv.caffeine.app.R
import tv.caffeine.app.databinding.FragmentFeaturedProgramGuideBinding
import tv.caffeine.app.ui.CaffeineFragment
import tv.caffeine.app.ui.CaffeineViewModel
import tv.caffeine.app.util.DispatchConfig
import javax.inject.Inject

class FeaturedProgramGuideFragment : CaffeineFragment(R.layout.fragment_featured_program_guide) {

    @Inject lateinit var guideAdapter: FeaturedProgramGuideAdapter
    private var binding: FragmentFeaturedProgramGuideBinding? = null
    private val viewModel: FeaturedProgramGuideViewModel by viewModels { viewModelFactory }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        binding = FragmentFeaturedProgramGuideBinding.bind(view).apply {
            configure(this)
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

class FeaturedProgramGuideViewModel @Inject constructor(
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

