package tv.caffeine.app.social

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.map
import tv.caffeine.app.api.OAuthCallbackResult
import tv.caffeine.app.api.model.CaffeineResult
import tv.caffeine.app.api.model.Event
import javax.inject.Inject

class TwitterAuthViewModel @Inject constructor() : ViewModel() {
    private val _oauthResult = MutableLiveData<Event<CaffeineResult<OAuthCallbackResult>>>()
    val oauthResult: LiveData<Event<CaffeineResult<OAuthCallbackResult>>> = _oauthResult.map { it }

    fun processTwitterOAuthResult(result: CaffeineResult<OAuthCallbackResult>) {
        _oauthResult.value = Event(result)
    }
}
