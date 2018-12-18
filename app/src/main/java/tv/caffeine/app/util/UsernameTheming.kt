package tv.caffeine.app.util

import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.annotation.DimenRes
import androidx.annotation.StyleRes
import androidx.core.view.isVisible
import com.squareup.picasso.Picasso
import com.squareup.picasso.Transformation
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import tv.caffeine.app.R
import tv.caffeine.app.api.model.User
import tv.caffeine.app.session.FollowManager
import tv.caffeine.app.ui.FollowButtonDecorator
import tv.caffeine.app.ui.FollowButtonDecorator.*

class UserTheme(val avatarImageTransformation: Transformation, @StyleRes val usernameTextAppearance: Int)

fun User.configure(
        avatarImageView: ImageView,
        usernameTextView: TextView,
        followButton: Button?,
        followManager: FollowManager,
        allowUnfollowing: Boolean = false,
        followHandler: FollowManager.FollowHandler? = null,
        @DimenRes avatarImageSize: Int = R.dimen.avatar_size,
        followedTheme: UserTheme,
        notFollowedTheme: UserTheme
) {
    val following = followManager.isFollowing(caid)
    val theme = if (following) followedTheme else notFollowedTheme
    val transformation = theme.avatarImageTransformation
    followButton?.let { followButton ->
        if (followManager.followersLoaded() && !following) {
            FollowButtonDecorator(Style.FOLLOW).decorate(followButton)
            followButton.isVisible = true
            followButton.setOnClickListener {
                if (followHandler != null) {
                    followHandler.callback.follow(caid)
                } else {
                    FollowButtonDecorator(Style.FOLLOWING).decorate(followButton)
                    followButton.isVisible = allowUnfollowing
                    GlobalScope.launch {
                        followManager.followUser(caid)
                    }
                }
            }
        } else if (allowUnfollowing && followManager.followersLoaded() && following) {
            FollowButtonDecorator(Style.FOLLOWING).decorate(followButton)
            followButton.isVisible = true
            followButton.setOnClickListener {
                followHandler?.let { handler ->
                    handler.fragmentManager?.navigateToUnfollowUserDialog(caid, username, handler.callback)
                }
            }
        } else {
            followButton.isVisible = false
            followButton.setOnClickListener(null)
        }
    }
    Picasso.get()
            .load(avatarImageUrl)
            .centerCrop()
            .resizeDimen(avatarImageSize, avatarImageSize)
            .placeholder(R.drawable.default_avatar_round)
            .transform(transformation)
            .into(avatarImageView)
    usernameTextView.text = username
    if (isVerified) {
        usernameTextView.setCompoundDrawablesRelativeWithIntrinsicBounds(0, 0, R.drawable.verified_large, 0)
    } else {
        usernameTextView.setCompoundDrawablesRelativeWithIntrinsicBounds(0, 0, 0, 0)
    }
    usernameTextView.setTextAppearance(theme.usernameTextAppearance)
}
