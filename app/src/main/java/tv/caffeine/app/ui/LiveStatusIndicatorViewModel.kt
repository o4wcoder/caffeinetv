package tv.caffeine.app.ui

import android.view.View
import androidx.databinding.BaseObservable
import androidx.databinding.Bindable

class LiveStatusIndicatorViewModel : BaseObservable() {

    var isUserLive = false
        set(value) {
            field = value
            notifyChange()
        }

    @Bindable
    fun getIndicatorVisibility() = if (isUserLive) View.VISIBLE else View.GONE
}