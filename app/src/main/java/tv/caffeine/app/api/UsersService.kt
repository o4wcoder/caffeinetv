package tv.caffeine.app.api

import kotlinx.coroutines.Deferred
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query
import tv.caffeine.app.api.model.CAID
import tv.caffeine.app.api.model.CaidRecord
import tv.caffeine.app.api.model.IdentityProvider
import tv.caffeine.app.api.model.PaginatedFollowers
import tv.caffeine.app.api.model.PaginatedFollowing
import tv.caffeine.app.api.model.SignedUserToken
import tv.caffeine.app.api.model.User
import tv.caffeine.app.api.model.UserContainer
import tv.caffeine.app.api.model.UserUpdateBody

const val DEFAULT_PAGE_LIMIT = 500
const val MAX_PAGE_LIMIT = 500

interface UsersService {
    @GET("v2/users/{caid}/followers")
    suspend fun listFollowers(@Path("caid") userId: CAID): PaginatedFollowers

    @GET("v2/users/{caid}/following")
    suspend fun listFollowing(@Path("caid") userId: CAID, @Query("limit") limit: Int = DEFAULT_PAGE_LIMIT): PaginatedFollowing

    @GET("v1/users/{caid}/following")
    suspend fun legacyListFollowing(@Path("caid") userId: CAID): List<CaidRecord.FollowRecord>

    @POST("v1/users/{caid1}/follow/{caid2}")
    fun follow(@Path("caid1") follower: CAID, @Path("caid2") toFollow: CAID): Deferred<Response<Void>>

    @DELETE("v1/users/{caid1}/unfollow/{caid2}")
    fun unfollow(@Path("caid1") follower: CAID, @Path("caid2") toUnfollow: CAID): Deferred<Response<Void>>

    @POST("v1/users/{caid1}/ignore/{caid2}")
    fun ignore(@Path("caid1") ignorer: CAID, @Path("caid2") ignoree: CAID): Deferred<Response<Void>>

    @POST("v1/users/{caid1}/report")
    fun report(@Path("caid1") reportee: CAID, @Body reportUserBody: ReportUserBody): Deferred<Response<Void>>

    @GET("v1/users/suggestions")
    fun listSuggestions(): Deferred<Response<List<SearchUserItem>>>

    @GET("v1/users/{userHandle}")
    fun userDetails(@Path("userHandle") userHandle: String): Deferred<Response<UserContainer>>

    @GET("v1/users/{caid}/signed")
    fun signedUserDetails(@Path("caid") userId: CAID): Deferred<Response<SignedUserToken>>

    @GET("v1/users/{caid}/ignores")
    fun listIgnoredUsers(@Path("caid") userId: CAID): Deferred<Response<List<CaidRecord.IgnoreRecord>>>

    @DELETE("v1/users/{caid}/identities/{socialUid},{identityProvider}")
    fun disconnectIdentity(@Path("caid") userId: CAID, @Path("socialUid") socialUid: String, @Path("identityProvider") identityProvider: IdentityProvider): Deferred<Response<Void>>

    @PATCH("v1/users/{caid}")
    fun updateUser(@Path("caid") userId: CAID, @Body user: UserUpdateBody): Deferred<Response<UserContainer>>

    @PATCH("v1/users/{caid}/notifications-viewed")
    fun notificationsViewed(@Path("caid") userId: CAID): Deferred<Response<UserContainer>>

    @POST("v1/users/list")
    suspend fun multipleUserDetails(@Body batchUserFetchBody: BatchUserFetchBody): List<User>
}

class ReportUserBody(val reason: String, val description: String?)

class BatchUserFetchBody(val identifiers: List<String>)

enum class ReasonKey {
    HARASSMENT_OR_TROLLING,
    INAPPROPRIATE_CONTENT,
    VIOLENCE_OR_SELF_HARM,
    HACKING_OR_CHEATING, // used for reporting a stage only
    SPAM,
    OTHER
}
