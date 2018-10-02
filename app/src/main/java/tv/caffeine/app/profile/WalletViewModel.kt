package tv.caffeine.app.profile

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import timber.log.Timber
import tv.caffeine.app.api.GetWalletBody
import tv.caffeine.app.api.PaymentsClientService

class WalletViewModel(
        private val paymentsClientService: PaymentsClientService
): ViewModel() {
    val walletBalance = MutableLiveData<Int>()

    private val job: Job = GlobalScope.launch(Dispatchers.IO) {
        val deferred = paymentsClientService.getWallet(GetWalletBody())
        val wallet = deferred.await().payload
        Timber.d("Wallet: ${wallet.gold}")
        launch(Dispatchers.Main) {
            walletBalance.value = wallet.gold
        }
    }

    override fun onCleared() {
        job.cancel()
        super.onCleared()
    }
}