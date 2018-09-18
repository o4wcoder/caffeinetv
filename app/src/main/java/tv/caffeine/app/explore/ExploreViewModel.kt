package tv.caffeine.app.explore

import androidx.lifecycle.MutableLiveData
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import timber.log.Timber
import tv.caffeine.app.api.*
import tv.caffeine.app.util.BaseObservableViewModel

class ExploreViewModel(private val searchService: SearchService, private val usersService: UsersService) : BaseObservableViewModel() {
    val data: MutableLiveData<Result> = MutableLiveData()

    class Result(val state: State, val data: Array<SearchUserItem>)

    enum class State { Explore, Search }

    var queryString: String = ""
        set(value) {
            field = value
            usersMatching(value)
        }

    private fun usersMatching(query: String) {
        if (query.isBlank()) {
            suggestUsers()
        } else {
            findUsers(query)
        }
    }

    private fun suggestUsers() {
        usersService.listSuggestions().enqueue(object: Callback<List<SearchUserItem>?> {
            override fun onFailure(call: Call<List<SearchUserItem>?>?, t: Throwable?) {
                Timber.e(t, "Failed to get list of user suggestions")
            }

            override fun onResponse(call: Call<List<SearchUserItem>?>?, response: Response<List<SearchUserItem>?>?) {
                Timber.d("Received the user suggestions response $response")
                response?.body()?.let {
                    data.value = Result(State.Explore, it.toTypedArray())
                }
            }
        })
    }

    private fun findUsers(query: String) {
        searchService.searchUsers(SearchQueryBody(query)).enqueue(object: Callback<SearchUsersResult?> {
            override fun onFailure(call: Call<SearchUsersResult?>?, t: Throwable?) {
            }

            override fun onResponse(call: Call<SearchUsersResult?>?, response: Response<SearchUsersResult?>?) {
                response?.body()?.let {
                    data.value = Result(State.Search, it.results)
                }
            }
        })
    }
}
