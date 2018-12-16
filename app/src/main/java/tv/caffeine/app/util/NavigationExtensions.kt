package tv.caffeine.app.util

import android.os.Bundle
import androidx.annotation.IdRes
import androidx.fragment.app.FragmentManager
import androidx.navigation.NavController
import androidx.navigation.NavDirections
import androidx.navigation.NavOptions
import androidx.navigation.Navigator
import timber.log.Timber
import tv.caffeine.app.LobbyDirections
import tv.caffeine.app.R
import tv.caffeine.app.profile.ReportOrIgnoreDialogFragment

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
    val action = LobbyDirections.ActionGlobalLandingFragment(message)
    val navOptions = NavOptions.Builder().setPopUpTo(R.id.lobbyFragment, true).build()
    safeNavigate(action, navOptions)
}

fun NavController.navigateToNeedsUpdate() {
    val action = LobbyDirections.ActionGlobalNeedsUpdateFragment()
    val navOptions = NavOptions.Builder().setPopUpTo(R.id.lobbyFragment, true).build()
    safeNavigate(action, navOptions)
}

fun NavController.navigateToNoNetwork() {
    if (currentDestination?.id == R.id.noNetworkFragment) return
    val action = LobbyDirections.ActionGlobalNoNetworkFragment()
    safeNavigate(action)
}

fun NavController.closeNoNetwork() {
    if (currentDestination?.id == R.id.noNetworkFragment) popBackStack()
}

fun FragmentManager.navigateToReportOrIgnoreDialog(caid: String, username: String, shouldNavigateBackWhenDone: Boolean) {
    ReportOrIgnoreDialogFragment().let {
        it.arguments = LobbyDirections.ActionGlobalReportOrIgnoreDialogFragment(
                caid, username, shouldNavigateBackWhenDone).arguments
        it.show(this, "reportOrIgnoreUser")
    }
}
