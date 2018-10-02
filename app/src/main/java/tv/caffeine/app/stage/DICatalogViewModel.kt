package tv.caffeine.app.stage

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import tv.caffeine.app.api.DigitalItemsPayload
import tv.caffeine.app.api.GetDigitalItemsBody
import tv.caffeine.app.api.PaymentsClientService

class DICatalogViewModel(private val paymentsClientService: PaymentsClientService): ViewModel() {
    val items = MutableLiveData<DigitalItemsPayload>()
    private var job: Job? = null

    fun refresh() {
        job?.cancel()
        job = GlobalScope.launch(Dispatchers.Default) {
            val deferred = paymentsClientService.getDigitalItems(GetDigitalItemsBody())
            val digitalItems = deferred.await()
            launch(Dispatchers.Main) {
                items.value = digitalItems.payload
            }
        }
    }
}