package tv.caffeine.app.social

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Transformations
import tv.caffeine.app.api.OAuthCallbackResult
import tv.caffeine.app.api.model.CaffeineResult
import tv.caffeine.app.api.model.Event
import tv.caffeine.app.ui.CaffeineViewModel
import tv.caffeine.app.util.DispatchConfig
import javax.inject.Inject

class TwitterAuthViewModel @Inject constructor(
    dispatchConfig: DispatchConfig
) : CaffeineViewModel(dispatchConfig) {
    private val _oauthResult = MutableLiveData<Event<CaffeineResult<OAuthCallbackResult>>>()
    val oauthResult: LiveData<Event<CaffeineResult<OAuthCallbackResult>>> = Transformations.map(_oauthResult) { it }

    fun processTwitterOAuthResult(result: CaffeineResult<OAuthCallbackResult>) {
        _oauthResult.value = Event(result)
    }
}
