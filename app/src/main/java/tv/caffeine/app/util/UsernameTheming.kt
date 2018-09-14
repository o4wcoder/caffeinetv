package tv.caffeine.app.util

import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.annotation.StyleRes
import androidx.core.view.isVisible
import com.squareup.picasso.Picasso
import com.squareup.picasso.Transformation
import tv.caffeine.app.R
import tv.caffeine.app.api.Api
import tv.caffeine.app.session.FollowManager

class UserTheme(val transformation: Transformation, @StyleRes val textAppearance: Int)

fun Api.User.configure(avatarImageView: ImageView, usernameTextView: TextView, followButton: Button, followManager: FollowManager, followedTheme: UserTheme, notFollowedTheme: UserTheme) {
    val following = followManager.isFollowing(caid)
    val theme = if (following) followedTheme else notFollowedTheme
    val transformation = theme.transformation
    if (followManager.followersLoaded() && !following) {
        followButton.isVisible = true
        followButton.setOnClickListener {
            followButton.isVisible = false
            // TODO: trigger lobby reload?
            followManager.followUser(caid)
        }
    } else {
        followButton.isVisible = false
        followButton.setOnClickListener(null)
    }
    Picasso.get()
            .load(avatarImageUrl)
            .centerCrop()
            .resizeDimen(R.dimen.avatar_size, R.dimen.avatar_size)
            .placeholder(R.drawable.default_avatar_round)
            .transform(transformation)
            .into(avatarImageView)
    usernameTextView.text = username
    if (isVerified) {
        usernameTextView.setCompoundDrawablesRelativeWithIntrinsicBounds(0, 0, R.drawable.verified_large, 0)
    } else {
        usernameTextView.setCompoundDrawablesRelativeWithIntrinsicBounds(0, 0, 0, 0)
    }
    usernameTextView.setTextAppearance(theme.textAppearance)
}