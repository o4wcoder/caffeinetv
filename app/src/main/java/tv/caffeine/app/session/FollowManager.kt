package tv.caffeine.app.session

import androidx.fragment.app.FragmentManager
import com.google.gson.Gson
import timber.log.Timber
import tv.caffeine.app.api.BroadcastsService
import tv.caffeine.app.api.UsersService
import tv.caffeine.app.api.model.*
import tv.caffeine.app.auth.TokenStore
import tv.caffeine.app.util.DispatchConfig
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FollowManager @Inject constructor(
        private val dispatchConfig: DispatchConfig,
        private val gson: Gson,
        private val usersService: UsersService,
        private val broadcastsService: BroadcastsService,
        private val tokenStore: TokenStore
) {

    private val followedUsers: MutableMap<CAID, Set<String>> = mutableMapOf()
    private val userDetails: MutableMap<CAID, User> = mutableMapOf()
    private val usernameToCAID: MutableMap<String, CAID> = mutableMapOf()

    fun followers() = tokenStore.caid?.let { followedUsers[it] } ?: setOf()

    fun isFollowing(caidFollower: CAID) = tokenStore.caid?.let { followedUsers[it]?.contains(caidFollower) } ?: false

    fun isSelf(caid: CAID) = tokenStore.caid == caid

    fun currentUserDetails(): User? {
        val caid = tokenStore.caid ?: return null
        return userDetails[caid]
    }

    fun followersLoaded() = tokenStore.caid?.let { followedUsers.containsKey(it) } == true

    suspend fun refreshFollowedUsers() {
        tokenStore.caid?.let { caid ->
            val result = usersService.listFollowing(caid).awaitAndParseErrors(gson)
            when(result) {
                is CaffeineResult.Success -> followedUsers[caid] = result.value.map { it.caid }.toSet()
                is CaffeineResult.Error -> Timber.e("Error loading following list ${result.error}")
                is CaffeineResult.Failure -> Timber.e(result.throwable)
            }
        }
    }

    suspend fun followUser(caid: CAID, callback: FollowCompletedCallback? = null): CaffeineEmptyResult {
        val self = tokenStore.caid ?: return CaffeineEmptyResult.Failure(Exception("Not logged in"))
        val result = usersService.follow(self, caid).awaitEmptyAndParseErrors(gson)
        if (result is CaffeineEmptyResult.Success) {
            followedUsers[self] = (followedUsers[self]?.toMutableSet() ?: mutableSetOf()).apply { add(caid) }.toSet()
            callback?.onUserFollowed()
        }
        refreshFollowedUsers()
        return result
    }

    suspend fun unfollowUser(caid: CAID): CaffeineEmptyResult {
        val self = tokenStore.caid ?: return CaffeineEmptyResult.Failure(Exception("Not logged in"))
        val result = usersService.unfollow(self, caid).awaitEmptyAndParseErrors(gson)
        if (result is CaffeineEmptyResult.Success) {
            followedUsers[self] = followedUsers[self]?.toMutableSet()?.apply { remove(caid) }?.toSet() ?: setOf()
        }
        refreshFollowedUsers()
        return result
    }

    /// can be called with CAID or username
    suspend fun userDetails(userHandle: String): User? {
        val caid = if (userHandle.isCAID()) {
            userHandle
        } else {
           usernameToCAID[userHandle]
        }
        userDetails[caid]?.let { return it }
        return loadUserDetails(userHandle)
    }

    /// can be called with CAID or username
    suspend fun loadUserDetails(userHandle: String): User? {
        val result = usersService.userDetails(userHandle).awaitAndParseErrors(gson)
        when(result) {
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
        return when(result) {
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

