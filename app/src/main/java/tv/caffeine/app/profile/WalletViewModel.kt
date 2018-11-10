package tv.caffeine.app.profile

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Transformations
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber
import tv.caffeine.app.api.GetWalletBody
import tv.caffeine.app.api.PaymentsClientService
import tv.caffeine.app.api.Wallet
import tv.caffeine.app.ui.CaffeineViewModel

class WalletViewModel(
        private val paymentsClientService: PaymentsClientService
): CaffeineViewModel() {
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
            withContext(Dispatchers.Main) {
                _wallet.value = wallet
            }
        }
    }
}
