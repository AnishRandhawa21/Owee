package com.owee.app.navigation

import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import androidx.navigation.compose.navigation
import com.owee.app.ui.screens.*
import com.owee.app.viewmodel.AuthViewModel
import com.owee.app.viewmodel.FriendsViewModel
import com.owee.app.viewmodel.GroupsViewModel

fun NavGraphBuilder.mainNavGraph(
    navController: NavHostController,
    authViewModel: AuthViewModel,
    friendsViewModel: FriendsViewModel,
    groupsViewModel: GroupsViewModel
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

    navigation(
        startDestination = Routes.Groups.route,
        route = Routes.GroupsGraph.route
    ) {
        composable(Routes.Groups.route) {
            GroupsScreen(
                authViewModel = authViewModel,
                groupsViewModel = groupsViewModel,
                onNavigateToCreate = {
                    navController.navigate(Routes.CreateGroup.route)
                },
                onNavigateToDetails = {
                    navController.navigate(Routes.GroupDetails.route)
                }
            )
        }

        composable(Routes.CreateGroup.route) {
            CreateGroupScreen(
                groupsViewModel = groupsViewModel,
                onNavigateBack = { navController.popBackStack() },
                onGroupCreated = {
                    navController.popBackStack(Routes.Groups.route, inclusive = false)
                }
            )
        }

        composable(Routes.GroupDetails.route) {
            GroupDetailsScreen(
                groupsViewModel = groupsViewModel,
                onNavigateBack = { navController.popBackStack() }
            )
        }
    }

    composable(Routes.Profile.route) {
        ProfileScreen(
            viewModel = authViewModel,
            onLogout = {
                navController.navigate(Routes.Login.route) {
                    popUpTo(0) { inclusive = true }
                }
            }
        )
    }
}