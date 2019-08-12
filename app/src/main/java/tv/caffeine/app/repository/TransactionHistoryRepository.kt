package tv.caffeine.app.repository

import com.google.gson.Gson
import tv.caffeine.app.api.GetTransactionHistoryBody
import tv.caffeine.app.api.PaymentsClientService
import tv.caffeine.app.api.PaymentsEnvelope
import tv.caffeine.app.api.TransactionHistoryPayload
import tv.caffeine.app.api.model.CaffeineResult
import tv.caffeine.app.api.model.awaitAndParseErrors
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TransactionHistoryRepository @Inject constructor(
    private val paymentsClientService: PaymentsClientService,
    private val gson: Gson
) {
    suspend fun getTransactionHistory(): CaffeineResult<PaymentsEnvelope<TransactionHistoryPayload>> =
        paymentsClientService.getTransactionHistory(GetTransactionHistoryBody()).awaitAndParseErrors(gson)
}