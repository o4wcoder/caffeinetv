package tv.caffeine.app.settings

import android.os.Bundle
import android.os.Parcelable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.Observer
import androidx.navigation.fragment.findNavController
import kotlinx.android.parcel.Parcelize
import tv.caffeine.app.R
import tv.caffeine.app.databinding.FragmentGoldAndCreditsBinding
import tv.caffeine.app.wallet.WalletViewModel
import tv.caffeine.app.ui.CaffeineFragment
import tv.caffeine.app.ui.htmlText
import tv.caffeine.app.util.safeNavigate
import java.text.NumberFormat

@Parcelize
enum class BuyGoldOption: Parcelable {
    UsingPlayStore, UsingCredits
}

class GoldAndCreditsFragment : CaffeineFragment() {
    private lateinit var binding: FragmentGoldAndCreditsBinding
    private val walletViewModel by lazy { viewModelProvider.get(WalletViewModel::class.java) }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        binding = FragmentGoldAndCreditsBinding.inflate(inflater, container, false).apply {
            setLifecycleOwner(viewLifecycleOwner)
        }
        binding.walletViewModel = walletViewModel
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val numberFormat = NumberFormat.getIntegerInstance()
        walletViewModel.wallet.observe(viewLifecycleOwner, Observer {  wallet ->
            binding.goldBalanceTextView.htmlText = getString(R.string.gold_formatted, numberFormat.format(wallet.gold))
            binding.creditBalanceTextView.htmlText = getString(R.string.credits_formatted, numberFormat.format(wallet.credits))
            binding.cumulativeCreditBalanceTextView.htmlText = getString(R.string.credits_formatted, numberFormat.format(wallet.cumulativeCredits))
        })
        binding.transactionHistoryButton.setOnClickListener {
            val action = GoldAndCreditsFragmentDirections.actionGoldAndCreditsFragmentToTransactionHistoryFragment()
            findNavController().safeNavigate(action)
        }
        binding.buyGoldButton.setOnClickListener { navigateToBuyGold(BuyGoldOption.UsingPlayStore) }
        binding.buyGoldWithCreditsButton.setOnClickListener { navigateToBuyGold(BuyGoldOption.UsingCredits) }
        binding.goldAndCreditsHelpTextView.htmlText = getString(R.string.gold_and_credits_help_html)
    }

    private fun navigateToBuyGold(buyGoldOption: BuyGoldOption) {
        val action = GoldAndCreditsFragmentDirections.actionGoldAndCreditsFragmentToGoldBundlesFragment(buyGoldOption)
        findNavController().safeNavigate(action)
    }
}
