package tv.caffeine.app.profile

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Transformations
import com.google.gson.Gson
import kotlinx.coroutines.launch
import timber.log.Timber
import tv.caffeine.app.api.GetWalletBody
import tv.caffeine.app.api.PaymentsClientService
import tv.caffeine.app.api.Wallet
import tv.caffeine.app.api.model.CaffeineResult
import tv.caffeine.app.api.model.awaitAndParseErrors
import tv.caffeine.app.ui.CaffeineViewModel
import tv.caffeine.app.util.DispatchConfig

class WalletViewModel(
        dispatchConfig: DispatchConfig,
        private val gson: Gson,
        private val paymentsClientService: PaymentsClientService
): CaffeineViewModel(dispatchConfig) {
    private val _wallet = MutableLiveData<Wallet>()
    val wallet: LiveData<Wallet> = Transformations.map(_wallet) { it }

    init {
        load()
    }

    private fun load() {
        launch {
            val result = paymentsClientService.getWallet(GetWalletBody()).awaitAndParseErrors(gson)
            when(result) {
                is CaffeineResult.Success -> _wallet.value = result.value.payload
                is CaffeineResult.Error -> Timber.e("Error loading wallet ${result.error}")
                is CaffeineResult.Failure -> Timber.e(result.throwable)
            }
        }
    }
}
