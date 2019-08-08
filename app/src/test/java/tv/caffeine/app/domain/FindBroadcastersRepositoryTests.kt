package tv.caffeine.app.domain

import com.google.gson.Gson
import io.mockk.MockKAnnotations
import io.mockk.coEvery
import io.mockk.every
import io.mockk.impl.annotations.MockK
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
import tv.caffeine.app.repository.FindBroadcastersRepository
import tv.caffeine.app.explore.Findings

class FindBroadcastersRepositoryTests {
    @MockK lateinit var user1: SearchUserItem
    @MockK lateinit var user2: SearchUserItem
    @MockK lateinit var user3: SearchUserItem
    @MockK lateinit var mockSearchUsersResponse: Deferred<Response<SearchUsersResult>>
    @MockK lateinit var fakeSearchService: SearchService
    @MockK lateinit var mockListSuggestionsResponse: Deferred<Response<List<SearchUserItem>>>
    @MockK lateinit var fakeUsersService: UsersService
    private lateinit var subject: FindBroadcastersRepository

    @Before
    fun setup() {
        MockKAnnotations.init(this)
        val searchResultList = SearchUsersResult(arrayOf(user1, user2, user3))
        coEvery { mockSearchUsersResponse.await() } returns Response.success(searchResultList)
        coEvery { fakeSearchService.searchUsers(any()) } returns mockSearchUsersResponse
        val resultList = listOf(user1, user2)
        coEvery { mockListSuggestionsResponse.await() } returns Response.success(resultList)
        every { fakeUsersService.listSuggestions() } returns mockListSuggestionsResponse
        val gson = Gson()
        subject = FindBroadcastersRepository(fakeSearchService, fakeUsersService, gson, mockk(relaxed = true))
    }

    @Test
    fun emptySearchStringReturnsExploreResult() {
        val result = runBlocking { subject.search(null) }
        when {
            result !is CaffeineResult.Success -> Assert.fail("Was expecting lobby to load")
            result.value !is Findings.Explore -> Assert.fail("Expected explore to be returned")
            result.value is Findings.Explore -> Assert.assertTrue("Expected number of users returned", result.value.data.size == 2)
            else -> Assert.fail("what happened?")
        }
    }

    @Test
    fun nonEmptySearchStringReturnsSearchResults() {
        val result = runBlocking { subject.search("random") }
        when {
            result !is CaffeineResult.Success -> Assert.fail("Expected the call to succeed")
            result.value !is Findings.Search -> Assert.fail("Expected search to be returned")
            result.value is Findings.Search -> Assert.assertTrue("Expected number of search results", result.value.data.size == 3)
            else -> Assert.fail("what happened?")
        }
    }
}
