package com.owee.app.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import com.owee.app.viewmodel.AuthViewModel
import com.owee.app.viewmodel.FriendsViewModel
import androidx.lifecycle.viewmodel.compose.viewModel

@Composable
fun RootNavGraph(
    navController: NavHostController,
    authViewModel: AuthViewModel,
    modifier: Modifier = Modifier
) {
    val friendsViewModel: FriendsViewModel = viewModel()

    NavHost(
        navController = navController,
        startDestination = Routes.AuthCheck.route,
        modifier = modifier
    ) {

        loginNavGraph(navController, authViewModel)
        mainNavGraph(navController, authViewModel, friendsViewModel)

    }
}
