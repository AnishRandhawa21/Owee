package com.owee.app

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.rememberNavController
import com.owee.app.navigation.RootNavGraph
import com.owee.app.ui.components.BottomBar
import com.owee.app.viewmodel.AuthViewModel

@Composable
fun MainScreen() {

    val navController = rememberNavController()
    val authViewModel: AuthViewModel = viewModel()

    Scaffold(
        bottomBar = {
            BottomBar(navController)
        }
    ) { innerPadding ->

        RootNavGraph(
            navController = navController,
            authViewModel = authViewModel,
            modifier = Modifier.padding(innerPadding)
        )
    }
}
