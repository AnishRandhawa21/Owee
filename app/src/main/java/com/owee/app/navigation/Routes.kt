package com.owee.app.navigation

sealed class Routes(val route: String) {
    object AuthCheck : Routes("auth_check")
    object Login : Routes("login")
    object ProfileSetup : Routes("profile_setup")
    object Home : Routes("home")
    object Profile : Routes("profile")
}
