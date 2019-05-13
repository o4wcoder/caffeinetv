package tv.caffeine.app.lobby

import android.os.Bundle
import android.view.View
import androidx.core.view.isVisible
import androidx.fragment.app.viewModels
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import androidx.lifecycle.Transformations
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import tv.caffeine.app.R
import tv.caffeine.app.databinding.FragmentFeaturedProgramGuideBinding
import tv.caffeine.app.ui.CaffeineFragment
import javax.inject.Inject

class FeaturedProgramGuideFragment @Inject constructor(
    private val guideAdapter: FeaturedProgramGuideAdapter
) : CaffeineFragment(R.layout.fragment_featured_program_guide) {

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
    private val loadFeaturedProgramGuideUseCase: LoadFeaturedProgramGuideUseCase
) : ViewModel() {

    private val _listings = MutableLiveData<List<FeaturedGuideItem>>()
    val listings: LiveData<List<FeaturedGuideItem>> = Transformations.map(_listings) { it }

    private var refreshJob: Job? = null

    init {
        load()
    }

    fun load() {
        refreshJob?.cancel()
        refreshJob = viewModelScope.launch {
            _listings.value = loadFeaturedProgramGuideUseCase()
        }
    }
}
