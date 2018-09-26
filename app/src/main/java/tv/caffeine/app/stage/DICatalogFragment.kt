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
import tv.caffeine.app.api.DigitalItem
import tv.caffeine.app.databinding.DiCatalogItemBinding
import tv.caffeine.app.databinding.FragmentDiCatalogBinding
import tv.caffeine.app.profile.WalletViewModel
import tv.caffeine.app.ui.CaffeineBottomSheetDialogFragment

class DICatalogFragment : CaffeineBottomSheetDialogFragment() {

    private val adapter = DigitalItemAdapter()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val viewModel = viewModelProvider.get(DICatalogViewModel::class.java)
        viewModel.items.observe(this, Observer {
            adapter.submitList(it.digitalItems.state)
        })
        viewModel.refresh()
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        val binding = FragmentDiCatalogBinding.inflate(inflater, container, false)
        binding.walletViewModel = viewModelProvider.get(WalletViewModel::class.java)
        binding.username = DICatalogFragmentArgs.fromBundle(arguments).broadcaster
        binding.list.adapter = adapter
        binding.setLifecycleOwner(this)
        return binding.root
    }

    private inner class ViewHolder internal constructor(val binding: DiCatalogItemBinding)
        : RecyclerView.ViewHolder(binding.root) {

        fun bind(digitalItem: DigitalItem) {
            binding.digitalItem = digitalItem
            binding.nameTextView.text = digitalItem.name
            binding.goldCostTextView.text = digitalItem.goldCost.toString()
            Picasso.get()
                    .load(digitalItem.staticImageUrl)
                    .into(binding.previewImageView)
        }
    }

    val diffCallback = object : DiffUtil.ItemCallback<DigitalItem?>() {
        override fun areItemsTheSame(oldItem: DigitalItem, newItem: DigitalItem) = oldItem.id == newItem.id

        override fun areContentsTheSame(oldItem: DigitalItem, newItem: DigitalItem) = oldItem == newItem
    }

    private inner class DigitalItemAdapter internal constructor() : ListAdapter<DigitalItem, ViewHolder>(diffCallback) {

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val binding = DiCatalogItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
            return ViewHolder(binding)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            holder.bind(getItem(position))
        }
    }

}
