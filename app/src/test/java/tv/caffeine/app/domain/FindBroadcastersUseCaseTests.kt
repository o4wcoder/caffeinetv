package tv.caffeine.app.domain

import com.google.gson.Gson
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.doReturn
import com.nhaarman.mockitokotlin2.mock
import io.mockk.mockk
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.runBlocking
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import retrofit2.Response
import tv.caffeine.app.api.SearchService
import tv.caffeine.app.api.SearchUserItem
import tv.caffeine.app.api.SearchUsersResult
import tv.caffeine.app.api.UsersService
import tv.caffeine.app.api.model.CaffeineResult
import tv.caffeine.app.explore.FindBroadcastersUseCase
import tv.caffeine.app.explore.Findings

class FindBroadcastersUseCaseTests {
    private lateinit var subject: FindBroadcastersUseCase

    @Before
    fun setup() {
        val user1 = mockk<SearchUserItem>()
        val user2 = mockk<SearchUserItem>()
        val user3 = mockk<SearchUserItem>()
        val searchResultList = SearchUsersResult(arrayOf(user1, user2, user3))
        val mockSearchUsersResponse = mock<Deferred<Response<SearchUsersResult>>> {
            onBlocking { await() } doReturn Response.success(searchResultList)
        }
        val fakeSearchService = mock<SearchService> {
            on { searchUsers(any()) } doReturn mockSearchUsersResponse
        }
        val resultList = listOf(user1, user2)
        val mockListSuggestionsResponse = mock<Deferred<Response<List<SearchUserItem>>>> {
            onBlocking { await() } doReturn Response.success(resultList)
        }
        val fakeUsersService = mock<UsersService> {
            on { listSuggestions() } doReturn mockListSuggestionsResponse
        }
        val gson = Gson()
        subject = FindBroadcastersUseCase(fakeSearchService, fakeUsersService, gson)
    }

    @Test
    fun emptySearchStringReturnsExploreResult() {
        val result = runBlocking { subject(null) }
        when {
            result !is CaffeineResult.Success -> Assert.fail("Was expecting lobby to load")
            result.value !is Findings.Explore -> Assert.fail("Expected explore to be returned")
            result.value is Findings.Explore -> Assert.assertTrue("Expected number of users returned", result.value.data.size == 2)
            else -> Assert.fail("what happened?")
        }
    }

    @Test
    fun nonEmptySearchStringReturnsSearchResults() {
        val result = runBlocking { subject("random") }
        when {
            result !is CaffeineResult.Success -> Assert.fail("Expected the call to succeed")
            result.value !is Findings.Search -> Assert.fail("Expected search to be returned")
            result.value is Findings.Search -> Assert.assertTrue("Expected number of search results", result.value.data.size == 3)
            else -> Assert.fail("what happened?")
        }
    }
}
