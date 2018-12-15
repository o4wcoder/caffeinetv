package tv.caffeine.app.api

import kotlinx.coroutines.Deferred
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Headers

interface VersionCheckService {
    @GET("v1/version-check")
    @Headers("No-Authentication: true")
    fun versionCheck(): Deferred<Response<ApiErrorResult>>
}
