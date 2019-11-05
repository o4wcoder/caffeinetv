package tv.caffeine.app.users

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.map
import androidx.paging.PageKeyedDataSource
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import timber.log.Timber
import tv.caffeine.app.api.BatchUserFetchBody
import tv.caffeine.app.api.UsersService
import tv.caffeine.app.api.model.CAID
import tv.caffeine.app.api.model.User
import javax.inject.Inject

class PagedDataSource<Item>(
    val userId: CAID,
    val coroutineScope: CoroutineScope,
    val pagedService: PagedService<Item>
) : PageKeyedDataSource<Int, Item>() {

    override fun loadInitial(
        params: LoadInitialParams<Int>,
        callback: LoadInitialCallback<Int, Item>
    ) {
        coroutineScope.launch {
            try {
                val response = pagedService.getPage(userId, params.requestedLoadSize, null)
                callback.onResult(response.items, null, response.offset)
            } catch (e: Exception) {
                Timber.e(e)
            }
        }
    }

    override fun loadAfter(
        params: LoadParams<Int>,
        callback: LoadCallback<Int, Item>
    ) {
        coroutineScope.launch {
            try {
                val response = pagedService.getPage(userId, params.requestedLoadSize, params.key)
                callback.onResult(response.items, response.offset)
            } catch (e: Exception) {
                Timber.e(e)
            }
        }
    }

    override fun loadBefore(
        params: LoadParams<Int>,
        callback: LoadCallback<Int, Item>
    ) {
        coroutineScope.launch {
            try {
                val response = pagedService.getPage(userId, params.requestedLoadSize, params.key)
                val previousKey = params.key - params.requestedLoadSize
                callback.onResult(response.items, previousKey)
            } catch (e: Exception) {
                Timber.e(e)
            }
        }
    }
}

interface PagedService<Item> {
    suspend fun getPage(userId: CAID, limit: Int?, offset: Int?): PagedResponse<Item>
    val isRefreshingState: LiveData<Boolean>
    val isEmptyState: LiveData<Boolean>
}

class PagedResponse<Item>(val offset: Int?, val items: List<Item>)

abstract class PagedUserListService(val usersService: UsersService) : PagedService<User> {
    override suspend fun getPage(userId: CAID, limit: Int?, offset: Int?): PagedResponse<User> {
        try {
            val isInitialLoad = (offset == null)
            if (isInitialLoad) {
                _isRefreshingState.value = true
            }
            val userIdListResponse = getUserIdList(userId, limit, offset)
            val userIdList = userIdListResponse.items
            _isEmptyState.value = isInitialLoad && userIdList.isEmpty()
            val rawUserDetailsList =
                usersService.multipleUserDetails(BatchUserFetchBody(userIdList))
            val userDetailsMap = rawUserDetailsList.associateBy { it.caid }
            val userDetailsList = userIdList.mapNotNull { userDetailsMap[it] }
            return PagedResponse(userIdListResponse.offset, userDetailsList)
        } finally {
            _isRefreshingState.value = false
        }
    }

    abstract suspend fun getUserIdList(userId: CAID, limit: Int?, offset: Int?): PagedResponse<CAID>

    private val _isRefreshingState = MutableLiveData<Boolean>(false)
    override val isRefreshingState: LiveData<Boolean> = _isRefreshingState.map { it }

    private val _isEmptyState = MutableLiveData<Boolean>(false)
    override val isEmptyState: LiveData<Boolean> = _isEmptyState.map { it }
}

class PagedFollowersService @Inject constructor(
    usersService: UsersService
) : PagedUserListService(usersService) {
    override suspend fun getUserIdList(userId: CAID, limit: Int?, offset: Int?): PagedResponse<CAID> {
        val paginatedFollowers = usersService.listFollowers(userId, limit, offset)
        val userIdList = paginatedFollowers.followers.map { it.caid }
        return PagedResponse(paginatedFollowers.offset, userIdList)
    }
}

class PagedFollowedUsersService @Inject constructor(
    usersService: UsersService
) : PagedUserListService(usersService) {
    override suspend fun getUserIdList(userId: CAID, limit: Int?, offset: Int?): PagedResponse<CAID> {
        val paginatedFollowing = usersService.listFollowing(userId, limit, offset)
        val userIdList = paginatedFollowing.following.map { it.caid }
        return PagedResponse(paginatedFollowing.offset, userIdList)
    }
}
