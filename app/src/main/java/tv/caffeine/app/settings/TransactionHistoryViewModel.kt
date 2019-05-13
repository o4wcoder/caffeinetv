package tv.caffeine.app.settings

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Transformations
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import tv.caffeine.app.api.PaymentsEnvelope
import tv.caffeine.app.api.TransactionHistoryPayload
import tv.caffeine.app.api.model.CaffeineResult
import javax.inject.Inject

class TransactionHistoryViewModel @Inject constructor(
    private val transactionHistoryUseCase: TransactionHistoryUseCase
) : ViewModel() {
    private val _transactionHistory = MutableLiveData<CaffeineResult<PaymentsEnvelope<TransactionHistoryPayload>>>()
    val transactionHistory: LiveData<CaffeineResult<PaymentsEnvelope<TransactionHistoryPayload>>> = Transformations.map(_transactionHistory) { it }

    init {
        load()
    }

    private fun load() {
        viewModelScope.launch {
            _transactionHistory.value = transactionHistoryUseCase()
        }
    }
}
