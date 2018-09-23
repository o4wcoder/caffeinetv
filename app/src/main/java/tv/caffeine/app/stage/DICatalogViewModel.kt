package tv.caffeine.app.stage

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.experimental.Job
import kotlinx.coroutines.experimental.android.UI
import kotlinx.coroutines.experimental.launch
import tv.caffeine.app.api.DigitalItemsPayload
import tv.caffeine.app.api.GetDigitalItemsBody
import tv.caffeine.app.api.PaymentsClientService

class DICatalogViewModel(private val paymentsClientService: PaymentsClientService): ViewModel() {
    val items = MutableLiveData<DigitalItemsPayload>()
    private var job: Job? = null

    fun refresh() {
        job?.cancel()
        job = launch {
            val deferred = paymentsClientService.getDigitalItems(GetDigitalItemsBody())
            val digitalItems = deferred.await()
            launch(UI) {
                items.value = digitalItems.payload
            }
        }
    }
}