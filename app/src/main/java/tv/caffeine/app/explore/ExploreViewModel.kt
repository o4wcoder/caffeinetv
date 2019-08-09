package tv.caffeine.app.explore

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.map
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import tv.caffeine.app.api.model.CaffeineResult
import tv.caffeine.app.repository.FindBroadcastersRepository
import javax.inject.Inject

class ExploreViewModel @Inject constructor(
    private val findBroadcastersRepository: FindBroadcastersRepository
) : ViewModel() {
    private val _data = MutableLiveData<CaffeineResult<Findings>>()
    val data: LiveData<CaffeineResult<Findings>> = _data.map { it }

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
        exploreJob = viewModelScope.launch {
            val result = findBroadcastersRepository.search(query)
            _data.value = result
        }
    }
}
