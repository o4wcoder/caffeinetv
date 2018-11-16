package tv.caffeine.app.profile

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Transformations
import kotlinx.coroutines.launch
import timber.log.Timber
import tv.caffeine.app.api.GetWalletBody
import tv.caffeine.app.api.PaymentsClientService
import tv.caffeine.app.api.Wallet
import tv.caffeine.app.ui.CaffeineViewModel
import tv.caffeine.app.util.DispatchConfig

class WalletViewModel(
        dispatchConfig: DispatchConfig,
        private val paymentsClientService: PaymentsClientService
): CaffeineViewModel(dispatchConfig) {
    private val _wallet = MutableLiveData<Wallet>()
    val wallet: LiveData<Wallet> = Transformations.map(_wallet) { it }

    init {
        load()
    }

    private fun load() {
        launch {
            val deferred = paymentsClientService.getWallet(GetWalletBody())
            val wallet = deferred.await().payload
            Timber.d("Wallet: ${wallet.gold}")
            _wallet.value = wallet
        }
    }
}
