package tv.caffeine.app.util

import android.content.DialogInterface
import android.os.Bundle
import androidx.annotation.IdRes
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.navigation.NavController
import androidx.navigation.NavDirections
import androidx.navigation.NavOptions
import androidx.navigation.Navigator
import com.squareup.picasso.Picasso
import timber.log.Timber
import tv.caffeine.app.MainNavDirections
import tv.caffeine.app.R
import tv.caffeine.app.api.model.CAID
import tv.caffeine.app.lobby.SendingVerificationEmailDialogFragment
import tv.caffeine.app.profile.UnfollowUserDialogFragment
import tv.caffeine.app.session.FollowManager
import tv.caffeine.app.settings.ReleaseDesignConfig
import tv.caffeine.app.stage.DICatalogFragment
import tv.caffeine.app.stage.SendMessageFragment
import tv.caffeine.app.stage.StagePagerFragmentDirections

private const val SEND_MESSAGE = 1
private const val PICK_DIGITAL_ITEM = 0
fun NavController.safeNavigate(directions: NavDirections) {
    try {
        navigate(directions)
    } catch (e: IllegalArgumentException) {
        Timber.d(e, "More than one navigation are triggered at the same time")
    }
}

fun NavController.safeNavigate(directions: NavDirections, navOptions: NavOptions?) {
    try {
        navigate(directions, navOptions)
    } catch (e: IllegalArgumentException) {
        Timber.d(e, "More than one navigation are triggered at the same time")
    }
}

fun NavController.safeNavigate(directions: NavDirections, navigatorExtras: Navigator.Extras) {
    try {
        navigate(directions, navigatorExtras)
    } catch (e: IllegalArgumentException) {
        Timber.d(e, "More than one navigation are triggered at the same time")
    }
}

// TODO: Check if we can use NavDirections instead of resId.
fun NavController.safeNavigate(@IdRes resId: Int) {
    try {
        navigate(resId)
    } catch (e: IllegalArgumentException) {
        Timber.d(e, "More than one navigation are triggered at the same time")
    }
}

fun NavController.safeNavigate(@IdRes resId: Int, args: Bundle?) {
    try {
        navigate(resId, args)
    } catch (e: IllegalArgumentException) {
        Timber.d(e, "More than one navigation are triggered at the same time")
    }
}

fun NavController.safeNavigate(@IdRes resId: Int, args: Bundle?, navOptions: NavOptions?) {
    try {
        navigate(resId, args, navOptions)
    } catch (e: IllegalArgumentException) {
        Timber.d(e, "More than one navigation are triggered at the same time")
    }
}

fun NavController.safeNavigate(@IdRes resId: Int, args: Bundle?, navOptions: NavOptions?, navigatorExtras: Navigator.Extras?) {
    try {
        navigate(resId, args, navOptions, navigatorExtras)
    } catch (e: IllegalArgumentException) {
        Timber.d(e, "More than one navigation are triggered at the same time")
    }
}

fun NavController.navigateToLanding(message: String? = null) {
    val action = MainNavDirections.actionGlobalLandingFragment(message)
    val navOptions = NavOptions.Builder().setPopUpTo(R.id.lobbySwipeFragment, true).build()
    safeNavigate(action, navOptions)
}

fun NavController.navigateToNeedsUpdate() {
    safeNavigate(MainNavDirections.actionGlobalNeedsUpdateFragment())
}

fun NavController.navigateToNoNetwork() {
    if (currentDestination?.id == R.id.noNetworkFragment) return
    val action = MainNavDirections.actionGlobalNoNetworkFragment()
    safeNavigate(action)
}

fun NavController.closeNoNetwork() {
    if (currentDestination?.id == R.id.noNetworkFragment) popBackStack()
}

fun NavController.navigateToReportOrIgnoreDialog(caid: CAID, username: String, shouldNavigateBackWhenDone: Boolean) {
    val action = MainNavDirections.actionGlobalReportOrIgnoreDialogFragment(
            caid, username, shouldNavigateBackWhenDone)
    safeNavigate(action)
}

fun FragmentManager.navigateToUnfollowUserDialog(caid: CAID, username: String, callback: FollowManager.Callback) {
    UnfollowUserDialogFragment().apply {
        positiveClickListener = DialogInterface.OnClickListener { _, _ -> callback.unfollow(caid) }
        arguments = MainNavDirections.actionGlobalUnfollowUserDialogFragment(username).arguments
        show(this@navigateToUnfollowUserDialog, "unfollowUser")
    }
}

fun FragmentManager.navigateToSendingVerificationEmailDialog(email: String) {
    SendingVerificationEmailDialogFragment().apply {
        arguments = MainNavDirections.actionGlobalSendingVerificationEmailDialogFragment(email).arguments
        show(this@navigateToSendingVerificationEmailDialog, "verificationEmailSent")
    }
}

fun FragmentManager.navigateToSendMessage(callingFragment: Fragment, isMe: Boolean, message: String? = null) {
    SendMessageFragment().apply {
        val action = StagePagerFragmentDirections.actionStagePagerFragmentToSendMessageFragment(message, !isMe)
        arguments = action.arguments
        setTargetFragment(callingFragment, SEND_MESSAGE)
        show(this@navigateToSendMessage, "sendMessage")
    }
}

fun FragmentManager.navigateToDigitalItemWithMessage(
    callingFragment: Fragment,
    picasso: Picasso,
    broadcasterUsername: String,
    message: String? = null,
    releaseDesignConfig: ReleaseDesignConfig
) {
    DICatalogFragment(picasso, releaseDesignConfig).apply {
        val action = StagePagerFragmentDirections.actionStagePagerFragmentToDigitalItemListDialogFragment(
            broadcasterUsername,
            message
        )
        arguments = action.arguments
        setTargetFragment(callingFragment, PICK_DIGITAL_ITEM)
        show(this@navigateToDigitalItemWithMessage, "digitalItem")
    }
}
