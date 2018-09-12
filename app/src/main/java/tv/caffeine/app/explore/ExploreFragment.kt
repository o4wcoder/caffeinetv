package tv.caffeine.app.explore


import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import androidx.recyclerview.widget.LinearLayoutManager
import dagger.android.support.DaggerFragment
import timber.log.Timber
import tv.caffeine.app.databinding.FragmentExploreBinding
import tv.caffeine.app.di.ViewModelFactory
import javax.inject.Inject

class ExploreFragment : DaggerFragment() {

    @Inject lateinit var viewModelFactory: ViewModelFactory
    @Inject lateinit var exploreAdapter: ExploreAdapter
    private val viewModelProvider by lazy { ViewModelProviders.of(this, viewModelFactory) }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        // Inflate the layout for this fragment
        val binding = FragmentExploreBinding.inflate(inflater, container, false)
        val viewModel = viewModelProvider.get(ExploreViewModel::class.java)
        viewModel.data.observe(this, Observer {
            Timber.d("Got results $it")
            exploreAdapter.submitList(it.toList())
        })
        viewModel.queryString = "" // trigger suggestions
        binding.exploreRecyclerView.layoutManager = LinearLayoutManager(context)
        binding.exploreRecyclerView.adapter = exploreAdapter
        binding.viewModel = viewModel
        return binding.root
    }

}
