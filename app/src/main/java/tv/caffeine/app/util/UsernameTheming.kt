package tv.caffeine.app.util

import android.widget.ImageView
import android.widget.TextView
import androidx.annotation.DimenRes
import androidx.annotation.StyleRes
import androidx.core.view.isVisible
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import tv.caffeine.app.R
import tv.caffeine.app.api.model.User
import tv.caffeine.app.session.FollowManager
import tv.caffeine.app.ui.FollowButtonDecorator
import tv.caffeine.app.ui.FollowButtonDecorator.Style
import tv.caffeine.app.ui.configureUserIcon
import tv.caffeine.app.ui.loadAvatar

enum class UsernameTheming(
    val followedTheme: UserTheme,
    val notFollowedTheme: UserTheme,
    val currentUserTheme: UserTheme
) {
    CHAT_USERNAME(UserTheme(R.style.ChatMessageUsername_Following), UserTheme(R.style.ChatMessageUsername_NotFollowing), UserTheme(R.style.ChatMessageUsername_NotFollowing)),
    CHAT_RELEASE_USERNAME(UserTheme(R.style.ChatMessageUsername_Release_Following), UserTheme(R.style.ChatMessageUsername_Release_NotFollowing), UserTheme(R.style.ChatMessageUsername_Release_CurrentUser));

    companion object {
        fun getChatUsernameTheme(isRelease: Boolean) =
            if (isRelease) {
                CHAT_RELEASE_USERNAME
            } else {
                CHAT_USERNAME
            }
    }
}

class UserTheme(@StyleRes val usernameTextAppearance: Int)

fun User.configure(
    avatarImageView: ImageView,
    usernameTextView: TextView,
    followButton: TextView?,
    followManager: FollowManager,
    allowUnfollowing: Boolean = false,
    followHandler: FollowManager.FollowHandler? = null,
    @DimenRes avatarImageSize: Int = R.dimen.avatar_size,
    followedTheme: UserTheme,
    notFollowedTheme: UserTheme,
    currentUserTheme: UserTheme = notFollowedTheme
) {
    val isFollowing = followManager.isFollowing(caid)
    val isSelf = followManager.isSelf(caid)
    val theme = when {
        isSelf -> currentUserTheme
        isFollowing -> followedTheme
        else -> notFollowedTheme
    }

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
        configureUserIcon(when {
            isVerified -> R.drawable.verified
            isCaster -> R.drawable.caster
            else -> 0
        })
        setTextAppearance(theme.usernameTextAppearance)
    }
}
