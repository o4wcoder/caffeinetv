package tv.caffeine.app.settings

import android.os.Bundle
import android.os.Parcelable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.navigation.fragment.findNavController
import com.squareup.picasso.Picasso
import kotlinx.android.parcel.Parcelize
import tv.caffeine.app.R
import tv.caffeine.app.databinding.FragmentGoldAndCreditsBinding
import tv.caffeine.app.ui.CaffeineFragment
import tv.caffeine.app.ui.formatUsernameAsHtml
import tv.caffeine.app.util.safeNavigate
import tv.caffeine.app.wallet.WalletViewModel
import java.text.NumberFormat
import javax.inject.Inject

@Parcelize
enum class BuyGoldOption: Parcelable {
    UsingPlayStore, UsingCredits
}

class GoldAndCreditsFragment : CaffeineFragment() {
    @Inject lateinit var picasso: Picasso
    private lateinit var binding: FragmentGoldAndCreditsBinding
    private val walletViewModel: WalletViewModel by viewModels { viewModelFactory }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        binding = FragmentGoldAndCreditsBinding.inflate(inflater, container, false).apply {
            lifecycleOwner = viewLifecycleOwner
        }
        binding.walletViewModel = walletViewModel
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val numberFormat = NumberFormat.getIntegerInstance()
        walletViewModel.wallet.observe(viewLifecycleOwner, Observer {  wallet ->
            binding.goldBalanceTextView.formatUsernameAsHtml(picasso, getString(R.string.gold_formatted, numberFormat.format(wallet.gold)))
            binding.creditBalanceTextView.formatUsernameAsHtml(picasso, getString(R.string.credits_formatted, numberFormat.format(wallet.credits)))
            binding.cumulativeCreditBalanceTextView.formatUsernameAsHtml(picasso, getString(R.string.credits_formatted, numberFormat.format(wallet.cumulativeCredits)))
        })
        binding.transactionHistoryButton.setOnClickListener {
            val action = GoldAndCreditsFragmentDirections.actionGoldAndCreditsFragmentToTransactionHistoryFragment()
            findNavController().safeNavigate(action)
        }
        binding.buyGoldButton.setOnClickListener { navigateToBuyGold(BuyGoldOption.UsingPlayStore) }
        binding.buyGoldWithCreditsButton.setOnClickListener { navigateToBuyGold(BuyGoldOption.UsingCredits) }
        binding.goldAndCreditsHelpTextView.formatUsernameAsHtml(picasso, getString(R.string.gold_and_credits_help_html))
    }

    private fun navigateToBuyGold(buyGoldOption: BuyGoldOption) {
        val action = GoldAndCreditsFragmentDirections.actionGoldAndCreditsFragmentToGoldBundlesFragment(buyGoldOption)
        findNavController().safeNavigate(action)
    }
}
