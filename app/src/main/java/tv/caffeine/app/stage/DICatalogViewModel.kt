package tv.caffeine.app.stage

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Transformations
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.*
import tv.caffeine.app.api.DigitalItemsPayload
import tv.caffeine.app.api.GetDigitalItemsBody
import tv.caffeine.app.api.PaymentsClientService
import kotlin.coroutines.CoroutineContext

class DICatalogViewModel(private val paymentsClientService: PaymentsClientService): ViewModel(), CoroutineScope {
    private val _items = MutableLiveData<DigitalItemsPayload>()
    val items: LiveData<DigitalItemsPayload> = Transformations.map(_items) { it }
    private var job = Job()
    override val coroutineContext: CoroutineContext
        get() = Dispatchers.IO + job

    init {
        load()
    }

    private fun load() {
        launch {
            val deferred = paymentsClientService.getDigitalItems(GetDigitalItemsBody())
            val digitalItems = deferred.await()
            withContext(Dispatchers.Main) {
                _items.value = digitalItems.payload
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        job.cancel()
    }
}
