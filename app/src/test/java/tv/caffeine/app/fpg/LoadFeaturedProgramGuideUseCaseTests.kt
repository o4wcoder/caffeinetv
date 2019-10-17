package tv.caffeine.app.fpg

import io.mockk.MockKAnnotations
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.impl.annotations.MockK
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import tv.caffeine.app.api.ContentGuideService
import tv.caffeine.app.api.FeaturedGuideList
import tv.caffeine.app.api.FeaturedGuideListing
import tv.caffeine.app.lobby.LoadFeaturedProgramGuideUseCase
import tv.caffeine.app.session.FollowManager
import java.util.concurrent.TimeUnit

class LoadFeaturedProgramGuideUseCaseTests {

    @MockK lateinit var contentGuideService: ContentGuideService
    @MockK lateinit var followManager: FollowManager

    @Before
    fun setup() {
        MockKAnnotations.init(this)
        coEvery { followManager.refreshFollowedUsers() } returns Unit
    }

    @Test
    fun `featured program guide of a single day`() {
        val dateHeaderCount = 1
        val featuredGuideList = FeaturedGuideList(listOf(
                buildFeaturedGuideListing(0, 0),
                buildFeaturedGuideListing(1, 1)
        ))
        coEvery { contentGuideService.featuredGuide() } returns featuredGuideList
        coEvery { followManager.loadMultipleUserDetails(any()) } returns listOf()
        val subject = LoadFeaturedProgramGuideUseCase(contentGuideService, followManager)
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
        coEvery { contentGuideService.featuredGuide() } returns featuredGuideList
        coEvery { followManager.loadMultipleUserDetails(any()) } returns listOf()
        val subject = LoadFeaturedProgramGuideUseCase(contentGuideService, followManager)
        val result = runBlocking { subject() }
        assertEquals(featuredGuideList.listings.size + dateHeaderCount, result.size)
        coVerify(exactly = 1) { followManager.refreshFollowedUsers() }
    }

    @Test
    fun `empty featured program guide on failure`() {
        coEvery { contentGuideService.featuredGuide() } throws Exception()
        val subject = LoadFeaturedProgramGuideUseCase(contentGuideService, followManager)
        val result = runBlocking { subject() }
        assertTrue(result.isEmpty())
        coVerify(exactly = 0) { followManager.refreshFollowedUsers() }
    }

    private fun buildFeaturedGuideListing(id: Int, startTimestamp: Long): FeaturedGuideListing {
        val endTimestamp = startTimestamp + 1
        return FeaturedGuideListing("$id", "$id", "category $id", "title $id", startTimestamp, endTimestamp, "description $id", null, false)
    }
}
