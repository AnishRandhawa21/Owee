package com.owee.app.navigation

sealed class Routes(val route: String) {

    // Auth
    data object AuthCheck : Routes("auth_check")
    data object Login : Routes("login")
    data object ProfileSetup : Routes("profile_setup")

    // Main Bottom Navigation
    data object Home : Routes("home")
    data object People : Routes("people")
    data object Groups : Routes("groups")
    data object Profile : Routes("profile")
}