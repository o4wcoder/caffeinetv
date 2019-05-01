package tv.caffeine.app.settings


import android.os.Bundle
import android.view.View
import androidx.core.content.ContextCompat
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.DividerItemDecoration
import timber.log.Timber
import tv.caffeine.app.R
import tv.caffeine.app.api.convert
import tv.caffeine.app.databinding.FragmentTransactionHistoryBinding
import tv.caffeine.app.ui.CaffeineFragment
import tv.caffeine.app.ui.PaddingItemDecoration
import javax.inject.Inject

class TransactionHistoryFragment : CaffeineFragment(R.layout.fragment_transaction_history) {

    @Inject lateinit var adapter: TransactionHistoryAdapter

    private val viewModel: TransactionHistoryViewModel by viewModels { viewModelFactory }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val binding = FragmentTransactionHistoryBinding.bind(view)
        binding.lifecycleOwner = viewLifecycleOwner
        binding.transactionHistoryRecyclerView.adapter = adapter
        val padding = resources.getDimensionPixelSize(R.dimen.margin_line_spacing)
        val paddingItemDecoration = PaddingItemDecoration(paddingLeft = 0, paddingTop = padding, paddingRight = 0, paddingBottom = padding)
        binding.transactionHistoryRecyclerView.addItemDecoration(paddingItemDecoration)
        val drawable = context?.let { ContextCompat.getDrawable(it, R.drawable.gray_top_divider) }
        if (drawable != null) {
            val dividerItemDecoration = DividerItemDecoration(context, DividerItemDecoration.VERTICAL).apply {
                setDrawable(drawable)
            }
            binding.transactionHistoryRecyclerView.addItemDecoration(dividerItemDecoration)
        }
        viewModel.transactionHistory.observe(viewLifecycleOwner, Observer { result ->
            Timber.d("Got transaction history results")
            handle(result) { paymentsEnvelope ->
                val list = paymentsEnvelope.payload.transactions.state.mapNotNull { it.convert() }
                Timber.d("Transaction history $list")
                adapter.submitList(list)
            }
        })
    }
}
