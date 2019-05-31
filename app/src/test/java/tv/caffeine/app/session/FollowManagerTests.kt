package tv.caffeine.app.session

import com.google.gson.Gson
import io.mockk.MockKAnnotations
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.verify
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import tv.caffeine.app.api.BroadcastsService
import tv.caffeine.app.api.UsersService
import tv.caffeine.app.api.model.CaffeineEmptyResult
import tv.caffeine.app.auth.TokenStore

class FollowManagerTests {
    lateinit var followManager: FollowManager

    @MockK(relaxed = true) lateinit var gson: Gson
    @MockK(relaxed = true) lateinit var usersService: UsersService
    @MockK(relaxed = true) lateinit var broadcastsService: BroadcastsService
    @MockK(relaxed = true) lateinit var tokenStore: TokenStore

    @Before
    fun setup() {
        MockKAnnotations.init(this)
        followManager = FollowManager(gson, usersService, broadcastsService, tokenStore)
    }

    @Test
    fun `refreshing followed users requests max page limit`() {
        every { tokenStore.caid } returns "CAID1"
        runBlocking {
            followManager.refreshFollowedUsers()
        }
        verify { usersService.listFollowing("CAID1", 500) }
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
}
