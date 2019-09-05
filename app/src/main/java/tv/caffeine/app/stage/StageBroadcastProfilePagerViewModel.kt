package tv.caffeine.app.stage

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.map
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import tv.caffeine.app.profile.UserProfile
import tv.caffeine.app.repository.ProfileRepository
import tv.caffeine.app.ui.CaffeineViewModel
import javax.inject.Inject

class StageBroadcastProfilePagerViewModel @Inject constructor(
    private val profileRepository: ProfileRepository
) : CaffeineViewModel() {

    private val _userProfile = MutableLiveData<UserProfile>()
    val userProfile: LiveData<UserProfile> = _userProfile.map { it }

    fun loadUserProfile(broadcasterUserName: String): LiveData<UserProfile> {
        viewModelScope.launch {
            val result = profileRepository.getUserProfile(broadcasterUserName)
            _userProfile.value = result
        }
        return _userProfile
    }
}