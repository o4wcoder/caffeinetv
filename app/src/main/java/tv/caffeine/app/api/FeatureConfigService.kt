package tv.caffeine.app.api

import kotlinx.coroutines.Deferred
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Path
import tv.caffeine.app.feature.Feature
import tv.caffeine.app.feature.FeatureNode

interface FeatureConfigService {
    @GET("v1/feature-configuration/{feature}")
    fun check(@Path("feature") feature: Feature): Deferred<Response<Map<String, FeatureNode>>>
}

