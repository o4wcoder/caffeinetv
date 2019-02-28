package tv.caffeine.app.settings


import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.DividerItemDecoration
import timber.log.Timber
import tv.caffeine.app.R
import tv.caffeine.app.api.convert
import tv.caffeine.app.databinding.FragmentTransactionHistoryBinding
import tv.caffeine.app.ui.CaffeineFragment
import tv.caffeine.app.ui.PaddingItemDecoration
import javax.inject.Inject

class TransactionHistoryFragment : CaffeineFragment() {

    @Inject lateinit var adapter: TransactionHistoryAdapter

    private lateinit var binding: FragmentTransactionHistoryBinding
    private val viewModel by lazy { viewModelProvider.get(TransactionHistoryViewModel::class.java) }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        // Inflate the layout for this fragment
        binding = FragmentTransactionHistoryBinding.inflate(inflater, container, false).apply {
            lifecycleOwner = viewLifecycleOwner
            transactionHistoryRecyclerView.adapter = adapter
            val padding = resources.getDimensionPixelSize(R.dimen.margin_line_spacing)
            val paddingItemDecoration = PaddingItemDecoration(paddingLeft = 0, paddingTop = padding, paddingRight = 0, paddingBottom = padding)
            transactionHistoryRecyclerView.addItemDecoration(paddingItemDecoration)
            val drawable = context?.let { ContextCompat.getDrawable(it, R.drawable.gray_top_divider) }
            if (drawable != null) {
                transactionHistoryRecyclerView.addItemDecoration(DividerItemDecoration(context, DividerItemDecoration.VERTICAL).apply { setDrawable(drawable) })
            }
        }
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
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
