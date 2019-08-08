package tv.caffeine.app.repository

import com.google.gson.Gson
import tv.caffeine.app.api.SearchQueryBody
import tv.caffeine.app.api.SearchService
import tv.caffeine.app.api.UsersService
import tv.caffeine.app.api.model.CaffeineResult
import tv.caffeine.app.api.model.awaitAndParseErrors
import tv.caffeine.app.explore.Findings
import tv.caffeine.app.settings.ReleaseDesignConfig
import javax.inject.Inject

class FindBroadcastersRepository @Inject constructor(
    private val searchService: SearchService,
    private val usersService: UsersService,
    private val gson: Gson,
    private val releaseDesignConfig: ReleaseDesignConfig
) {

    suspend fun search(searchString: String? = null): CaffeineResult<Findings> =

        if (searchString.isNullOrBlank()) suggestUsers()
        else findUsers(searchString)

    private suspend fun suggestUsers(): CaffeineResult<Findings> {
        val result = usersService.listSuggestions().awaitAndParseErrors(gson)
        return when (result) {
            is CaffeineResult.Success -> {
                if (releaseDesignConfig.isReleaseDesignActive()) {
                    return CaffeineResult.Success(Findings.Search(result.value.toTypedArray()))
                } else {
                    return CaffeineResult.Success(Findings.Explore(result.value.toTypedArray()))
                }
            }
            is CaffeineResult.Error -> CaffeineResult.Error(result.error)
            is CaffeineResult.Failure -> CaffeineResult.Failure(result.throwable)
        }
    }

    private suspend fun findUsers(query: String): CaffeineResult<Findings> {
        val result = searchService.searchUsers(SearchQueryBody(query)).awaitAndParseErrors(gson)
        return when (result) {
            is CaffeineResult.Success -> CaffeineResult.Success(Findings.Search(result.value.results))
            is CaffeineResult.Error -> CaffeineResult.Error(result.error)
            is CaffeineResult.Failure -> CaffeineResult.Failure(result.throwable)
        }
    }
}
