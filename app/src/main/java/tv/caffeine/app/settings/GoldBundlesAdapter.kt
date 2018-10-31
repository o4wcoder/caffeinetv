package tv.caffeine.app.settings

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import tv.caffeine.app.api.GoldBundle
import tv.caffeine.app.databinding.GoldBundleItemBinding
import java.text.NumberFormat
import java.util.*
import javax.inject.Inject

interface GoldBundleClickListener {
    fun onClick(goldBundle: GoldBundle)
}

class GoldBundlesAdapter @Inject constructor(
        private val itemClickListener: GoldBundleClickListener
) : ListAdapter<GoldBundle, GoldBundleViewHolder>(object: DiffUtil.ItemCallback<GoldBundle?>() {
    override fun areItemsTheSame(oldItem: GoldBundle, newItem: GoldBundle) = oldItem.id == newItem.id

    override fun areContentsTheSame(oldItem: GoldBundle, newItem: GoldBundle) = oldItem == newItem
}) {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): GoldBundleViewHolder {
        val binding = GoldBundleItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return GoldBundleViewHolder(binding, itemClickListener)
    }

    override fun onBindViewHolder(holder: GoldBundleViewHolder, position: Int) {
        holder.bind(getItem(position))
    }
}

class GoldBundleViewHolder(
        private val binding: GoldBundleItemBinding,
        private val itemClickListener: GoldBundleClickListener
) : RecyclerView.ViewHolder(binding.root) {

    fun bind(goldBundle: GoldBundle) {
        binding.goldCostTextView.text = goldBundle.amount.toString()
        binding.dollarCostTextView.text = goldBundle.skuDetails?.let {
            NumberFormat.getCurrencyInstance().apply { currency = Currency.getInstance(it.priceCurrencyCode) }.format(it.priceAmountMicros/1000000f)
        } ?: goldBundle.amount.toString()
        itemView.setOnClickListener { itemClickListener.onClick(goldBundle) }
    }
}
