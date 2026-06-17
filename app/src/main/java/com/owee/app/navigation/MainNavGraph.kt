package com.owee.app.navigation

import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import com.owee.app.ui.screens.HomeScreen
import com.owee.app.ui.screens.ProfileScreen
import com.owee.app.viewmodel.AuthViewModel

fun NavGraphBuilder.mainNavGraph(
    navController: NavHostController,
    authViewModel: AuthViewModel
) {

    composable(Routes.Home.route) {
        HomeScreen()
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
