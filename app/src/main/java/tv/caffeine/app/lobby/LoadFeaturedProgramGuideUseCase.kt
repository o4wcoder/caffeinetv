package tv.caffeine.app.lobby

import timber.log.Timber
import tv.caffeine.app.api.ContentGuideService
import tv.caffeine.app.session.FollowManager
import javax.inject.Inject

class LoadFeaturedProgramGuideUseCase @Inject constructor(
    private val contentGuideService: ContentGuideService,
    private val followManager: FollowManager
) {
    suspend operator fun invoke(): List<FeaturedGuideItem> {
        return try {
            val result = contentGuideService.featuredGuide()
            val userIDs = result.listings.map { it.caid }.distinct()
            val broadcasters = followManager.loadMultipleUserDetails(userIDs)
            // TODO [AND-164] Improve how FollowManager refreshes the followed users list
            followManager.refreshFollowedUsers()
            FeaturedProgramGuideListingBuilder(result.listings).build()
        } catch(e: Exception) {
            Timber.e(e)
            listOf()
        }
    }
}
