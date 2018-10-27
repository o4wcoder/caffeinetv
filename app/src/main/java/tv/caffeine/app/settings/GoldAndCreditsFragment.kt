package tv.caffeine.app.settings

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.navigation.fragment.findNavController
import tv.caffeine.app.databinding.FragmentGoldAndCreditsBinding
import tv.caffeine.app.profile.WalletViewModel
import tv.caffeine.app.ui.CaffeineFragment

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
    }
}
