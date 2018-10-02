package tv.caffeine.app.session

import kotlinx.coroutines.*
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import timber.log.Timber
import tv.caffeine.app.api.BroadcastsService
import tv.caffeine.app.api.UsersService
import tv.caffeine.app.api.model.Broadcast
import tv.caffeine.app.api.model.User
import tv.caffeine.app.auth.TokenStore
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FollowManager @Inject constructor(
        private val usersService: UsersService,
        private val broadcastsService: BroadcastsService,
        private val tokenStore: TokenStore
) {

    private val followedUsers: MutableMap<String, Set<String>> = mutableMapOf()
    private val userDetails: MutableMap<String, User> = mutableMapOf()
    private var refreshFollowedUsersJob: Job? = null

    fun followers() = tokenStore.caid?.let { followedUsers[it] } ?: setOf()

    fun isFollowing(caidFollower: String) = tokenStore.caid?.let { followedUsers[it]?.contains(caidFollower) } ?: false

    fun isDefinitelyNotFollowing(caidFollower: String) = tokenStore.caid?.let { followedUsers[it]?.contains(caidFollower) == false } ?: false

    fun followersLoaded() = tokenStore.caid?.let { followedUsers.containsKey(it) } == true

    fun refreshFollowedUsers() {
        refreshFollowedUsersJob?.cancel()
        refreshFollowedUsersJob = GlobalScope.launch(Dispatchers.Default) {
            repeat(5) {
                tokenStore.caid?.let { caid ->
                    val result = usersService.listFollowing(caid).await()
                    launch(Dispatchers.Main) { followedUsers[caid] = (result.map { it.caid }).toSet() }
                    return@launch
                }
                delay(TimeUnit.SECONDS.toMillis(1))
            }
        }
    }

    fun followUser(caid: String) {
        val self = tokenStore.caid ?: return // TODO: report error
        followedUsers[self] = (followedUsers[self]?.toMutableSet() ?: mutableSetOf()).apply { add(caid) }.toSet()
        refreshFollowedUsers()
        usersService.follow(self, caid).enqueue(object: Callback<Void?> {
            override fun onFailure(call: Call<Void?>?, t: Throwable?) {
                Timber.e(t, "Failed to follow user")
            }

            override fun onResponse(call: Call<Void?>?, response: Response<Void?>?) {
                response?.body()?.let {
                }
            }
        })
    }

    fun unfollowUser(caid: String) {
        val self = tokenStore.caid ?: return // TODO: report error
        followedUsers[self] = followedUsers[self]?.toMutableSet()?.apply { remove(caid) }?.toSet() ?: setOf()
        refreshFollowedUsers()
        usersService.unfollow(self, caid).enqueue(object: Callback<Void?> {
            override fun onFailure(call: Call<Void?>?, t: Throwable?) {
                Timber.e(t, "Failed to follow user")
            }

            override fun onResponse(call: Call<Void?>?, response: Response<Void?>?) {
                response?.body()?.let {
                }
            }
        })
    }

    suspend fun userDetails(caid: String): User {
        userDetails[caid]?.let { return it }
        val result = usersService.userDetails(caid)
        val user = result.await().user
        userDetails[caid] = user
        return user
    }

    suspend fun broadcastDetails(user: User): Broadcast {
        return broadcastsService.broadcastDetails(user.broadcastId).await().broadcast
    }
}

