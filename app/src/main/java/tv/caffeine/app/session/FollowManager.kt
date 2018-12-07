package tv.caffeine.app.session

import com.google.gson.Gson
import kotlinx.coroutines.withContext
import timber.log.Timber
import tv.caffeine.app.api.BroadcastsService
import tv.caffeine.app.api.UsersService
import tv.caffeine.app.api.model.Broadcast
import tv.caffeine.app.api.model.CaffeineEmptyResult
import tv.caffeine.app.api.model.User
import tv.caffeine.app.api.model.awaitEmptyAndParseErrors
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
            val result = usersService.listFollowing(caid).await()
            withContext(dispatchConfig.main) { followedUsers[caid] = (result.map { it.caid }).toSet() }
        }
    }

    suspend fun followUser(caid: String): CaffeineEmptyResult {
        val self = tokenStore.caid ?: return CaffeineEmptyResult.Failure(Exception("Not logged in"))
        followedUsers[self] = (followedUsers[self]?.toMutableSet() ?: mutableSetOf()).apply { add(caid) }.toSet()
        val result = runCatching {
            usersService.follow(self, caid).awaitEmptyAndParseErrors(gson)
        }.getOrElse { return CaffeineEmptyResult.Failure(Exception("Failed to follow")) }
        refreshFollowedUsers()
        return result
    }

    suspend fun unfollowUser(caid: String): CaffeineEmptyResult {
        val self = tokenStore.caid ?: return CaffeineEmptyResult.Failure(Exception("Not logged in"))
        followedUsers[self] = followedUsers[self]?.toMutableSet()?.apply { remove(caid) }?.toSet() ?: setOf()
        val result = runCatching {
            usersService.unfollow(self, caid).awaitEmptyAndParseErrors(gson)
        }.getOrElse { return CaffeineEmptyResult.Failure(Exception("Failed to unfollow")) }
        refreshFollowedUsers()
        return result
    }

    suspend fun userDetails(caid: String): User? {
        userDetails[caid]?.let { return it }
        return loadUserDetails(caid)
    }

    suspend fun loadUserDetails(caid: String): User? {
        val result = usersService.userDetails(caid)
        try {
            val user = result.await().user
            userDetails[caid] = user
            return user
        } catch (e: Exception) {
            Timber.e(e, "Failed to load user details")
        }
        return null
    }

    suspend fun broadcastDetails(user: User): Broadcast? {
        val broadcastId = user.broadcastId ?: return null
        return broadcastsService.broadcastDetails(broadcastId).await().broadcast
    }
}

