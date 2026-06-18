package com.owee.app

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Payment
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import coil.compose.AsyncImage
import com.owee.app.navigation.RootNavGraph
import com.owee.app.navigation.Routes
import com.owee.app.ui.components.BottomBar
import com.owee.app.viewmodel.AuthViewModel
import com.owee.app.viewmodel.GroupsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen() {
    val navController = rememberNavController()
    val authViewModel: AuthViewModel = viewModel()
    val groupsViewModel: GroupsViewModel = viewModel()

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    Scaffold(
        topBar = {
            DynamicTopBar(currentRoute, navController, groupsViewModel, authViewModel)
        },
        bottomBar = {
            BottomBar(navController)
        },
        floatingActionButton = {
            DynamicFAB(currentRoute, navController)
        }
    ) { innerPadding ->
        RootNavGraph(
            navController = navController,
            authViewModel = authViewModel,
            modifier = Modifier.padding(innerPadding)
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DynamicTopBar(
    currentRoute: String?,
    navController: NavHostController,
    groupsViewModel: GroupsViewModel,
    authViewModel: AuthViewModel
) {
    val groupState by groupsViewModel.uiState.collectAsState()
    val user by authViewModel.user.collectAsState()

    val showTopBar = when (currentRoute) {
        Routes.AuthCheck.route, Routes.Login.route, Routes.ProfileSetup.route -> false
        else -> true
    }

    if (!showTopBar) return

    val title = when (currentRoute) {
        Routes.Home.route -> "OWEE"
        Routes.People.route -> "People"
        Routes.Groups.route -> "Groups"
        Routes.CreateGroup.route -> "New Group"
        Routes.GroupDetails.route -> groupState.selectedGroup?.group?.name ?: "Group Details"
        Routes.CreateExpense.route -> "Add Expense"
        Routes.ExpenseDetails.route -> "Details"
        Routes.Settlement.route -> "Settlements"
        Routes.Profile.route -> "Profile"
        else -> "OWEE"
    }

    val showBackButton = currentRoute !in listOf(
        Routes.Home.route,
        Routes.People.route,
        Routes.Groups.route,
        Routes.Profile.route
    )

    TopAppBar(
        title = {
            Text(
                text = title,
                fontWeight = FontWeight.Bold,
                fontSize = 22.sp,
                color = MaterialTheme.colorScheme.primary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        },
        navigationIcon = {
            if (showBackButton) {
                IconButton(onClick = { navController.popBackStack() }) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                }
            } else {
                IconButton(onClick = {
                    navController.navigate(Routes.Profile.route) {
                        launchSingleTop = true
                        restoreState = true
                        popUpTo(Routes.Home.route) {
                            saveState = true
                        }
                    }
                }) {
                    AsyncImage(
                        model = user.photoUrl,
                        contentDescription = "Profile",
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape),
                        fallback = androidx.compose.ui.graphics.vector.rememberVectorPainter(Icons.Default.Person)
                    )
                }
            }
        },
        actions = {
            if (currentRoute == Routes.GroupDetails.route) {
                IconButton(onClick = { navController.navigate(Routes.Settlement.route) }) {
                    Icon(Icons.Default.Payment, contentDescription = "Settle Up")
                }
            }
            if (currentRoute in listOf(Routes.Home.route, Routes.People.route, Routes.Groups.route)) {
                IconButton(onClick = { /* TODO: Open Notifications */ }) {
                    Icon(Icons.Default.Notifications, contentDescription = "Notifications")
                }
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = Color.Transparent,
            titleContentColor = MaterialTheme.colorScheme.primary
        )
    )
}

@Composable
fun DynamicFAB(currentRoute: String?, navController: NavHostController) {
    when (currentRoute) {
        Routes.People.route -> {
            FloatingActionButton(
                onClick = { /* Search is enough in redesigned PeopleScreen */ },
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                shape = RoundedCornerShape(16.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.PersonAdd,
                    contentDescription = "Add Friend",
                    modifier = Modifier.size(28.dp)
                )
            }
        }
        Routes.Groups.route -> {
            FloatingActionButton(
                onClick = { navController.navigate(Routes.CreateGroup.route) },
                containerColor = MaterialTheme.colorScheme.primary
            ) {
                Icon(Icons.Default.Add, contentDescription = "Create Group")
            }
        }
        Routes.GroupDetails.route -> {
            FloatingActionButton(
                onClick = { navController.navigate(Routes.CreateExpense.route) },
                containerColor = MaterialTheme.colorScheme.primary
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Expense")
            }
        }
    }
}
