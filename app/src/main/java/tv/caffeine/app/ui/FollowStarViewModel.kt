package tv.caffeine.app.ui

import android.view.View
import androidx.databinding.BaseObservable
import androidx.databinding.Bindable
import tv.caffeine.app.api.model.User

class FollowStarViewModel(private val onFollowButtonClick: (user: User, isFollowing: Boolean) -> Unit) :
    BaseObservable() {

    private var isSelf = false
    private lateinit var user: User

    @Bindable
    var isFollowing = false

    @Bindable
    fun getStarVisibility() = if (isSelf) View.INVISIBLE else View.VISIBLE

    fun bind(user: User, isFollowing: Boolean, isSelf: Boolean) {
        this.user = user
        this.isFollowing = isFollowing
        this.isSelf = isSelf
        notifyChange()
    }

    fun onFollowClick() = onFollowButtonClick(user, isFollowing)
}