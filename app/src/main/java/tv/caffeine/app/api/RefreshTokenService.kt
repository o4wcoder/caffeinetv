package tv.caffeine.app.api

import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.POST

interface RefreshTokenService {
    @POST("v1/account/token")
    fun refreshTokenSync(@Body refreshTokenBody: RefreshTokenBody): Call<RefreshTokenResult>
}