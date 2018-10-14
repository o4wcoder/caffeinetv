package tv.caffeine.app.settings

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.Observer
import tv.caffeine.app.databinding.FragmentGoldAndCreditsBinding
import tv.caffeine.app.profile.WalletViewModel
import tv.caffeine.app.ui.CaffeineFragment

class GoldAndCreditsFragment : CaffeineFragment() {
    private lateinit var binding: FragmentGoldAndCreditsBinding

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        binding = FragmentGoldAndCreditsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val walletViewModel = viewModelProvider.get(WalletViewModel::class.java)
        walletViewModel.walletBalance.observe(this, Observer {
            binding.goldBalanceTextView.text = it.toString()
        })
    }
}
