package tv.caffeine.app.explore

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Transformations
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import tv.caffeine.app.api.model.CaffeineResult
import tv.caffeine.app.ui.CaffeineViewModel
import tv.caffeine.app.util.DispatchConfig
import javax.inject.Inject

class ExploreViewModel @Inject constructor(
    dispatchConfig: DispatchConfig,
    private val findBroadcastersUseCase: FindBroadcastersUseCase
) : CaffeineViewModel(dispatchConfig) {
    private val _data = MutableLiveData<CaffeineResult<Findings>>()
    val data: LiveData<CaffeineResult<Findings>> = Transformations.map(_data) { it }

    var queryString: String? = null
        set(value) {
            field = value
            usersMatching(value)
        }

    init {
        usersMatching(null)
    }

    private var exploreJob: Job? = null

    private fun usersMatching(query: String?) {
        exploreJob?.cancel()
        exploreJob = launch {
            val result = findBroadcastersUseCase(query)
            _data.value = result
        }
    }
}
