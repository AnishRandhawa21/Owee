package com.owee.app.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import com.owee.app.data.repository.FriendRepository
import com.owee.app.viewmodel.*

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
    
    val application = androidx.compose.ui.platform.LocalContext.current.applicationContext as android.app.Application
    val balanceViewModel: BalanceViewModel = viewModel(
        factory = object : androidx.lifecycle.ViewModelProvider.Factory {
            override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
                return BalanceViewModel(application) as T
            }
        }
    )

    // Global Initialization: Fetch data as soon as user is logged in
    val authUser by authViewModel.user.collectAsState()
    LaunchedEffect(authUser.dbId) {
        val id = authUser.dbId
        if (!id.isNullOrBlank()) {
            friendsViewModel.initialize(id)
            groupsViewModel.initialize(id)
            balanceViewModel.initialize(id)
        }
    }

    NavHost(
        navController = navController,
        startDestination = Routes.AuthCheck.route,
        modifier = modifier
    ) {
        loginNavGraph(navController, authViewModel)
        mainNavGraph(
            navController,
            authViewModel,
            friendsViewModel,
            groupsViewModel,
            expensesViewModel,
            balanceViewModel
        )
    }
}