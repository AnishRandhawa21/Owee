package com.owee.app.navigation

import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import com.owee.app.ui.screens.GroupsScreen
import com.owee.app.ui.screens.HomeScreen
import com.owee.app.ui.screens.PeopleScreen
import com.owee.app.ui.screens.ProfileScreen
import com.owee.app.viewmodel.AuthViewModel
import com.owee.app.viewmodel.FriendsViewModel

fun NavGraphBuilder.mainNavGraph(
    navController: NavHostController,
    authViewModel: AuthViewModel,
    friendsViewModel: FriendsViewModel
) {
    composable(Routes.Home.route) {
        HomeScreen()
    }

    composable(Routes.People.route) {
        PeopleScreen(
            authViewModel = authViewModel,
            friendsViewModel = friendsViewModel
        )
    }

    composable(Routes.Groups.route) {
        GroupsScreen()
    }

    composable(Routes.Profile.route) {
        ProfileScreen(
            viewModel = authViewModel,
            onLogout = {
                navController.navigate(Routes.Login.route) {
                    popUpTo(0) {
                        inclusive = true
                    }
                }
            }
        )
    }
}
