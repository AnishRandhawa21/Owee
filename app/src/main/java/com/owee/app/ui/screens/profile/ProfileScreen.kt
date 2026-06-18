package com.owee.app.ui.screens.profile

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.owee.app.viewmodel.AuthViewModel

@Composable
fun ProfileScreen(
    onLogout: () -> Unit,
    viewModel: AuthViewModel = viewModel()
) {
    val user by viewModel.user.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Profile Picture
        AsyncImage(
            model = if (user.photoUrl.isNullOrEmpty()) null else user.photoUrl,
            contentDescription = "Profile Picture",
            modifier = Modifier
                .size(120.dp)
                .clip(CircleShape),
            contentScale = ContentScale.Crop,
            placeholder = rememberVectorPainter(Icons.Default.Person),
            error = rememberVectorPainter(Icons.Default.Person)
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Name
        Text(
            text = if (user.name.isNullOrEmpty()) "No Name" else user.name!!,
            style = MaterialTheme.typography.headlineMedium
        )

        // @username
        Text(
            text = "@${if (user.username.isNullOrEmpty()) "username" else user.username!!}",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.primary
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Email
        Text(
            text = user.email ?: "No Email",
            style = MaterialTheme.typography.bodyMedium
        )

        // Joined Date
        val joinedDate = user.joinedDate
        if (!joinedDate.isNullOrEmpty()) {
            val date = try {
                joinedDate.substringBefore("T")
            } catch (e: Exception) {
                joinedDate
            }
            Text(
                text = "Joined: $date",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.outline
            )
        }

        Spacer(modifier = Modifier.height(48.dp))

        // Logout Button
        Button(
            onClick = {
                viewModel.logout(onLogout)
            },
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.error
            ),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Logout")
        }
    }
}
