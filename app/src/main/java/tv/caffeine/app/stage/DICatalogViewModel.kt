package tv.caffeine.app.stage

import androidx.lifecycle.LiveData
import androidx.lifecycle.Transformations
import tv.caffeine.app.api.DigitalItemsPayload
import tv.caffeine.app.ui.CaffeineViewModel
import tv.caffeine.app.util.DispatchConfig
import tv.caffeine.app.wallet.DigitalItemRepository
import javax.inject.Inject

class DICatalogViewModel @Inject constructor(
        dispatchConfig: DispatchConfig,
        digitalItemRepository: DigitalItemRepository
): CaffeineViewModel(dispatchConfig) {
    init {
        digitalItemRepository.refresh()
    }

    val items: LiveData<DigitalItemsPayload> = Transformations.map(digitalItemRepository.items) { it }
}
