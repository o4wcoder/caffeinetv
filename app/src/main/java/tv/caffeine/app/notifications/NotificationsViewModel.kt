package tv.caffeine.app.notifications

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import timber.log.Timber
import tv.caffeine.app.api.FollowRecord
import tv.caffeine.app.api.UsersService
import tv.caffeine.app.auth.TokenStore

class NotificationsViewModel(private val usersService: UsersService, private val tokenStore: TokenStore) : ViewModel() {
    val followers: MutableLiveData<List<FollowRecord>> = MutableLiveData()

    fun refresh() {
        val caid = tokenStore.caid ?: return
        usersService.listFollowers(caid).enqueue(object: Callback<List<FollowRecord>?> {
            override fun onFailure(call: Call<List<FollowRecord>?>?, t: Throwable?) {
                Timber.e(t, "Failed to load followers")
            }

            override fun onResponse(call: Call<List<FollowRecord>?>?, response: Response<List<FollowRecord>?>?) {
                Timber.d("Load followers response: $response")
                response?.body()?.let { followers.value = it }
            }
        })
    }
}
