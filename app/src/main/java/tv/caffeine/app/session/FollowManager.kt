package tv.caffeine.app.session

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

    private val followedUsers: MutableMap<String, Set<String>> = mutableMapOf()
    private val userDetails: MutableMap<String, User> = mutableMapOf()

    fun followers() = tokenStore.caid?.let { followedUsers[it] } ?: setOf()

    fun isFollowing(caidFollower: String) = tokenStore.caid?.let { followedUsers[it]?.contains(caidFollower) } ?: false

    fun isDefinitelyNotFollowing(caidFollower: String) = tokenStore.caid?.let { followedUsers[it]?.contains(caidFollower) == false } ?: false

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

    suspend fun followUser(caid: String): CaffeineEmptyResult {
        val self = tokenStore.caid ?: return CaffeineEmptyResult.Failure(Exception("Not logged in"))
        followedUsers[self] = (followedUsers[self]?.toMutableSet() ?: mutableSetOf()).apply { add(caid) }.toSet()
        val result = usersService.follow(self, caid).awaitEmptyAndParseErrors(gson)
        refreshFollowedUsers()
        return result
    }

    suspend fun unfollowUser(caid: String): CaffeineEmptyResult {
        val self = tokenStore.caid ?: return CaffeineEmptyResult.Failure(Exception("Not logged in"))
        followedUsers[self] = followedUsers[self]?.toMutableSet()?.apply { remove(caid) }?.toSet() ?: setOf()
        val result = usersService.unfollow(self, caid).awaitEmptyAndParseErrors(gson)
        refreshFollowedUsers()
        return result
    }

    suspend fun userDetails(caid: String): User? {
        userDetails[caid]?.let { return it }
        return loadUserDetails(caid)
    }

    suspend fun loadUserDetails(caid: String): User? {
        val result = usersService.userDetails(caid).awaitAndParseErrors(gson)
        when(result) {
            is CaffeineResult.Success -> userDetails[caid] = result.value.user
            is CaffeineResult.Error -> Timber.e(result.error.toString())
            is CaffeineResult.Failure -> Timber.e(result.throwable)
        }
        return userDetails[caid]
    }

    suspend fun updateUser(caid: String, name: String? = null, bio: String? = null): CaffeineResult<UserContainer> {
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
}

