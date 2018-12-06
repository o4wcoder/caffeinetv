package tv.caffeine.app.wallet

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Transformations
import com.google.gson.Gson
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import timber.log.Timber
import tv.caffeine.app.api.DigitalItemsPayload
import tv.caffeine.app.api.GetDigitalItemsBody
import tv.caffeine.app.api.PaymentsClientService
import tv.caffeine.app.api.PaymentsEnvelope
import tv.caffeine.app.api.model.CaffeineResult
import tv.caffeine.app.api.model.awaitAndParseErrors
import tv.caffeine.app.util.DispatchConfig
import javax.inject.Inject

class DigitalItemRepository @Inject constructor(
        private val dispatchConfig: DispatchConfig,
        private val gson: Gson,
        private val paymentsClientService: PaymentsClientService
) : CoroutineScope {

    private val job = Job()
    override val coroutineContext get() = dispatchConfig.main + job

    private val _items = MutableLiveData<DigitalItemsPayload>()
    val items: LiveData<DigitalItemsPayload> = Transformations.map(_items) { it }

    fun refresh() = launch {
        loadDigitalItems()
    }

    private suspend fun loadDigitalItems() {
        val deferred = paymentsClientService.getDigitalItems(GetDigitalItemsBody())
        val result = runCatching { deferred.awaitAndParseErrors(gson) }.getOrElse { return }
        when(result) {
            is CaffeineResult.Success -> onSuccess(result.value)
            is CaffeineResult.Error -> Timber.e(Exception(result.error.toString()))
            is CaffeineResult.Failure -> Timber.e(result.exception)
        }
    }

    private fun onSuccess(paymentsEnvelope: PaymentsEnvelope<DigitalItemsPayload>) {
        //TODO: handle cursor and retryIn
        _items.value = paymentsEnvelope.payload
    }

    fun stop() {
        job.cancel()
    }

}
