package tv.caffeine.app.api

import retrofit2.Call
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path

interface UsersService {
    @GET("v1/users/{caid}/followers")
    fun listFollowers(@Path("caid") userId: String): Call<List<FollowRecord>>

    @GET("v1/users/{caid}/following")
    fun listFollowing(@Path("caid") userId: String): Call<List<FollowRecord>>

    @POST("v1/users/{caid1}/follow/{caid2}")
    fun follow(@Path("caid1") follower: String, @Path("caid2") toFollow: String): Call<Void>

    @DELETE("v1/users/{caid1}/unfollow/{caid2}")
    fun unfollow(@Path("caid1") follower: String, @Path("caid2") toUnfollow: String): Call<Void>
}

class FollowRecord(val caid: String, val followedAt: String) // followedAt = ISO-8601 date
