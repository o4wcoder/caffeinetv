package tv.caffeine.app.explore

import com.google.gson.Gson
import tv.caffeine.app.api.SearchQueryBody
import tv.caffeine.app.api.SearchService
import tv.caffeine.app.api.UsersService
import tv.caffeine.app.api.model.CaffeineResult
import tv.caffeine.app.api.model.awaitAndParseErrors
import javax.inject.Inject

class FindBroadcastersUseCase @Inject constructor(
        private val searchService: SearchService,
        private val usersService: UsersService,
        private val gson: Gson
) {

    suspend operator fun invoke(searchString: String? = null): CaffeineResult<Findings> =
            if (searchString.isNullOrBlank()) suggestUsers()
            else findUsers(searchString)

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
                    when (it) {
                        is CaffeineResult.Success -> CaffeineResult.Success(Findings.Search(it.value.results))
                        is CaffeineResult.Error -> CaffeineResult.Error(it.error)
                        is CaffeineResult.Failure -> CaffeineResult.Failure(it.exception)
                    }
                }, {
                    CaffeineResult.Failure(Exception("Unknown"))
                })
    }
}
