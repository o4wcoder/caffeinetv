package tv.caffeine.app.session

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import timber.log.Timber
import tv.caffeine.app.api.FollowRecord
import tv.caffeine.app.api.UsersService

class FollowersViewModel(private val caid: String, private val usersService: UsersService) : ViewModel() {
    private val followers: MutableLiveData<List<String>> = MutableLiveData()
    private var list: List<String>? = null

    fun getFollowers(): LiveData<List<String>> {
        if (list == null) loadFollowers()
        return followers
    }

    private fun loadFollowers() {
        usersService.listFollowers(caid).enqueue(object: Callback<List<FollowRecord>?> {
            override fun onFailure(call: Call<List<FollowRecord>?>?, t: Throwable?) {
                Timber.e(t, "Couldn't load followers for $caid")
            }

            override fun onResponse(call: Call<List<FollowRecord>?>?, response: Response<List<FollowRecord>?>?) {
                response?.body()?.map { it.caid }?.let { followers.postValue(it) }
            }
        })
    }
}