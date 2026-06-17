package com.owee.app.navigation

import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import com.owee.app.ui.auth.AuthCheckScreen
import com.owee.app.ui.auth.LoginScreen
import com.owee.app.ui.auth.ProfileSetupScreen
import com.owee.app.viewmodel.AuthViewModel

fun NavGraphBuilder.loginNavGraph(
    navController: NavHostController,
    authViewModel: AuthViewModel
) {
    composable(Routes.AuthCheck.route) {
        AuthCheckScreen(
            viewModel = authViewModel,
            onNavigateToLogin = {
                navController.navigate(Routes.Login.route) {
                    popUpTo(Routes.AuthCheck.route) { inclusive = true }
                }
            },
            onNavigateToHome = {
                navController.navigate(Routes.Home.route) {
                    popUpTo(Routes.AuthCheck.route) { inclusive = true }
                }
            },
            onNavigateToProfileSetup = {
                navController.navigate(Routes.ProfileSetup.route) {
                    popUpTo(Routes.AuthCheck.route) { inclusive = true }
                }
            }
        )
    }

    composable(Routes.Login.route) {
        LoginScreen(
            authViewModel = authViewModel,
            onLoginClick = {
                navController.navigate(Routes.Home.route) {
                    popUpTo(Routes.Login.route) { inclusive = true }
                }
            },
            onNavigateToProfileSetup = {
                navController.navigate(Routes.ProfileSetup.route) {
                    popUpTo(Routes.Login.route) { inclusive = true }
                }
            }
        )
    }

    composable(Routes.ProfileSetup.route) {
        ProfileSetupScreen(
            authViewModel = authViewModel,
            onContinueClick = {
                navController.navigate(Routes.Home.route) {
                    popUpTo(Routes.ProfileSetup.route) {
                        inclusive = true
                    }
                }
            }
        )
    }
}
