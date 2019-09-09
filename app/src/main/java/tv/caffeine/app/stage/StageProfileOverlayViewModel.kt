package tv.caffeine.app.stage

import android.content.Context
import androidx.annotation.VisibleForTesting
import androidx.core.content.ContextCompat
import androidx.databinding.BaseObservable
import androidx.databinding.Bindable
import tv.caffeine.app.BR
import tv.caffeine.app.R
import tv.caffeine.app.api.model.CAID

class StageProfileOverlayViewModel(
    val context: Context,
    val onProfileToggleButtonClick: (isProfileShowing: Boolean, caid: CAID) -> Unit
) : BaseObservable() {

    @VisibleForTesting
    var isProfileShowing = true
    private lateinit var caid: CAID

    fun bind(caid: CAID) {
        this.caid = caid
    }

    @Bindable
    fun getTint(): Int {
        val color = if (isProfileShowing) R.color.cyan else R.color.white
        return ContextCompat.getColor(context, color)
    }

    fun onProfileToggleClick() {
        isProfileShowing = !isProfileShowing
        notifyPropertyChanged(BR.tint)
        onProfileToggleButtonClick(isProfileShowing, caid)
    }
}