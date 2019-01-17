package tv.caffeine.app.api

import kotlinx.coroutines.Deferred
import retrofit2.Response
import retrofit2.http.*
import tv.caffeine.app.api.model.*

interface UsersService {
    @GET("v1/users/{caid}/followers")
    fun listFollowers(@Path("caid") userId: CAID): Deferred<Response<List<CaidRecord.FollowRecord>>>

    @GET("v1/users/{caid}/following")
    fun listFollowing(@Path("caid") userId: CAID): Deferred<Response<List<CaidRecord.FollowRecord>>>

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
