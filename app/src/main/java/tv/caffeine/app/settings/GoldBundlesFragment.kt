package tv.caffeine.app.settings

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.Observer
import tv.caffeine.app.databinding.FragmentGoldBundlesBinding
import tv.caffeine.app.ui.CaffeineFragment
import javax.inject.Inject

class GoldBundlesFragment : CaffeineFragment() {

    private lateinit var binding: FragmentGoldBundlesBinding
    private val viewModel by lazy { viewModelProvider.get(GoldBundlesViewModel::class.java) }
    @Inject lateinit var goldBundlesAdapter: GoldBundlesAdapter

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        // Inflate the layout for this fragment
        binding = FragmentGoldBundlesBinding.inflate(inflater, container, false).apply {
            setLifecycleOwner(viewLifecycleOwner)
            goldBundlesRecyclerView.adapter = goldBundlesAdapter
        }
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        viewModel.goldBundles.observe(viewLifecycleOwner, Observer { result ->
            handle(result, view) { paymentsEnvelope ->
                val list = paymentsEnvelope.payload.goldBundles.state
                goldBundlesAdapter.submitList(list)
            }
        })
    }

}
