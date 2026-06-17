package com.owee.app.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import com.owee.app.viewmodel.AuthViewModel

@Composable
fun RootNavGraph(
    navController: NavHostController,
    authViewModel: AuthViewModel,
    modifier: Modifier = Modifier
) {

    NavHost(
        navController = navController,
        startDestination = Routes.AuthCheck.route,
        modifier = modifier
    ) {

        loginNavGraph(navController, authViewModel)
        mainNavGraph(navController, authViewModel)

    }
}
