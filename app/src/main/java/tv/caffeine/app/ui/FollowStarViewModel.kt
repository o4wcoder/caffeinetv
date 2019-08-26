package tv.caffeine.app.ui

import android.content.Context
import android.view.View
import androidx.core.content.ContextCompat
import androidx.databinding.BaseObservable
import androidx.databinding.Bindable
import tv.caffeine.app.api.model.User
import tv.caffeine.app.util.FollowStarColor

class FollowStarViewModel(
    val context: Context,
    private val followStarColor: FollowStarColor,
    val onFollowButtonClick: (user: User, isFollowing: Boolean) -> Unit
) : BaseObservable() {

    private var isSelf = false
    private var isHidden = false
    private lateinit var user: User

    @Bindable
    var isFollowing = false

    @Bindable
    fun getStarVisibility() = if (isSelf || isHidden) View.INVISIBLE else View.VISIBLE

    @Bindable
    fun getTint() = ContextCompat.getColor(context, followStarColor.color)

    fun bind(user: User, isFollowing: Boolean, isSelf: Boolean) {
        this.user = user
        this.isFollowing = isFollowing
        this.isSelf = isSelf
        isHidden = false
        notifyChange()
    }

    fun onFollowClick() = onFollowButtonClick(user, isFollowing)

    fun hide() {
        isHidden = true
        notifyChange()
    }
}