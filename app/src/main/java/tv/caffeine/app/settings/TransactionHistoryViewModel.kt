package tv.caffeine.app.settings

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Transformations
import kotlinx.coroutines.launch
import tv.caffeine.app.api.PaymentsEnvelope
import tv.caffeine.app.api.TransactionHistoryPayload
import tv.caffeine.app.api.model.CaffeineResult
import tv.caffeine.app.ui.CaffeineViewModel

class TransactionHistoryViewModel(
        private val transactionHistoryUseCase: TransactionHistoryUseCase
): CaffeineViewModel() {
    private val _transactionHistory = MutableLiveData<CaffeineResult<PaymentsEnvelope<TransactionHistoryPayload>>>()
    val transactionHistory: LiveData<CaffeineResult<PaymentsEnvelope<TransactionHistoryPayload>>> = Transformations.map(_transactionHistory) { it }

    init {
        load()
    }

    private fun load() {
        launch {
            _transactionHistory.value = runCatching { transactionHistoryUseCase() }.fold({ it }, { CaffeineResult.Failure(it) })
        }
    }
}
