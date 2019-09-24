package tv.caffeine.app.users

import android.content.Context
import android.view.View
import androidx.core.content.ContextCompat
import androidx.databinding.Bindable
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.map
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import tv.caffeine.app.api.model.CAID
import tv.caffeine.app.api.model.CaidRecord
import tv.caffeine.app.profile.UserProfile
import tv.caffeine.app.repository.ProfileRepository
import tv.caffeine.app.ui.CaffeineViewModel
import tv.caffeine.app.util.ThemeColor

abstract class FollowListViewModel(
    val context: Context,
    private val profileRepository: ProfileRepository
) : CaffeineViewModel() {

    private val _followList = MutableLiveData<List<CaidRecord.FollowRecord>>()
    val followList: LiveData<List<CaidRecord.FollowRecord>> = _followList.map { it }
    private val _userProfile = MutableLiveData<UserProfile>()
    val userProfile: LiveData<UserProfile> = _userProfile.map { it }

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

    var caid: CAID = ""
        set(value) {
            field = value
            loadFollowList()
        }

    abstract fun loadFollowList()

    @Bindable
    fun getEmptyMessageVisibility() = if (isEmptyFollowList) View.VISIBLE else View.GONE

    @Bindable
    fun getEmptyMessageTextColor() = ContextCompat.getColor(
        context, if (isDarkMode) ThemeColor.DARK.color else ThemeColor.LIGHT.color)

    fun loadUserProfile(caid: CAID): LiveData<UserProfile> {
        viewModelScope.launch {
            val result = profileRepository.getUserProfile(caid)
            _userProfile.value = result
            notifyChange()
        }
        return _userProfile
    }

    fun setFollowList(followList: List<CaidRecord.FollowRecord>) {
        _followList.value = followList
    }
}