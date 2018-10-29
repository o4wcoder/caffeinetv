package tv.caffeine.app.settings

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import tv.caffeine.app.api.GoldBundle
import tv.caffeine.app.databinding.GoldBundleItemBinding
import javax.inject.Inject

class GoldBundlesAdapter @Inject constructor() : ListAdapter<GoldBundle, GoldBundleViewHolder>(object: DiffUtil.ItemCallback<GoldBundle?>() {
    override fun areItemsTheSame(oldItem: GoldBundle, newItem: GoldBundle) = oldItem.id == newItem.id

    override fun areContentsTheSame(oldItem: GoldBundle, newItem: GoldBundle) = oldItem == newItem
}) {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): GoldBundleViewHolder {
        val binding = GoldBundleItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return GoldBundleViewHolder(binding)
    }

    override fun onBindViewHolder(holder: GoldBundleViewHolder, position: Int) {
        holder.bind(getItem(position))
    }
}

class GoldBundleViewHolder(private val binding: GoldBundleItemBinding) : RecyclerView.ViewHolder(binding.root) {

    fun bind(goldBundle: GoldBundle) {
        binding.goldCostTextView.text = goldBundle.amount.toString()
        binding.dollarCostTextView.text = goldBundle.usingCredits?.cost?.toString()
    }
}
