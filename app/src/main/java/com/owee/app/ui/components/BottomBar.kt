package com.owee.app.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
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

    // Hide BottomBar on Auth screens
    val authRoutes = listOf(
        Routes.Login.route,
        Routes.ProfileSetup.route,
        Routes.AuthCheck.route
    )

    if (currentRoute in authRoutes) return

    NavigationBar {

        NavigationBarItem(
            selected = currentRoute == Routes.Home.route,
            onClick = {
                navController.navigate(Routes.Home.route)
            },
            icon = {
                Icon(
                    imageVector = Icons.Default.Home,
                    contentDescription = null
                )
            },
            label = {
                Text("Home")
            }
        )

        NavigationBarItem(
            selected = currentRoute == Routes.Profile.route,
            onClick = {
                navController.navigate(Routes.Profile.route)
            },
            icon = {
                Icon(
                    imageVector = Icons.Default.Person,
                    contentDescription = null
                )
            },
            label = {
                Text("Profile")
            }
        )
    }
}