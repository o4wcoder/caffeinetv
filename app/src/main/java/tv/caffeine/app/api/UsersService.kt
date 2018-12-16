package tv.caffeine.app.api

import kotlinx.coroutines.Deferred
import retrofit2.Response
import retrofit2.http.*
import tv.caffeine.app.api.model.*

interface UsersService {
    @GET("v1/users/{caid}/followers")
    fun listFollowers(@Path("caid") userId: String): Deferred<Response<List<CaidRecord.FollowRecord>>>

    @GET("v1/users/{caid}/following")
    fun listFollowing(@Path("caid") userId: String): Deferred<Response<List<CaidRecord.FollowRecord>>>

    @POST("v1/users/{caid1}/follow/{caid2}")
    fun follow(@Path("caid1") follower: String, @Path("caid2") toFollow: String): Deferred<Response<Any>>

    @DELETE("v1/users/{caid1}/unfollow/{caid2}")
    fun unfollow(@Path("caid1") follower: String, @Path("caid2") toUnfollow: String): Deferred<Response<Any>>

    @POST("v1/users/{caid1}/ignore/{caid2}")
    fun ignore(@Path("caid1") ignorer: String, @Path("caid2") ignoree: String): Deferred<Response<Void>>

    @POST("v1/users/{caid1}/report")
    fun report(@Path("caid1") reportee: String, @Body reportUserBody: ReportUserBody): Deferred<Response<Void>>

    @GET("v1/users/suggestions")
    fun listSuggestions(): Deferred<Response<List<SearchUserItem>>>

    @GET("v1/users/{caid}")
    fun userDetails(@Path("caid") userId: String): Deferred<Response<UserContainer>>

    @GET("v1/users/{caid}/signed")
    fun signedUserDetails(@Path("caid") userId: String): Deferred<Response<SignedUserToken>>

    @GET("v1/users/{caid}/ignores")
    fun listIgnoredUsers(@Path("caid") userId: String): Deferred<Response<List<CaidRecord.IgnoreRecord>>>

    @DELETE("v1/users/{caid}/identities/{socialUid},{identityProvider}")
    fun disconnectIdentity(@Path("caid") userId: String, @Path("socialUid") socialUid: String, @Path("identityProvider") identityProvider: IdentityProvider): Deferred<Response<Any>>

    @PATCH("v1/users/{caid}")
    fun updateUser(@Path("caid") userId: String, @Body user: UserUpdateBody): Deferred<Response<UserContainer>>

    @PATCH("v1/users/{caid}/notifications-viewed")
    fun notificationsViewed(@Path("caid") userId: String): Deferred<Response<UserContainer>>
}

class ReportUserBody(val reason: String, val description: String?)

enum class ReasonKey {
    HARASSMENT_OR_TROLLING,
    INAPPROPRIATE_CONTENT,
    VIOLENCE_OR_SELF_HARM,
    HACKING_OR_CHEATING, // used for reporting a stage only
    SPAM,
    OTHER
}
