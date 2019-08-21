package tv.caffeine.app.explore

import android.os.Bundle
import android.view.View
import androidx.core.view.isVisible
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import tv.caffeine.app.R
import tv.caffeine.app.databinding.FragmentExploreBinding
import tv.caffeine.app.settings.ReleaseDesignConfig
import tv.caffeine.app.ui.CaffeineFragment
import tv.caffeine.app.util.clearItemDecoration
import tv.caffeine.app.util.setItemDecoration
import javax.inject.Inject

class ExploreFragment @Inject constructor(
    private val exploreAdapter: ExploreAdapter,
    private val searchUsersAdapter: SearchUsersAdapter,
    val releaseDesignConfig: ReleaseDesignConfig
) : CaffeineFragment(R.layout.fragment_explore) {

    private val viewModel: ExploreViewModel by viewModels { viewModelFactory }
    private lateinit var binding: FragmentExploreBinding

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        binding = FragmentExploreBinding.bind(view)

        val isReleaseDesign = releaseDesignConfig.isReleaseDesignActive()
        binding.searchEditTextClassic.isVisible = !isReleaseDesign
        binding.searchEditTextRelease.isVisible = isReleaseDesign

        viewModel.data.observe(viewLifecycleOwner, Observer { result ->
            handle(result) { findings ->
                val adapter: UsersAdapter
                when (findings) {
                    is Findings.Explore -> {
                        adapter = exploreAdapter
                        binding.exploreRecyclerView.apply {
                            layoutManager = GridLayoutManager(context, 3)
                            clearItemDecoration()
                        }
                    }
                    is Findings.Search -> {
                        adapter = searchUsersAdapter
                        binding.exploreRecyclerView.apply {
                            layoutManager = LinearLayoutManager(context)
                            setItemDecoration(DividerItemDecoration(context, DividerItemDecoration.VERTICAL))
                        }
                    }
                }
                adapter.fragmentManager = fragmentManager
                adapter.submitList(findings.data.toList())
                binding.exploreRecyclerView.adapter = adapter
            }
        })
        binding.exploreRecyclerView.adapter = searchUsersAdapter
        binding.viewModel = viewModel
    }
}
