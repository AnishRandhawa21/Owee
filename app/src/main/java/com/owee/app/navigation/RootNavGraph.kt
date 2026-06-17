package com.owee.app.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import com.owee.app.data.repository.FriendRepository
import com.owee.app.viewmodel.AuthViewModel
import com.owee.app.viewmodel.ExpensesViewModel
import com.owee.app.viewmodel.FriendsViewModel
import com.owee.app.viewmodel.GroupsViewModel

@Composable
fun RootNavGraph(
    navController: NavHostController,
    authViewModel: AuthViewModel,
    modifier: Modifier = Modifier
) {
    val friendRepository = remember { FriendRepository() }
    val friendsViewModel: FriendsViewModel = viewModel {
        FriendsViewModel(repository = friendRepository)
    }
    val groupsViewModel: GroupsViewModel = viewModel {
        GroupsViewModel(friendRepository = friendRepository)
    }
    val expensesViewModel: ExpensesViewModel = viewModel()

    NavHost(
        navController = navController,
        startDestination = Routes.AuthCheck.route,
        modifier = modifier
    ) {
        loginNavGraph(navController, authViewModel)
        mainNavGraph(navController, authViewModel, friendsViewModel, groupsViewModel, expensesViewModel)
    }
}