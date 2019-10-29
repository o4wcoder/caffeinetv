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
    STANDARD(UserTheme(R.style.ExploreUsername_Following), UserTheme(R.style.ExploreUsername_NotFollowing), UserTheme(R.style.ExploreUsername_NotFollowing)),
    STANDARD_DARK(UserTheme(R.style.ExploreUsername_Following), UserTheme(R.style.ExploreUsername_NotFollowingDark), UserTheme(R.style.ExploreUsername_NotFollowingDark)),
    CHAT(UserTheme(R.style.ChatMessageUsername_Following), UserTheme(R.style.ChatMessageUsername_NotFollowing), UserTheme(R.style.ChatMessageUsername_NotFollowing)),
    CHAT_RELEASE(UserTheme(R.style.ChatMessageUsername_Release_Following), UserTheme(R.style.ChatMessageUsername_Release_NotFollowing), UserTheme(R.style.ChatMessageUsername_Release_CurrentUser)),
    LOBBY(UserTheme(R.style.BroadcastCardUsername_Following), UserTheme(R.style.BroadcastCardUsername_NotFollowing), UserTheme(R.style.BroadcastCardUsername_NotFollowing)),
    LOBBY_LIGHT(UserTheme(R.style.BroadcastCardUsername_Following_Previous), UserTheme(R.style.BroadcastCardUsername_NotFollowing_Previous), UserTheme(R.style.BroadcastCardUsername_NotFollowing_Previous));

    companion object {
        fun getChatTheme(isRelease: Boolean) =
            if (isRelease) {
                CHAT_RELEASE
            } else {
                CHAT
            }

        fun getLobbyTheme(isLight: Boolean) =
            if (isLight) {
                LOBBY_LIGHT
            } else {
                LOBBY
            }
    }
}

enum class ThemeColor(val color: Int) {
    DARK(R.color.white),
    LIGHT(R.color.black)
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
    theme: UsernameTheming
) {
    val isFollowing = followManager.isFollowing(caid)
    val isSelf = followManager.isSelf(caid)
    val theme = when {
        isSelf -> theme.currentUserTheme
        isFollowing -> theme.followedTheme
        else -> theme.notFollowedTheme
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

fun User.configureUsernameAndAvatar(
    avatarImageView: ImageView,
    usernameTextView: TextView
) {

    avatarImageView.loadAvatar(avatarImageUrl, false, R.dimen.avatar_size)
    usernameTextView.apply {
        text = username
        configureUserIcon(
            when {
                isVerified -> R.drawable.verified
                isCaster -> R.drawable.caster
                else -> 0
            }
        )
    }
}
