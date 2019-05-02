package tv.caffeine.app.wallet

import androidx.lifecycle.LiveData
import androidx.lifecycle.Transformations
import tv.caffeine.app.api.Wallet
import tv.caffeine.app.ui.CaffeineViewModel
import tv.caffeine.app.util.DispatchConfig
import javax.inject.Inject

class WalletViewModel @Inject constructor(
        dispatchConfig: DispatchConfig,
        walletRepository: WalletRepository
): CaffeineViewModel(dispatchConfig) {

    val wallet: LiveData<Wallet> = Transformations.map(walletRepository.wallet) { it }

    init {
        walletRepository.refresh()
    }
}
