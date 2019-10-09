package tv.caffeine.app.lobby

import com.google.gson.Gson
import timber.log.Timber
import tv.caffeine.app.api.ContentGuideService
import tv.caffeine.app.api.model.CaffeineResult
import tv.caffeine.app.api.model.awaitAndParseErrors
import tv.caffeine.app.session.FollowManager
import javax.inject.Inject

class LoadFeaturedProgramGuideUseCase @Inject constructor(
    private val contentGuideService: ContentGuideService,
    private val followManager: FollowManager,
    private val gson: Gson
) {
    suspend operator fun invoke(): List<FeaturedGuideItem> {
        val result = contentGuideService.featuredGuide().awaitAndParseErrors(gson)
        return when (result) {
            is CaffeineResult.Success -> {
                // TODO [AND-164] Improve how FollowManager refreshes the followed users list
                followManager.refreshFollowedUsers()
                FeaturedProgramGuideListingBuilder(result.value.listings).build()
            }
            is CaffeineResult.Error -> {
                Timber.e("Failed to fetch content guide ${result.error}")
                listOf()
            }
            is CaffeineResult.Failure -> {
                Timber.e(result.throwable)
                listOf()
            }
        }
    }
}
