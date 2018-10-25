package tv.caffeine.app.api

import kotlinx.coroutines.Deferred
import retrofit2.Call
import retrofit2.Response
import retrofit2.http.*
import tv.caffeine.app.api.model.*

interface UsersService {
    @GET("v1/users/{caid}/followers")
    fun listFollowers(@Path("caid") userId: String): Deferred<List<CaidRecord.FollowRecord>>

    @GET("v1/users/{caid}/following")
    fun listFollowing(@Path("caid") userId: String): Deferred<List<CaidRecord.FollowRecord>>

    @POST("v1/users/{caid1}/follow/{caid2}")
    fun follow(@Path("caid1") follower: String, @Path("caid2") toFollow: String): Call<Void>

    @DELETE("v1/users/{caid1}/unfollow/{caid2}")
    fun unfollow(@Path("caid1") follower: String, @Path("caid2") toUnfollow: String): Call<Void>

    @GET("v1/users/suggestions")
    fun listSuggestions(): Deferred<Response<List<SearchUserItem>>>

    @GET("v1/users/{caid}")
    fun userDetails(@Path("caid") userId: String): Deferred<UserContainer>

    @GET("v1/users/{caid}/signed")
    fun signedUserDetails(@Path("caid") userId: String): Deferred<SignedUserToken>

    @GET("v1/users/{caid}/ignores")
    fun listIgnoredUsers(@Path("caid") userId: String): Deferred<List<CaidRecord.IgnoreRecord>>

    @DELETE("v1/users/{caid}/connected_accounts/{connectedAccount}")
    fun disconnectIdentity(@Path("caid") userId: String, @Path("connectedAccount") identityProvider: IdentityProvider): Deferred<Response<Any>>

    @PATCH("v1/users/{caid}")
    fun updateUser(@Path("caid") userId: String, @Body user: UserUpdateBody): Deferred<Response<Any>>
}
