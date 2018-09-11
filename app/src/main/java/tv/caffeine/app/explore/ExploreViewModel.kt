package tv.caffeine.app.explore

import androidx.lifecycle.MutableLiveData
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import tv.caffeine.app.api.SearchQueryBody
import tv.caffeine.app.api.SearchService
import tv.caffeine.app.api.SearchUserItem
import tv.caffeine.app.api.SearchUsersResult
import tv.caffeine.app.util.BaseObservableViewModel

class ExploreViewModel(private val searchService: SearchService) : BaseObservableViewModel() {
    val data: MutableLiveData<Array<SearchUserItem>> = MutableLiveData()

    var queryString: String = ""
        set(value) {
            field = value
            findUsers(value)
        }

    private fun findUsers(query: String) {
        searchService.searchUsers(SearchQueryBody(query)).enqueue(object: Callback<SearchUsersResult?> {
            override fun onFailure(call: Call<SearchUsersResult?>?, t: Throwable?) {
            }

            override fun onResponse(call: Call<SearchUsersResult?>?, response: Response<SearchUsersResult?>?) {
                response?.body()?.let { data.value = it.results }
            }
        })
    }
}
