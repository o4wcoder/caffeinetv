package tv.caffeine.app.stage

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.squareup.picasso.Picasso
import tv.caffeine.app.R
import tv.caffeine.app.api.DigitalItem
import tv.caffeine.app.databinding.DiCatalogItemBinding
import tv.caffeine.app.databinding.FragmentDiCatalogBinding
import tv.caffeine.app.profile.WalletViewModel
import tv.caffeine.app.ui.CaffeineBottomSheetDialogFragment
import tv.caffeine.app.ui.htmlText
import java.text.NumberFormat

class DICatalogFragment : CaffeineBottomSheetDialogFragment() {

    interface Callback {
        fun digitalItemSelected(digitalItem: DigitalItem)
    }

    private val adapter = DigitalItemAdapter(object: Callback {
        override fun digitalItemSelected(digitalItem: DigitalItem) {
            val callback = targetFragment as? Callback ?: return
            callback.digitalItemSelected(digitalItem)
            dismiss()
        }
    })
    private val viewModel by lazy { viewModelProvider.get(DICatalogViewModel::class.java) }
    private val walletViewModel by lazy { viewModelProvider.get(WalletViewModel::class.java) }
    private lateinit var binding: FragmentDiCatalogBinding

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        binding = FragmentDiCatalogBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        walletViewModel.wallet.observe(viewLifecycleOwner, Observer {  wallet ->
            val numberFormat = NumberFormat.getIntegerInstance()
            binding.walletBalanceTextView.htmlText = getString(R.string.wallet_balance, numberFormat.format(wallet.gold))
        })
        binding.username = DICatalogFragmentArgs.fromBundle(arguments).broadcaster
        binding.list.adapter = adapter
        binding.setLifecycleOwner(viewLifecycleOwner)
        viewModel.items.observe(viewLifecycleOwner, Observer {
            adapter.submitList(it.digitalItems.state)
        })
    }

}

private class DigitalItemViewHolder constructor(
        val binding: DiCatalogItemBinding,
        val callback: DICatalogFragment.Callback
)
    : RecyclerView.ViewHolder(binding.root) {

    fun bind(digitalItem: DigitalItem) {
        binding.digitalItem = digitalItem
        binding.nameTextView.text = digitalItem.name
        binding.goldCostTextView.text = digitalItem.goldCost.toString()
        Picasso.get()
                .load(digitalItem.staticImageUrl)
                .into(binding.previewImageView)
        itemView.setOnClickListener {
            callback.digitalItemSelected(digitalItem)
        }
    }
}


private class DigitalItemAdapter constructor(
        private val callback: DICatalogFragment.Callback
) : ListAdapter<DigitalItem, DigitalItemViewHolder>(
        object : DiffUtil.ItemCallback<DigitalItem>() {
            override fun areItemsTheSame(oldItem: DigitalItem, newItem: DigitalItem) = oldItem.id == newItem.id

            override fun areContentsTheSame(oldItem: DigitalItem, newItem: DigitalItem) = oldItem == newItem
        }
) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DigitalItemViewHolder {
        val binding = DiCatalogItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return DigitalItemViewHolder(binding, callback)
    }

    override fun onBindViewHolder(holder: DigitalItemViewHolder, position: Int) {
        holder.bind(getItem(position))
    }
}
