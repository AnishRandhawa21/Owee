package com.owee.app.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.owee.app.viewmodel.AuthViewModel
import com.owee.app.viewmodel.FriendsViewModel

@Composable
fun PeopleScreen(
    authViewModel: AuthViewModel,
    friendsViewModel: FriendsViewModel
) {
    val authUser by authViewModel.user.collectAsState()
    val uiState by friendsViewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(authUser.dbId) {
        friendsViewModel.initialize(authUser.dbId)
    }

    LaunchedEffect(uiState.message, uiState.error) {
        uiState.message?.let {
            snackbarHostState.showSnackbar(it)
            friendsViewModel.clearMessage()
        }
        uiState.error?.let {
            snackbarHostState.showSnackbar(it)
            friendsViewModel.clearMessage()
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        item {
            Text(
                text = "Find People",
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            OutlinedTextField(
                value = uiState.searchQuery,
                onValueChange = { friendsViewModel.searchUsers(it) },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Search Username") },
                singleLine = true
            )
            Spacer(modifier = Modifier.height(16.dp))
        }

        if (uiState.searchResults.isNotEmpty()) {
            item {
                Text("Search Results", style = MaterialTheme.typography.titleMedium)
            }
            items(uiState.searchResults) { user ->
                if (user.id != authUser.dbId) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .padding(12.dp)
                                .fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(user.name, style = MaterialTheme.typography.bodyLarge)
                                Text("@${user.username}", style = MaterialTheme.typography.bodyMedium, color = Color.Gray)
                            }
                            when {
                                uiState.friends.any { it.id == user.id } -> {
                                    Text(
                                        "Friends",
                                        style = MaterialTheme.typography.labelLarge,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                                user.id in uiState.sentRequestIds -> {
                                    FilledTonalButton(enabled = false, onClick = {}) {
                                        Text("Request Sent")
                                    }
                                }
                                else -> {
                                    Button(onClick = {
                                        friendsViewModel.sendFriendRequest(authUser.dbId, user)
                                    }) {
                                        Text("Add")
                                    }
                                }
                            }
                        }
                    }
                }
            }
            item { Spacer(modifier = Modifier.height(24.dp)) }
        }

        if (uiState.friendRequests.isNotEmpty()) {
            item {
                Text("Friend Requests", style = MaterialTheme.typography.titleMedium)
            }
            items(uiState.friendRequests) { request ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .padding(12.dp)
                            .fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(request.senderName, style = MaterialTheme.typography.bodyLarge)
                            Text("@${request.senderUsername}", style = MaterialTheme.typography.bodyMedium)
                        }
                        Row {
                            TextButton(onClick = {
                                friendsViewModel.rejectRequest(request.requestId, authUser.dbId)
                            }) {
                                Text("Reject", color = MaterialTheme.colorScheme.error)
                            }
                            Button(onClick = {
                                friendsViewModel.acceptRequest(request, authUser.dbId)
                            }) {
                                Text("Accept")
                            }
                        }
                    }
                }
            }
            item { Spacer(modifier = Modifier.height(24.dp)) }
        }

        if (uiState.sentRequests.isNotEmpty()) {
            item {
                Text("Sent Invitations", style = MaterialTheme.typography.titleMedium)
            }
            items(uiState.sentRequests) { user ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .padding(12.dp)
                            .fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(user.name, style = MaterialTheme.typography.bodyLarge)
                            Text("@${user.username}", style = MaterialTheme.typography.bodyMedium, color = Color.Gray)
                        }
                        FilledTonalButton(enabled = false, onClick = {}) {
                            Text("Pending")
                        }
                    }
                }
            }
            item { Spacer(modifier = Modifier.height(24.dp)) }
        }

        item {
            Text("Friends", style = MaterialTheme.typography.titleMedium)
        }
        if (uiState.friends.isEmpty()) {
            item {
                Text(
                    "No friends yet",
                    modifier = Modifier.padding(vertical = 8.dp),
                    color = Color.Gray
                )
            }
        } else {
            items(uiState.friends) { friend ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(friend.name, style = MaterialTheme.typography.bodyLarge)
                            Text("@${friend.username}", style = MaterialTheme.typography.bodyMedium, color = Color.Gray)
                        }
                    }
                }
            }
        }
    }
}