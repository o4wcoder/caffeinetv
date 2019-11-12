package tv.caffeine.app.users

import android.content.Context
import android.view.View
import androidx.core.content.ContextCompat
import androidx.databinding.Bindable
import androidx.lifecycle.LiveData
import androidx.lifecycle.map
import androidx.lifecycle.viewModelScope
import androidx.paging.DataSource
import androidx.paging.LivePagedListBuilder
import androidx.paging.PagedList
import tv.caffeine.app.api.model.CAID
import tv.caffeine.app.api.model.User
import tv.caffeine.app.ui.CaffeineViewModel
import tv.caffeine.app.util.ThemeColor

abstract class FollowListViewModel(
    val context: Context,
    val pagedUserListService: PagedUserListService
) : CaffeineViewModel() {

    var isEmptyFollowList = false
        set(value) {
            field = value
            notifyChange()
        }

    var isDarkMode = true
        set(value) {
            field = value
            notifyChange()
        }

    var liveData: LiveData<PagedList<User>>? = null
        private set

    val isEmptyState: LiveData<Boolean> = pagedUserListService.isEmptyState.map { it }
    val isRefreshingState: LiveData<Boolean> = pagedUserListService.isRefreshingState.map { it }

    fun init(userId: CAID) {
        val config = PagedList.Config.Builder()
            .setInitialLoadSizeHint(75)
            .setPageSize(25)
            .setEnablePlaceholders(false)
            .build()
        val dataSourceFactory = object : DataSource.Factory<Int, User>() {
            override fun create(): DataSource<Int, User> {
                return PagedDataSource(userId, viewModelScope, pagedUserListService)
            }
        }
        liveData = LivePagedListBuilder<Int, User>(dataSourceFactory, config).build()
    }

    @Bindable
    fun getEmptyMessageVisibility() = pagedUserListService.isEmptyState.map { if (it) View.VISIBLE else View.GONE }

    @Bindable
    fun getEmptyMessageTextColor() = ContextCompat.getColor(
        context, if (isDarkMode) ThemeColor.DARK.color else ThemeColor.LIGHT.color)
}
