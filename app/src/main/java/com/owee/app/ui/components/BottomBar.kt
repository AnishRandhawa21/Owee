package com.owee.app.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.owee.app.navigation.Routes

@Composable
fun BottomBar(navController: NavHostController) {

    val currentRoute = navController
        .currentBackStackEntryAsState()
        .value?.destination?.route

    val hiddenRoutes = listOf(
        Routes.AuthCheck.route,
        Routes.Login.route,
        Routes.ProfileSetup.route,
        Routes.CreateGroup.route,
        Routes.GroupDetails.route,
        Routes.CreateExpense.route,
        Routes.ExpenseDetails.route,
        Routes.Settlement.route
    )

    if (currentRoute in hiddenRoutes) return

    val items = listOf(
        BottomNavItem(Routes.Home.route, "Home", Icons.Default.Home),
        BottomNavItem(Routes.People.route, "People", Icons.Default.People),
        BottomNavItem(Routes.GroupsGraph.route, "Groups", Icons.Default.Groups),
        BottomNavItem(Routes.Profile.route, "Profile", Icons.Default.Person)
    )

    NavigationBar {
        items.forEach { item ->
            // highlight Groups tab when on any screen in the groups graph
            val selected = when (item.route) {
                Routes.GroupsGraph.route -> currentRoute in listOf(
                    Routes.Groups.route,
                    Routes.CreateGroup.route,
                    Routes.GroupDetails.route
                )
                else -> currentRoute == item.route
            }

            NavigationBarItem(
                selected = selected,
                onClick = {
                    navController.navigate(item.route) {
                        launchSingleTop = true
                        restoreState = true
                        popUpTo(Routes.Home.route) {
                            saveState = true
                        }
                    }
                },
                icon = { Icon(item.icon, contentDescription = item.title) },
                label = { Text(item.title) }
            )
        }
    }
}