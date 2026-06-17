package com.owee.app.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.People
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.owee.app.navigation.Routes

@Composable
fun BottomBar(
    navController: NavHostController
) {

    val navBackStackEntry =
        navController.currentBackStackEntryAsState()

    val currentRoute =
        navBackStackEntry.value?.destination?.route

    // Hide BottomBar on Auth Screens
    val authRoutes = listOf(
        Routes.AuthCheck.route,
        Routes.Login.route,
        Routes.ProfileSetup.route
    )

    if (currentRoute in authRoutes) return

    val items = listOf(
        BottomNavItem(
            route = Routes.Home.route,
            title = "Home",
            icon = Icons.Default.Home
        ),
        BottomNavItem(
            route = Routes.People.route,
            title = "People",
            icon = Icons.Default.People
        ),
        BottomNavItem(
            route = Routes.Groups.route,
            title = "Groups",
            icon = Icons.Default.Groups
        ),
        BottomNavItem(
            route = Routes.Profile.route,
            title = "Profile",
            icon = Icons.Default.Person
        )
    )

    NavigationBar {

        items.forEach { item ->

            NavigationBarItem(
                selected = currentRoute == item.route,

                onClick = {

                    navController.navigate(item.route) {

                        // Avoid duplicate destinations
                        launchSingleTop = true

                        // Restore previous state
                        restoreState = true

                        // Pop to root of bottom navigation
                        popUpTo(navController.graph.startDestinationId) {
                            saveState = true
                        }
                    }
                },

                icon = {
                    Icon(
                        imageVector = item.icon,
                        contentDescription = item.title
                    )
                },

                label = {
                    Text(item.title)
                }
            )
        }
    }
}