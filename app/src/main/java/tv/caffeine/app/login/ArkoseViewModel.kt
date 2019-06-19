package tv.caffeine.app.login

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.map
import tv.caffeine.app.api.model.Event
import javax.inject.Inject

class ArkoseViewModel @Inject constructor() : ViewModel() {
    private val _arkoseToken = MutableLiveData<Event<String>>()
    val arkoseToken: LiveData<Event<String>> = _arkoseToken.map { it }

    var signUpBdayApiDate: String? = null

    fun processArkoseTokenResult(token: String) {
        _arkoseToken.value = Event(token)
    }
}