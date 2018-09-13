package tv.caffeine.app.session

import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import timber.log.Timber
import tv.caffeine.app.api.Api
import tv.caffeine.app.api.FollowRecord
import tv.caffeine.app.api.UsersService
import tv.caffeine.app.auth.TokenStore
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FollowManager @Inject constructor(private val usersService: UsersService, private val tokenStore: TokenStore) {

    private val followedUsers: MutableMap<String, Set<String>> = mutableMapOf()
    private val userDetails: MutableMap<String, Api.User> = mutableMapOf()

    fun followers() = tokenStore.caid?.let { followedUsers[it] } ?: setOf()

    fun isFollowing(caidFollower: String) = tokenStore.caid?.let { followedUsers[it]?.contains(caidFollower) } ?: false

    fun isDefinitelyNotFollowing(caidFollower: String) = tokenStore.caid?.let { followedUsers[it]?.contains(caidFollower) == false } ?: false

    fun followersLoaded() = tokenStore.caid?.let { followedUsers.containsKey(it) } == true

    fun refreshFollowedUsers(caid: String = tokenStore.caid ?: "") {
        usersService.listFollowing(caid).enqueue(object: Callback<List<FollowRecord>?> {
            override fun onFailure(call: Call<List<FollowRecord>?>?, t: Throwable?) {
                Timber.e(t, "Failed to get the list of followers for $caid")
            }

            override fun onResponse(call: Call<List<FollowRecord>?>?, response: Response<List<FollowRecord>?>?) {
                response?.body()?.let { followedUsers[caid] = it.map { it.caid }.toSet() }
            }
        })
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

    suspend fun userDetails(caid: String): Api.User {
        userDetails[caid]?.let { return it }
        val result = usersService.userDetails(caid)
        val user = result.await().user
        userDetails[caid] = user
        return user
    }
}

