package tv.caffeine.app.fpg

import com.google.gson.Gson
import io.mockk.*
import io.mockk.impl.annotations.MockK
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.runBlocking
import okhttp3.ResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import retrofit2.Response
import tv.caffeine.app.api.BroadcastsService
import tv.caffeine.app.api.FeaturedGuideList
import tv.caffeine.app.api.FeaturedGuideListing
import tv.caffeine.app.lobby.LoadFeaturedProgramGuideUseCase
import tv.caffeine.app.session.FollowManager
import java.util.concurrent.TimeUnit

class LoadFeaturedProgramGuideUseCaseTests {

    @MockK lateinit var broadcastsService: BroadcastsService
    @MockK lateinit var featuredGuideResponse: Deferred<Response<FeaturedGuideList>>
    @MockK lateinit var followManager: FollowManager
    private val gson = Gson()

    @Before
    fun setup() {
        MockKAnnotations.init(this)
        every { broadcastsService.featuredGuide() } returns featuredGuideResponse
        coEvery { followManager.refreshFollowedUsers() } returns Unit
    }

    @Test
    fun `featured program guide of a single day`() {
        val dateHeaderCount = 1
        val featuredGuideList = FeaturedGuideList(listOf(
                buildFeaturedGuideListing(0, 0),
                buildFeaturedGuideListing(1, 1)
        ))
        coEvery { featuredGuideResponse.await() } returns Response.success(featuredGuideList)
        val subject = LoadFeaturedProgramGuideUseCase(broadcastsService, followManager, gson)
        val result = runBlocking { subject() }
        assertEquals(featuredGuideList.listings.size + dateHeaderCount, result.size)
        coVerify(exactly = 1) { followManager.refreshFollowedUsers() }
    }

    @Test
    fun `featured program guide of 2 days`() {
        val dateHeaderCount = 2
        val featuredGuideList = FeaturedGuideList(listOf(
                buildFeaturedGuideListing(0, 0),
                buildFeaturedGuideListing(1, TimeUnit.DAYS.toMillis(1)),
                buildFeaturedGuideListing(2, TimeUnit.DAYS.toMillis(1))
        ))
        coEvery { featuredGuideResponse.await() } returns Response.success(featuredGuideList)
        val subject = LoadFeaturedProgramGuideUseCase(broadcastsService, followManager, gson)
        val result = runBlocking { subject() }
        assertEquals(featuredGuideList.listings.size + dateHeaderCount, result.size)
        coVerify(exactly = 1) { followManager.refreshFollowedUsers() }
    }

    @Test
    fun `empty featured program guide on error`() {
        coEvery { featuredGuideResponse.await() } returns Response.error(404, ResponseBody.create(null, "{}"))
        val subject = LoadFeaturedProgramGuideUseCase(broadcastsService, followManager, gson)
        val result = runBlocking { subject() }
        assertTrue(result.isEmpty())
        coVerify(exactly = 0) { followManager.refreshFollowedUsers() }
    }

    @Test
    fun `empty featured program guide on failure`() {
        coEvery { featuredGuideResponse.await() } throws Exception()
        val subject = LoadFeaturedProgramGuideUseCase(broadcastsService, followManager, gson)
        val result = runBlocking { subject() }
        assertTrue(result.isEmpty())
        coVerify(exactly = 0) { followManager.refreshFollowedUsers() }
    }

    private fun buildFeaturedGuideListing(id: Int, startTimestamp: Long): FeaturedGuideListing {
        val endTimestamp = startTimestamp + 1
        return FeaturedGuideListing("$id", "$id", "category $id", "title $id", startTimestamp, endTimestamp, "description $id", null, false)
    }
}

