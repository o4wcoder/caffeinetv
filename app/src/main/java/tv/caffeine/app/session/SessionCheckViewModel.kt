package tv.caffeine.app.session

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Transformations
import tv.caffeine.app.api.RefreshTokenMissingError
import tv.caffeine.app.api.model.CaffeineResult
import tv.caffeine.app.auth.TokenStore
import tv.caffeine.app.ui.CaffeineViewModel
import tv.caffeine.app.util.DispatchConfig
import javax.inject.Inject

class SessionCheckViewModel @Inject constructor(
    dispatchConfig: DispatchConfig,
    private val tokenStore: TokenStore
) : CaffeineViewModel(dispatchConfig) {

    private val _sessionCheck = MutableLiveData<CaffeineResult<Boolean>>()
    val sessionCheck: LiveData<CaffeineResult<Boolean>> = Transformations.map(_sessionCheck) { it }

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
