package tv.caffeine.app.wallet

import androidx.lifecycle.LiveData
import androidx.lifecycle.Transformations
import androidx.lifecycle.ViewModel
import tv.caffeine.app.api.Wallet
import javax.inject.Inject

class WalletViewModel @Inject constructor(
    walletRepository: WalletRepository
) : ViewModel() {

    val wallet: LiveData<Wallet> = Transformations.map(walletRepository.wallet) { it }

    init {
        walletRepository.refresh()
    }
}
