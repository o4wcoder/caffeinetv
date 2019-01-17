package tv.caffeine.app.util

import android.content.DialogInterface
import android.os.Bundle
import androidx.annotation.IdRes
import androidx.fragment.app.FragmentManager
import androidx.navigation.NavController
import androidx.navigation.NavDirections
import androidx.navigation.NavOptions
import androidx.navigation.Navigator
import timber.log.Timber
import tv.caffeine.app.MainNavDirections
import tv.caffeine.app.R
import tv.caffeine.app.api.model.CAID
import tv.caffeine.app.profile.ReportOrIgnoreDialogFragment
import tv.caffeine.app.profile.UnfollowUserDialogFragment
import tv.caffeine.app.session.FollowManager

fun NavController.safeNavigate(directions: NavDirections) {
    try {
        navigate(directions)
    } catch(e: IllegalArgumentException) {
        Timber.d(e, "More than one navigation are triggered at the same time")
    }
}

fun NavController.safeNavigate(directions: NavDirections, navOptions: NavOptions?) {
    try {
        navigate(directions, navOptions)
    } catch(e: IllegalArgumentException) {
        Timber.d(e, "More than one navigation are triggered at the same time")
    }
}

fun NavController.safeNavigate(directions: NavDirections, navigatorExtras: Navigator.Extras) {
    try {
        navigate(directions, navigatorExtras)
    } catch(e: IllegalArgumentException) {
        Timber.d(e, "More than one navigation are triggered at the same time")
    }
}

// TODO: Check if we can use NavDirections instead of resId.
fun NavController.safeNavigate(@IdRes resId: Int) {
    try {
        navigate(resId)
    } catch(e: IllegalArgumentException) {
        Timber.d(e, "More than one navigation are triggered at the same time")
    }
}

fun NavController.safeNavigate(@IdRes resId: Int, args: Bundle?) {
    try {
        navigate(resId, args)
    } catch(e: IllegalArgumentException) {
        Timber.d(e, "More than one navigation are triggered at the same time")
    }
}

fun NavController.safeNavigate(@IdRes resId: Int, args: Bundle?, navOptions: NavOptions?) {
    try {
        navigate(resId, args, navOptions)
    } catch(e: IllegalArgumentException) {
        Timber.d(e, "More than one navigation are triggered at the same time")
    }
}

fun NavController.safeNavigate(@IdRes resId: Int, args: Bundle?, navOptions: NavOptions?, navigatorExtras: Navigator.Extras?) {
    try {
        navigate(resId, args, navOptions, navigatorExtras)
    } catch(e: IllegalArgumentException) {
        Timber.d(e, "More than one navigation are triggered at the same time")
    }
}

fun NavController.navigateToLanding(message: String? = null) {
    val action = MainNavDirections.ActionGlobalLandingFragment(message)
    val navOptions = NavOptions.Builder().setPopUpTo(R.id.lobbyFragment, true).build()
    safeNavigate(action, navOptions)
}

fun NavController.navigateToNeedsUpdate() {
    val action = MainNavDirections.ActionGlobalNeedsUpdateFragment()
    val navOptions = NavOptions.Builder().setPopUpTo(R.id.lobbyFragment, true).build()
    safeNavigate(action, navOptions)
}

fun NavController.navigateToOutOfCapacity() {
    val action = MainNavDirections.ActionGlobalOutOfCapacityFragment()
    val navOptions = NavOptions.Builder().setPopUpTo(R.id.lobbyFragment, false).build()
    safeNavigate(action, navOptions)
}

fun NavController.navigateToNoNetwork() {
    if (currentDestination?.id == R.id.noNetworkFragment) return
    val action = MainNavDirections.ActionGlobalNoNetworkFragment()
    safeNavigate(action)
}

fun NavController.closeNoNetwork() {
    if (currentDestination?.id == R.id.noNetworkFragment) popBackStack()
}

fun FragmentManager.navigateToReportOrIgnoreDialog(caid: CAID, username: String, shouldNavigateBackWhenDone: Boolean) {
    ReportOrIgnoreDialogFragment().apply {
        arguments = MainNavDirections.ActionGlobalReportOrIgnoreDialogFragment(
                caid, username, shouldNavigateBackWhenDone).arguments
        show(this@navigateToReportOrIgnoreDialog, "reportOrIgnoreUser")
    }
}

fun FragmentManager.navigateToUnfollowUserDialog(caid: CAID, username: String, callback: FollowManager.Callback) {
    UnfollowUserDialogFragment().apply {
        positiveClickListener = DialogInterface.OnClickListener { _, _ -> callback.unfollow(caid) }
        arguments = MainNavDirections.ActionGlobalUnfollowUserDialogFragment(username).arguments
        show(this@navigateToUnfollowUserDialog, "unfollowUser")
    }
}
