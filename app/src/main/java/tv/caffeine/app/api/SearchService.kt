package tv.caffeine.app.api

import com.google.gson.annotations.SerializedName
import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.POST
import tv.caffeine.app.api.model.User

interface SearchService {
    @POST("v1/search/users")
    fun searchUsers(@Body searchQueryBody: SearchQueryBody): Call<SearchUsersResult>
}

class SearchQueryBody(@SerializedName("q") val query: String)
class SearchUsersResult(val results: Array<SearchUserItem>)
class SearchUserItem(val user: User, val score: Float, val id: String)
