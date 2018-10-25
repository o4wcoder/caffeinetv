package tv.caffeine.app.explore

import androidx.lifecycle.MutableLiveData
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import tv.caffeine.app.api.SearchQueryBody
import tv.caffeine.app.api.SearchService
import tv.caffeine.app.api.SearchUserItem
import tv.caffeine.app.api.UsersService
import tv.caffeine.app.api.model.CaffeineResult
import tv.caffeine.app.api.model.awaitAndParseErrors
import tv.caffeine.app.ui.CaffeineViewModel

class ExploreViewModel(
        private val searchService: SearchService,
        private val usersService: UsersService,
        private val gson: Gson
) : CaffeineViewModel() {
    val data: MutableLiveData<CaffeineResult<Findings>> = MutableLiveData()

    sealed class Findings(val data: Array<SearchUserItem>) {
        class Explore(data: Array<SearchUserItem>) : Findings(data)
        class Search(data: Array<SearchUserItem>) : Findings(data)
    }

    var queryString: String = ""
        set(value) {
            field = value
            usersMatching(value)
        }

    private var exploreJob: Job? = null

    private fun usersMatching(query: String) {
        exploreJob?.cancel()
        exploreJob = launch {
            val result = if (query.isBlank()) {
                suggestUsers()
            } else {
                findUsers(query)
            }
            withContext(Dispatchers.Main) {
                data.value = result
            }
        }
    }

    private suspend fun suggestUsers(): CaffeineResult<Findings> {
        return runCatching { usersService.listSuggestions().awaitAndParseErrors(gson) }
                .fold({
                    when(it) {
                        is CaffeineResult.Success -> CaffeineResult.Success(Findings.Explore(it.value.toTypedArray()))
                        is CaffeineResult.Error -> CaffeineResult.Error(it.error)
                        is CaffeineResult.Failure -> CaffeineResult.Failure(it.exception)
                    }
                }, {
                    CaffeineResult.Failure(Exception("Unknown"))
                })
    }

    private suspend fun findUsers(query: String): CaffeineResult<Findings> {
        return runCatching { searchService.searchUsers(SearchQueryBody(query)).awaitAndParseErrors(gson) }
                .fold({
                    when(it) {
                        is CaffeineResult.Success -> CaffeineResult.Success(Findings.Search(it.value.results))
                        is CaffeineResult.Error -> CaffeineResult.Error(it.error)
                        is CaffeineResult.Failure -> CaffeineResult.Failure(it.exception)
                    }
                }, {
                    CaffeineResult.Failure(Exception("Unknown"))
                })
    }
}
