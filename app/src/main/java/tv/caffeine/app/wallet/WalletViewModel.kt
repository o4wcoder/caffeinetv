package tv.caffeine.app.wallet

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.map
import tv.caffeine.app.api.Wallet
import javax.inject.Inject

class WalletViewModel @Inject constructor(
    walletRepository: WalletRepository
) : ViewModel() {

    val wallet: LiveData<Wallet> = walletRepository.wallet.map { it }

    init {
        walletRepository.refresh()
    }
}
