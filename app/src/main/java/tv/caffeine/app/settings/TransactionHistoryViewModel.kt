package tv.caffeine.app.settings

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.map
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import tv.caffeine.app.api.PaymentsEnvelope
import tv.caffeine.app.api.TransactionHistoryPayload
import tv.caffeine.app.api.model.CaffeineResult
import tv.caffeine.app.repository.TransactionHistoryRepository
import javax.inject.Inject

class TransactionHistoryViewModel @Inject constructor(
    private val transactionHistoryRepository: TransactionHistoryRepository
) : ViewModel() {
    private val _transactionHistory = MutableLiveData<CaffeineResult<PaymentsEnvelope<TransactionHistoryPayload>>>()
    val transactionHistory: LiveData<CaffeineResult<PaymentsEnvelope<TransactionHistoryPayload>>> = _transactionHistory.map { it }

    init {
        load()
    }

    private fun load() {
        viewModelScope.launch {
            _transactionHistory.value = transactionHistoryRepository.getTransactionHistory()
        }
    }
}
