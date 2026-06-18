package com.owee.app.navigation

import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import androidx.navigation.compose.navigation
import com.owee.app.ui.screens.home.HomeScreen
import com.owee.app.ui.screens.people.PeopleScreen
import com.owee.app.ui.screens.groups.GroupsScreen
import com.owee.app.ui.screens.groups.CreateGroupScreen
import com.owee.app.ui.screens.groups.GroupDetailsScreen
import com.owee.app.ui.screens.settlement.SettlementScreen
import com.owee.app.ui.screens.expenses.CreateExpenseScreen
import com.owee.app.ui.screens.expenses.ExpenseDetailsScreen
import com.owee.app.ui.screens.profile.ProfileScreen
import com.owee.app.viewmodel.*

fun NavGraphBuilder.mainNavGraph(
    navController: NavHostController,
    authViewModel: AuthViewModel,
    friendsViewModel: FriendsViewModel,
    groupsViewModel: GroupsViewModel,
    expensesViewModel: ExpensesViewModel,
    balanceViewModel: BalanceViewModel
) {
    composable(Routes.Home.route) {
        HomeScreen(
            authViewModel = authViewModel,
            balanceViewModel = balanceViewModel
        )
    }

    composable(Routes.People.route) {
        PeopleScreen(
            authViewModel = authViewModel,
            friendsViewModel = friendsViewModel,
            balanceViewModel = balanceViewModel
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
                balanceViewModel = balanceViewModel,
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
            val currentUserId = authViewModel.user.value.dbId ?: return@composable

            GroupDetailsScreen(
                currentUserId = currentUserId,
                groupsViewModel = groupsViewModel,
                expensesViewModel = expensesViewModel,
                balanceViewModel = balanceViewModel,
                onNavigateBack = { navController.popBackStack() },
                onNavigateToCreateExpense = {
                    navController.navigate(Routes.CreateExpense.route)
                },
                onNavigateToExpenseDetails = {
                    navController.navigate(Routes.ExpenseDetails.route)
                },
                onNavigateToSettlement = {
                    navController.navigate(Routes.Settlement.route)
                }
            )
        }

        composable(Routes.CreateExpense.route) {
            val currentUserId = authViewModel.user.value.dbId ?: return@composable
            val members = groupsViewModel.uiState.value.selectedGroup?.members ?: emptyList()

            CreateExpenseScreen(
                currentUserId = currentUserId,
                members = members,
                expensesViewModel = expensesViewModel,
                onNavigateBack = { navController.popBackStack() },
                onExpenseCreated = {
                    navController.popBackStack(Routes.GroupDetails.route, inclusive = false)
                }
            )
        }

        composable(Routes.ExpenseDetails.route) {
            val currentUserId = authViewModel.user.value.dbId ?: return@composable

            ExpenseDetailsScreen(
                currentUserId = currentUserId,
                expensesViewModel = expensesViewModel,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(Routes.Settlement.route) {
            val groupId = groupsViewModel.uiState.value.selectedGroup?.group?.id ?: return@composable
            SettlementScreen(
                groupId = groupId,
                balanceViewModel = balanceViewModel,
                onNavigateBack = { navController.popBackStack() }
            )
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
}
