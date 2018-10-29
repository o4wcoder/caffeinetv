package tv.caffeine.app.settings

import android.os.Bundle
import android.os.Parcelable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.navigation.fragment.findNavController
import kotlinx.android.parcel.Parcelize
import tv.caffeine.app.databinding.FragmentGoldAndCreditsBinding
import tv.caffeine.app.profile.WalletViewModel
import tv.caffeine.app.ui.CaffeineFragment

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
        super.onViewCreated(view, savedInstanceState)
        binding.transactionHistoryButton.setOnClickListener {
            val action = GoldAndCreditsFragmentDirections.actionGoldAndCreditsFragmentToTransactionHistoryFragment()
            findNavController().navigate(action)
        }
        binding.buyGoldButton.setOnClickListener { navigateToBuyGold(BuyGoldOption.UsingPlayStore) }
        binding.buyGoldWithCreditsButton.setOnClickListener { navigateToBuyGold(BuyGoldOption.UsingCredits) }
    }

    private fun navigateToBuyGold(buyGoldOption: BuyGoldOption) {
        val action = GoldAndCreditsFragmentDirections.actionGoldAndCreditsFragmentToGoldBundlesFragment(buyGoldOption)
        findNavController().navigate(action)
    }
}
