package tv.caffeine.app.util

import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.annotation.DimenRes
import androidx.annotation.StyleRes
import androidx.core.view.isVisible
import com.squareup.picasso.Picasso
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import tv.caffeine.app.R
import tv.caffeine.app.api.model.User
import tv.caffeine.app.session.FollowManager
import tv.caffeine.app.ui.FollowButtonDecorator
import tv.caffeine.app.ui.FollowButtonDecorator.Style
import tv.caffeine.app.ui.loadAvatar

class UserTheme(@StyleRes val usernameTextAppearance: Int)

fun User.configure(
        avatarImageView: ImageView,
        usernameTextView: TextView,
        followButton: Button?,
        followManager: FollowManager,
        allowUnfollowing: Boolean = false,
        followHandler: FollowManager.FollowHandler? = null,
        @DimenRes avatarImageSize: Int = R.dimen.avatar_size,
        followedTheme: UserTheme,
        notFollowedTheme: UserTheme,
        picasso: Picasso // TODO (AND-210) Get the Picasso instance from the Context.
) {
    val isFollowing = followManager.isFollowing(caid)
    val theme = if (isFollowing) followedTheme else notFollowedTheme
    if (followButton != null) {
        if (followManager.followersLoaded() && !isFollowing) {
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
        } else if (allowUnfollowing && followManager.followersLoaded() && isFollowing) {
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
    avatarImageView.loadAvatar(avatarImageUrl, isFollowing, avatarImageSize)
    usernameTextView.apply {
        text = username
        setTextAppearance(theme.usernameTextAppearance)
        setCompoundDrawablesRelativeWithIntrinsicBounds(0, 0, if (isVerified) R.drawable.verified_large else 0, 0)
        compoundDrawablePadding = if (isVerified) resources.getDimensionPixelSize(R.dimen.margin_line_spacing_small) else 0
    }
}

