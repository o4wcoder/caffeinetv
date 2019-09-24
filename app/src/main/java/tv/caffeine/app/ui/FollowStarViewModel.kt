package tv.caffeine.app.ui

import android.content.Context
import android.view.View
import androidx.core.content.ContextCompat
import androidx.databinding.BaseObservable
import androidx.databinding.Bindable
import tv.caffeine.app.api.model.CAID
import tv.caffeine.app.util.ThemeColor

class FollowStarViewModel(
    val context: Context,
    private val themeColor: ThemeColor,
    val onFollowButtonClick: (caid: CAID, isFollowing: Boolean) -> Unit
) : BaseObservable() {

    private var isSelf = false
    private var isHidden = false
    private lateinit var caid: CAID

    @Bindable
    var isFollowing = false

    @Bindable
    fun getStarVisibility() = if (isSelf || isHidden) View.INVISIBLE else View.VISIBLE

    @Bindable
    fun getTint() = ContextCompat.getColor(context, themeColor.color)

    fun bind(caid: CAID, isFollowing: Boolean, isSelf: Boolean) {
        this.caid = caid
        this.isFollowing = isFollowing
        this.isSelf = isSelf
        isHidden = false
        notifyChange()
    }

    fun onFollowClick() = onFollowButtonClick(caid, isFollowing)

    fun hide() {
        isHidden = true
        notifyChange()
    }
}