package tv.caffeine.app.util

import androidx.navigation.NavController
import androidx.navigation.NavOptions
import tv.caffeine.app.LobbyDirections
import tv.caffeine.app.R

fun NavController.navigateToLanding(message: String? = null) {
    val action = LobbyDirections.ActionGlobalLandingFragment(message)
    val navOptions = NavOptions.Builder().setPopUpTo(R.id.lobbyFragment, true).build()
    navigate(action, navOptions)
}

