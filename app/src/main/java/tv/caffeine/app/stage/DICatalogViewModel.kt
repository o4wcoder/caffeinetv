package tv.caffeine.app.stage

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Transformations
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import tv.caffeine.app.api.DigitalItemsPayload
import tv.caffeine.app.api.GetDigitalItemsBody
import tv.caffeine.app.api.PaymentsClientService
import tv.caffeine.app.ui.CaffeineViewModel
import tv.caffeine.app.util.DispatchConfig

class DICatalogViewModel(
        dispatchConfig: DispatchConfig,
        private val paymentsClientService: PaymentsClientService
): CaffeineViewModel(dispatchConfig) {
    private val _items = MutableLiveData<DigitalItemsPayload>()
    val items: LiveData<DigitalItemsPayload> = Transformations.map(_items) { it }
    private var job = Job()

    init {
        load()
    }

    private fun load() {
        launch {
            val deferred = paymentsClientService.getDigitalItems(GetDigitalItemsBody())
            val digitalItems = deferred.await()
            _items.value = digitalItems.payload
        }
    }

    override fun onCleared() {
        super.onCleared()
        job.cancel()
    }
}
