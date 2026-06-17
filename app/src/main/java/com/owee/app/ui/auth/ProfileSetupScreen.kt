package com.owee.app.ui.auth

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.owee.app.data.remote.SupabaseProvider
import com.owee.app.viewmodel.AuthViewModel
import com.owee.app.viewmodel.ProfileSetupState
import com.owee.app.viewmodel.ProfileSetupViewModel
import io.github.jan.supabase.auth.auth

@Composable
fun ProfileSetupScreen(
    authViewModel: AuthViewModel,
    onContinueClick: () -> Unit,
    profileViewModel: ProfileSetupViewModel = viewModel()
) {

    var name by remember { mutableStateOf("") }
    var username by remember { mutableStateOf("") }

    val uiState by profileViewModel.uiState.collectAsState()
    val authUserState by authViewModel.user.collectAsState()

    LaunchedEffect(uiState) {
        if (uiState is ProfileSetupState.Success) {
            val authId = SupabaseProvider.client.auth.currentUserOrNull()?.id
            if (authId != null) {
                authViewModel.updateUser(
                    id = authId,
                    token = "",
                    email = authUserState.email,
                    photoUrl = authUserState.photoUrl,
                    name = name,
                    username = username
                )
            }
            onContinueClick()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {

        if (uiState is ProfileSetupState.Error) {
            Text(
                text = (uiState as ProfileSetupState.Error).message,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(bottom = 8.dp)
            )
        }

        if (uiState is ProfileSetupState.UsernameAlreadyExists) {
            Text(
                text = "Username is already taken",
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(bottom = 8.dp)
            )
        }

        OutlinedTextField(
            value = name,
            onValueChange = { name = it },
            label = { Text("Name") },
            enabled = uiState !is ProfileSetupState.Loading
        )

        OutlinedTextField(
            value = username,
            onValueChange = { username = it },
            label = { Text("Username") },
            enabled = uiState !is ProfileSetupState.Loading
        )

        Button(
            onClick = {
                profileViewModel.completeProfile(
                    name = name,
                    username = username,
                    photoUrl = authUserState.photoUrl
                )
            },
            modifier = Modifier.padding(top = 16.dp),
            enabled = uiState !is ProfileSetupState.Loading
        ) {
            if (uiState is ProfileSetupState.Loading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    color = MaterialTheme.colorScheme.onPrimary
                )
            } else {
                Text("Continue")
            }
        }
    }
}
