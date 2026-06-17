package com.owee.app.ui.auth

import android.util.Log
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import com.owee.app.data.remote.AuthManager
import com.owee.app.viewmodel.AuthState
import com.owee.app.viewmodel.AuthViewModel
import kotlinx.coroutines.launch

@Composable
fun LoginScreen(
    authViewModel: AuthViewModel,
    onLoginClick: () -> Unit,
    onNavigateToProfileSetup: () -> Unit
) {

    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {

        Text("OWEE")

        val context = LocalContext.current
        val scope = rememberCoroutineScope()
        val authState by authViewModel.authState.collectAsState()

        LaunchedEffect(authState) {
            when (authState) {
                is AuthState.Authenticated -> onLoginClick()
                is AuthState.NeedsProfile -> onNavigateToProfileSetup()
                else -> Unit
            }
        }

        Button(
            onClick = {
                scope.launch {
                    try {
                        val user = AuthManager(context).signIn()
                        if (user != null) {
                            authViewModel.updateUser(
                                id = user.id,
                                token = user.token,
                                email = user.email,
                                photoUrl = user.photoUrl ?: ""
                            )
                        }
                    } catch (e: Exception) {
                        Log.e("OWEE_LOGIN", e.message ?: "Error")
                    }
                }
            }
        ) {
            Text("Continue with Google")
        }
    }
}
