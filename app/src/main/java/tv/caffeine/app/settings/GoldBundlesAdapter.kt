package tv.caffeine.app.settings

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import tv.caffeine.app.R
import tv.caffeine.app.api.GoldBundle
import tv.caffeine.app.databinding.GoldBundleItemBinding
import tv.caffeine.app.ui.htmlText
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
        val numberFormat = NumberFormat.getNumberInstance()
        binding.goldCostTextView.text = numberFormat.format(goldBundle.amount)
        val skuDetails = goldBundle.skuDetails
        if (skuDetails != null) {
            val currencyFormat = NumberFormat.getCurrencyInstance().apply { currency = Currency.getInstance(skuDetails.priceCurrencyCode) }
            binding.dollarCostTextView.text = currencyFormat.format(skuDetails.priceAmountMicros/1000000f)
        } else if (goldBundle.usingCredits != null) {
            val amount = numberFormat.format(goldBundle.usingCredits.cost)
            binding.dollarCostTextView.htmlText = itemView.resources.getString(R.string.credits_formatted, amount)
        } else {
            binding.dollarCostTextView.text = null
        }
        itemView.setOnClickListener { itemClickListener.onClick(goldBundle) }
    }
}
