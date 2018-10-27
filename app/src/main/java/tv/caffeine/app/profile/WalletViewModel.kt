package tv.caffeine.app.profile

import androidx.lifecycle.MutableLiveData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber
import tv.caffeine.app.api.GetWalletBody
import tv.caffeine.app.api.PaymentsClientService
import tv.caffeine.app.ui.CaffeineViewModel

class WalletViewModel(
        private val paymentsClientService: PaymentsClientService
): CaffeineViewModel() {
    val walletBalance = MutableLiveData<String>()
    val creditsBalance = MutableLiveData<String>()
    val cumulativeCreditsBalance = MutableLiveData<String>()

    init {
        load()
    }

    private fun load() {
        launch {
            val deferred = paymentsClientService.getWallet(GetWalletBody())
            val wallet = deferred.await().payload
            Timber.d("Wallet: ${wallet.gold}")
            withContext(Dispatchers.Main) {
                walletBalance.value = wallet.gold.toString()
                creditsBalance.value = wallet.credits.toString()
                cumulativeCreditsBalance.value = wallet.cumulativeCredits.toString()
            }
        }
    }
}
