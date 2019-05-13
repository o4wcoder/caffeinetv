package tv.caffeine.app.stage

import androidx.lifecycle.LiveData
import androidx.lifecycle.Transformations
import androidx.lifecycle.ViewModel
import tv.caffeine.app.api.DigitalItemsPayload
import tv.caffeine.app.wallet.DigitalItemRepository
import javax.inject.Inject

class DICatalogViewModel @Inject constructor(
    digitalItemRepository: DigitalItemRepository
) : ViewModel() {
    init {
        digitalItemRepository.refresh()
    }

    val items: LiveData<DigitalItemsPayload> = Transformations.map(digitalItemRepository.items) { it }
}
