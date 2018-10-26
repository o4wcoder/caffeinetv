package tv.caffeine.app.settings

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import kotlinx.coroutines.launch
import tv.caffeine.app.api.PaymentsEnvelope
import tv.caffeine.app.api.TransactionHistoryPayload
import tv.caffeine.app.api.model.CaffeineResult
import tv.caffeine.app.ui.CaffeineViewModel

class TransactionHistoryViewModel(
        private val transactionHistoryUseCase: TransactionHistoryUseCase
): CaffeineViewModel() {
    val transactionHistory: LiveData<CaffeineResult<PaymentsEnvelope<TransactionHistoryPayload>>> get() = _transactionHistory
    private val _transactionHistory = MutableLiveData<CaffeineResult<PaymentsEnvelope<TransactionHistoryPayload>>>()

    init {
        load()
    }

    private fun load() {
        launch {
            _transactionHistory.value = runCatching { transactionHistoryUseCase() }.fold({ it }, { CaffeineResult.Failure(it) })
        }
    }
}
