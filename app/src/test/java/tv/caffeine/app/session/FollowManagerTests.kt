package tv.caffeine.app.session

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.google.gson.Gson
import io.mockk.MockKAnnotations
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.impl.annotations.MockK
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.runBlocking
import okhttp3.MediaType
import okhttp3.ResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import retrofit2.Response
import tv.caffeine.app.api.BroadcastsService
import tv.caffeine.app.api.UsersService
import tv.caffeine.app.api.model.CaffeineEmptyResult
import tv.caffeine.app.api.model.isCAID
import tv.caffeine.app.auth.TokenStore
import tv.caffeine.app.util.CoroutinesTestRule
import tv.caffeine.app.util.makeGenericUser

class FollowManagerTests {
    lateinit var followManager: FollowManager

    @get:Rule val instantExecutorRule = InstantTaskExecutorRule()
    @get:Rule val coroutinesTestRule = CoroutinesTestRule()

    @MockK(relaxed = true) lateinit var gson: Gson
    @MockK(relaxed = true) lateinit var usersService: UsersService
    @MockK(relaxed = true) lateinit var broadcastsService: BroadcastsService
    @MockK(relaxed = true) lateinit var tokenStore: TokenStore
    @MockK private lateinit var fakeSuccessResponse: Deferred<Response<Void>>
    @MockK private lateinit var fakeErrorResponse: Deferred<Response<Void>>

    @Before
    fun setup() {
        MockKAnnotations.init(this)
        coEvery { fakeSuccessResponse.await() } returns Response.success(null)
        coEvery { fakeErrorResponse.await() } returns Response.error(404, ResponseBody.create(MediaType.parse("text/json"), "{}"))
        followManager = FollowManager(gson, usersService, broadcastsService, tokenStore)
    }

    @Test
    fun `refreshing followed users uses legacy following endpoint`() {
        every { tokenStore.caid } returns "CAID1"
        runBlocking {
            followManager.refreshFollowedUsers()
        }
        coVerify { usersService.legacyListFollowing("CAID1") }
    }

    @Test
    fun `attempting to follow without current user fails`() {
        every { tokenStore.caid } returns null
        val result = runBlocking {
            followManager.followUser("CAID1")
        }
        val success = when (result) {
            is CaffeineEmptyResult.Failure -> true
            else -> false
        }
        assertTrue("Expected to get failure", success)
    }

    @Test
    fun `is following is correct after follow user success`() {
        val caid = "CAID1"
        coEvery { usersService.follow(any(), any()) } returns fakeSuccessResponse
        runBlocking {
            followManager.followUser(caid)
        }

        assertTrue(followManager.isFollowing(caid))
    }

    @Test
    fun `is following is correct after follow user error`() {
        val caid = "CAID1"
        coEvery { usersService.follow(any(), any()) } returns fakeErrorResponse
        runBlocking {
            followManager.followUser(caid)
        }

        assertFalse(followManager.isFollowing(caid))
    }

    @Test
    fun `is following is correct after unfollow user success`() {
        val caid = "CAID1"

        coEvery { usersService.follow(any(), any()) } returns fakeSuccessResponse
        runBlocking {
            followManager.followUser(caid)
        }
        assertTrue(followManager.isFollowing(caid)) // pre-condition

        coEvery { usersService.unfollow(any(), any()) } returns fakeSuccessResponse
        runBlocking {
            followManager.unfollowUser(caid)
        }

        assertFalse(followManager.isFollowing(caid))
    }

    @Test
    fun `is following is correct after unfollow user error`() {
        val caid = "CAID1"

        coEvery { usersService.follow(any(), any()) } returns fakeSuccessResponse
        runBlocking {
            followManager.followUser(caid)
        }
        assertTrue(followManager.isFollowing(caid)) // pre-condition

        coEvery { usersService.unfollow(any(), any()) } returns fakeErrorResponse
        runBlocking {
            followManager.unfollowUser(caid)
        }

        assertTrue(followManager.isFollowing(caid))
    }

    @Test
    fun `multiple users returns null when users service throws exception`() {
        val userIDs = listOf("CAID001", "CAID002")
        coEvery { usersService.multipleUserDetails(any()) } throws Exception()
        val result = runBlocking {
            followManager.loadMultipleUserDetails(userIDs)
        }
        assertNull(result)
    }

    @Test
    fun `caches user details returned by users service`() {
        val caid1 = "CAID01234567890123456789012345678901"
        val caid2 = "CAID01234567890123456789012345678902"
        assertTrue(caid1.isCAID())
        assertTrue(caid2.isCAID())
        val userIDs = listOf(caid1, caid2)
        val user1 = makeGenericUser(caid1)
        val user2 = makeGenericUser(caid2)
        coEvery { usersService.multipleUserDetails(any()) } returns listOf(user1, user2)
        val result = runBlocking {
            followManager.loadMultipleUserDetails(userIDs)
        }
        assertNotNull(result)
        val user1Details = runBlocking {
            followManager.userDetails(caid1)
        }
        assertEquals(user1, user1Details)
        val user2Details = runBlocking {
            followManager.userDetails(caid2)
        }
        assertEquals(user2, user2Details)
    }
}
