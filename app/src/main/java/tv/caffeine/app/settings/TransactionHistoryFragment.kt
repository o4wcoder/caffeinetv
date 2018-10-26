package tv.caffeine.app.settings


import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.Observer
import timber.log.Timber
import tv.caffeine.app.api.convert
import tv.caffeine.app.databinding.FragmentTransactionHistoryBinding
import tv.caffeine.app.ui.CaffeineFragment
import javax.inject.Inject

class TransactionHistoryFragment : CaffeineFragment() {
    private lateinit var binding: tv.caffeine.app.databinding.FragmentTransactionHistoryBinding
    private val viewModel by lazy { viewModelProvider.get(TransactionHistoryViewModel::class.java) }
    @Inject lateinit var adapter: TransactionHistoryAdapter

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        // Inflate the layout for this fragment
        binding = FragmentTransactionHistoryBinding.inflate(inflater, container, false).apply {
            setLifecycleOwner(viewLifecycleOwner)
            transactionHistoryRecyclerView.adapter = adapter
        }
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewModel.transactionHistory.observe(viewLifecycleOwner, Observer { result ->
            Timber.d("Got transaction history results")
            handle(result, view) { paymentsEnvelope ->
                val list = paymentsEnvelope.payload.transactions.state.mapNotNull { it.convert() }
                Timber.d("Transaction history $list")
                adapter.submitList(list)
            }
        })
    }

}
