package tv.caffeine.app.explore


import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import tv.caffeine.app.databinding.FragmentExploreBinding
import tv.caffeine.app.ui.CaffeineFragment
import javax.inject.Inject

class ExploreFragment : CaffeineFragment() {

    @Inject lateinit var exploreAdapter: ExploreAdapter
    @Inject lateinit var searchUsersAdapter: SearchUsersAdapter

    private val viewModel by lazy { viewModelProvider.get(ExploreViewModel::class.java) }
    private lateinit var binding: FragmentExploreBinding

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        // Inflate the layout for this fragment
        binding = FragmentExploreBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        viewModel.data.observe(viewLifecycleOwner, Observer { result ->
            handle(result, view) { findings ->
                val adapter: UsersAdapter
                when (findings) {
                    is Findings.Explore -> {
                        adapter = exploreAdapter
                        binding.exploreRecyclerView.layoutManager = GridLayoutManager(context, 3)
                    }
                    is Findings.Search -> {
                        adapter = searchUsersAdapter
                        binding.exploreRecyclerView.layoutManager = LinearLayoutManager(context)
                    }
                }
                adapter.submitList(findings.data.toList())
                binding.exploreRecyclerView.adapter = adapter
            }
        })
        binding.exploreRecyclerView.adapter = searchUsersAdapter
        binding.viewModel = viewModel
    }
}
