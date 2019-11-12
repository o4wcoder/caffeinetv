package tv.caffeine.app.session

import androidx.fragment.app.FragmentManager
import androidx.lifecycle.MutableLiveData
import com.google.gson.Gson
import timber.log.Timber
import tv.caffeine.app.api.BatchUserFetchBody
import tv.caffeine.app.api.BroadcastsService
import tv.caffeine.app.api.UsersService
import tv.caffeine.app.api.model.Broadcast
import tv.caffeine.app.api.model.CAID
import tv.caffeine.app.api.model.CaffeineEmptyResult
import tv.caffeine.app.api.model.CaffeineResult
import tv.caffeine.app.api.model.Event
import tv.caffeine.app.api.model.User
import tv.caffeine.app.api.model.UserContainer
import tv.caffeine.app.api.model.UserUpdateBody
import tv.caffeine.app.api.model.UserUpdateDetails
import tv.caffeine.app.api.model.awaitAndParseErrors
import tv.caffeine.app.api.model.awaitEmptyAndParseErrors
import tv.caffeine.app.api.model.isCAID
import tv.caffeine.app.auth.TokenStore
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FollowManager @Inject constructor(
    private val gson: Gson,
    private val usersService: UsersService,
    private val broadcastsService: BroadcastsService,
    private val tokenStore: TokenStore
) {

    private val followedUsers: MutableMap<CAID, Set<String>> = mutableMapOf()
    private val userDetails: MutableMap<CAID, User> = mutableMapOf()
    private val usernameToCAID: MutableMap<String, CAID> = mutableMapOf()

    val followResult: MutableLiveData<Event<CaffeineEmptyResult>> = MutableLiveData()

    fun followers() = tokenStore.caid?.let { followedUsers[it] } ?: setOf()

    fun isFollowing(caidFollower: CAID) = tokenStore.caid?.let { followedUsers[it]?.contains(caidFollower) } ?: false

    fun isSelf(caid: CAID) = tokenStore.caid == caid

    fun currentUserDetails(): User? {
        val caid = tokenStore.caid ?: return null
        return userDetails[caid]
    }

    fun followersLoaded() = tokenStore.caid?.let { followedUsers.containsKey(it) } == true

    suspend fun refreshFollowedUsers() {
        val caid = tokenStore.caid ?: return
        try {
            val result = usersService.legacyListFollowing(caid)
            followedUsers[caid] = result.map { it.caid }.toSet()
        } catch (e: Exception) {
            Timber.e(e)
        }
    }

    suspend fun followUser(caid: CAID, callback: FollowCompletedCallback? = null): CaffeineEmptyResult {
        val self = tokenStore.caid ?: return CaffeineEmptyResult.Failure(Exception("Not logged in"))
        followedUsers[self] = (followedUsers[self]?.toMutableSet() ?: mutableSetOf()).apply { add(caid) }.toSet()
        val result = usersService.follow(self, caid).awaitEmptyAndParseErrors(gson)
        when (result) {
            is CaffeineEmptyResult.Success -> callback?.onUserFollowed()
            else -> {
                followedUsers[self] = followedUsers[self]?.toMutableSet()?.apply { remove(caid) }?.toSet() ?: setOf()
                callback?.onUserFollowed()
            }
        }
        followResult.value = Event(result)
        refreshFollowedUsers()
        return result
    }

    suspend fun unfollowUser(caid: CAID): CaffeineEmptyResult {
        val self = tokenStore.caid ?: return CaffeineEmptyResult.Failure(Exception("Not logged in"))
        followedUsers[self] = followedUsers[self]?.toMutableSet()?.apply { remove(caid) }?.toSet() ?: setOf()
        val result = usersService.unfollow(self, caid).awaitEmptyAndParseErrors(gson)
        when (result) {
            is CaffeineEmptyResult.Error, is CaffeineEmptyResult.Failure -> {
                followedUsers[self] = (followedUsers[self]?.toMutableSet() ?: mutableSetOf()).apply { add(caid) }.toSet()
            }
        }
        refreshFollowedUsers()
        return result
    }

    // / can be called with CAID or username
    suspend fun userDetails(userHandle: String): User? {
        val caid = if (userHandle.isCAID()) {
            userHandle
        } else {
            usernameToCAID[userHandle]
        }
        userDetails[caid]?.let { return it }
        return loadUserDetails(userHandle)
    }

    suspend fun loadMultipleUserDetails(userIDs: List<CAID>): List<User>? {
        return try {
            val users = usersService.multipleUserDetails(BatchUserFetchBody(userIDs))
            users.forEach {
                userDetails[it.caid] = it
            }
            users
        } catch (e: Exception) {
            Timber.e(e)
            null
        }
    }

    // / can be called with CAID or username
    suspend fun loadUserDetails(userHandle: String): User? {
        val result = usersService.userDetails(userHandle).awaitAndParseErrors(gson)
        when (result) {
            is CaffeineResult.Success -> {
                val user = result.value.user
                userDetails[user.caid] = user
                if (!userHandle.isCAID()) {
                    usernameToCAID[userHandle] = user.caid
                }
                return user
            }
            is CaffeineResult.Error -> Timber.e(result.error.toString())
            is CaffeineResult.Failure -> Timber.e(result.throwable)
        }
        return null
    }

    suspend fun loadMyUserDetails(): User? {
        return tokenStore.caid?.let { loadUserDetails(it) }
    }

    suspend fun updateUser(caid: CAID, name: String? = null, bio: String? = null): CaffeineResult<UserContainer> {
        val userUpdateBody = UserUpdateBody(UserUpdateDetails(name, bio, null))
        val result = usersService.updateUser(caid, userUpdateBody).awaitAndParseErrors(gson)
        if (result is CaffeineResult.Success) {
            userDetails[caid] = result.value.user
        }
        return result
    }

    suspend fun broadcastDetails(user: User): Broadcast? {
        val broadcastId = user.broadcastId ?: return null
        val result = broadcastsService.broadcastDetails(broadcastId).awaitAndParseErrors(gson)
        return when (result) {
            is CaffeineResult.Success -> result.value.broadcast
            is CaffeineResult.Error -> Timber.e("Failure loading broadcast details ${result.error}").let { null }
            is CaffeineResult.Failure -> Timber.e(result.throwable).let { null }
        }
    }

    class FollowHandler(val fragmentManager: FragmentManager?, val callback: Callback)
    abstract class Callback {
        abstract fun follow(caid: CAID)
        abstract fun unfollow(caid: CAID)
    }

    interface FollowCompletedCallback {
        fun onUserFollowed()
    }
}
