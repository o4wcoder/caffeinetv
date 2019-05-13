package tv.caffeine.app.session

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.map
import tv.caffeine.app.api.RefreshTokenMissingError
import tv.caffeine.app.api.model.CaffeineResult
import tv.caffeine.app.auth.TokenStore
import javax.inject.Inject

class SessionCheckViewModel @Inject constructor(
    private val tokenStore: TokenStore
) : ViewModel() {

    private val _sessionCheck = MutableLiveData<CaffeineResult<Boolean>>()
    val sessionCheck: LiveData<CaffeineResult<Boolean>> = _sessionCheck.map { it }

    init {
        load()
    }

    private fun load() {
        _sessionCheck.value = when (tokenStore.hasRefreshToken) {
            true -> CaffeineResult.Success(true)
            false -> CaffeineResult.Error(RefreshTokenMissingError())
        }
    }
}
