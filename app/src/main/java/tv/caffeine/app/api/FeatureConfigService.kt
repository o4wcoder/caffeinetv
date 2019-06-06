package tv.caffeine.app.api

import kotlinx.coroutines.Deferred
import retrofit2.Response
import retrofit2.http.GET
import tv.caffeine.app.feature.FeatureNode

interface FeatureConfigService {
    @GET("v1/feature-configuration")
    fun load(): Deferred<Response<Map<String, FeatureNode>>>
}
