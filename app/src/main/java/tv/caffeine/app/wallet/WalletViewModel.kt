package tv.caffeine.app.wallet

import androidx.lifecycle.LiveData
import androidx.lifecycle.Transformations
import tv.caffeine.app.api.Wallet
import tv.caffeine.app.ui.CaffeineViewModel
import tv.caffeine.app.util.DispatchConfig

class WalletViewModel(
        dispatchConfig: DispatchConfig,
        walletRepository: WalletRepository
): CaffeineViewModel(dispatchConfig) {

    val wallet: LiveData<Wallet> = Transformations.map(walletRepository.wallet) { it }

    init {
        walletRepository.refresh()
    }
}
