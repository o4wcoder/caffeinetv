package tv.caffeine.app.explore

import androidx.lifecycle.MutableLiveData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import tv.caffeine.app.api.model.CaffeineResult
import tv.caffeine.app.ui.CaffeineViewModel

class ExploreViewModel(
        private val findBroadcastersUseCase: FindBroadcastersUseCase
) : CaffeineViewModel() {
    val data: MutableLiveData<CaffeineResult<Findings>> = MutableLiveData()

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
            withContext(Dispatchers.Main) {
                data.value = result
            }
        }
    }
}
