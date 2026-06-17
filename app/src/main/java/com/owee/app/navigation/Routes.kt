package com.owee.app.navigation

sealed class Routes(val route: String) {

    // Auth
    data object AuthCheck : Routes("auth_check")
    data object Login : Routes("login")
    data object ProfileSetup : Routes("profile_setup")

    // Main Bottom Navigation
    data object Home : Routes("home")
    data object People : Routes("people")
    data object Profile : Routes("profile")

    // Groups Graph
    data object GroupsGraph : Routes("groups_graph")
    data object Groups : Routes("groups")
    data object CreateGroup : Routes("create_group")
    data object GroupDetails : Routes("group_details")
}