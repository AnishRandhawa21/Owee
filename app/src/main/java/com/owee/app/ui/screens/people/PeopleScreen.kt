package com.owee.app.ui.screens.people

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.owee.app.data.remote.model.FriendRequestUi
import com.owee.app.data.remote.model.User
import com.owee.app.viewmodel.AuthViewModel
import com.owee.app.viewmodel.BalanceViewModel
import com.owee.app.viewmodel.FriendsViewModel

@Composable
fun PeopleScreen(
    authViewModel: AuthViewModel,
    friendsViewModel: FriendsViewModel,
    balanceViewModel: BalanceViewModel
) {
    val authUser by authViewModel.user.collectAsState()
    val uiState by friendsViewModel.uiState.collectAsState()
    val balanceState by balanceViewModel.uiState.collectAsState()

    LaunchedEffect(authUser.dbId) {
        friendsViewModel.initialize(authUser.dbId)
        authUser.dbId?.let { balanceViewModel.initialize(it) }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 16.dp)
        ) {
            // ─── Search Bar ───────────────────────────────────────────────────
            item {
                SearchBar(
                    query = uiState.searchQuery,
                    onQueryChange = { friendsViewModel.searchUsers(it) }
                )
            }

            // ─── Friend Requests Section ──────────────────────────────────────
            if (uiState.friendRequests.isNotEmpty()) {
                item {
                    SectionHeader(
                        title = "Friend Requests",
                        badgeText = "${uiState.friendRequests.size} New"
                    )
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 20.dp),
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        modifier = Modifier.padding(vertical = 12.dp)
                    ) {
                        items(uiState.friendRequests) { request ->
                            FriendRequestCard(
                                request = request,
                                onConfirm = { friendsViewModel.acceptRequest(request, authUser.dbId) },
                                onIgnore = { friendsViewModel.rejectRequest(request.requestId, authUser.dbId) }
                            )
                        }
                    }
                }
            }

            // ─── Search Results ───────────────────────────────────────────────
            if (uiState.searchResults.isNotEmpty()) {
                item {
                    SectionHeader(title = "Search Results")
                }
                items(uiState.searchResults) { user ->
                    if (user.id != authUser.dbId) {
                        SearchResultItem(
                            user = user,
                            isFriend = uiState.friends.any { it.id == user.id },
                            isSent = user.id in uiState.sentRequestIds,
                            onAdd = { friendsViewModel.sendFriendRequest(authUser.dbId, user) }
                        )
                    }
                }
            }

            // ─── Friends List Section ─────────────────────────────────────────
            item {
                SectionHeader(title = "Friends List")
            }

            if (uiState.friends.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(40.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "No friends yet",
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                items(uiState.friends) { friend ->
                    val balance = balanceViewModel.getBalanceWithFriend(friend.id ?: "")
                    FriendListItem(
                        friend = friend,
                        balance = balance
                    )
                }
            }
        }
    }
}

@Composable
fun SearchBar(query: String, onQueryChange: (String) -> Unit) {
    OutlinedTextField(
        value = query,
        onValueChange = onQueryChange,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 16.dp),
        placeholder = {
            Text(
                "Search people or groups...",
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
            )
        },
        leadingIcon = {
            Icon(
                Icons.Default.Search,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        },
        shape = RoundedCornerShape(24.dp),
        colors = TextFieldDefaults.colors(
            focusedContainerColor = MaterialTheme.colorScheme.surface,
            unfocusedContainerColor = MaterialTheme.colorScheme.surface,
            disabledContainerColor = MaterialTheme.colorScheme.surface,
            focusedIndicatorColor = MaterialTheme.colorScheme.primary,
            unfocusedIndicatorColor = MaterialTheme.colorScheme.outline,
            focusedTextColor = MaterialTheme.colorScheme.onSurface,
            unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
        ),
        singleLine = true
    )
}

@Composable
fun SectionHeader(title: String, badgeText: String? = null) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )
        if (badgeText != null) {
            Surface(
                color = MaterialTheme.colorScheme.primaryContainer,
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    text = badgeText,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
fun FriendRequestCard(
    request: FriendRequestUi,
    onConfirm: () -> Unit,
    onIgnore: () -> Unit
) {
    Card(
        modifier = Modifier.width(220.dp),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (androidx.compose.foundation.isSystemInDarkTheme()) MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = if (androidx.compose.foundation.isSystemInDarkTheme()) 0.dp else 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            AsyncImage(
                model = null, // In a real app, use request.senderPhotoUrl
                contentDescription = null,
                modifier = Modifier
                    .size(64.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentScale = ContentScale.Crop,
                fallback = rememberVectorPainter(Icons.Default.Person)
            )
            Spacer(Modifier.height(8.dp))
            Text(
                request.senderName,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                "Mutual friends info",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(16.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                // Confirm → secondary (green accent)
                Button(
                    onClick = onConfirm,
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(vertical = 8.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.secondary,
                        contentColor = MaterialTheme.colorScheme.onSecondary
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Confirm", fontSize = 12.sp)
                }
                // Ignore → surfaceVariant (muted)
                Button(
                    onClick = onIgnore,
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(vertical = 8.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant,
                        contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Ignore", fontSize = 12.sp)
                }
            }
        }
    }
}

@Composable
fun FriendListItem(friend: User, balance: Double) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 6.dp),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (androidx.compose.foundation.isSystemInDarkTheme()) MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = if (androidx.compose.foundation.isSystemInDarkTheme()) 0.dp else 2.dp)
    ) {
        Row(
            modifier = Modifier
                .padding(12.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AsyncImage(
                model = friend.photo_url,
                contentDescription = null,
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentScale = ContentScale.Crop,
                fallback = rememberVectorPainter(Icons.Default.Person)
            )
            Spacer(Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    friend.name,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    "Recent activity or group",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                val statusText = when {
                    balance > 0.01 -> "OWES YOU"
                    balance < -0.01 -> "YOU OWE"
                    else -> "SETTLED"
                }
                val statusColor = when {
                    balance > 0.01 -> MaterialTheme.colorScheme.secondary       // green accent from theme
                    balance < -0.01 -> MaterialTheme.colorScheme.error           // red for owing
                    else -> MaterialTheme.colorScheme.onSurfaceVariant           // neutral
                }
                Text(
                    statusText,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "₹${"%.2f".format(kotlin.math.abs(balance))}",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = statusColor
                )
            }
        }
    }
}

@Composable
fun SearchResultItem(
    user: User,
    isFriend: Boolean,
    isSent: Boolean,
    onAdd: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 4.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (androidx.compose.foundation.isSystemInDarkTheme()) MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = if (androidx.compose.foundation.isSystemInDarkTheme()) 0.dp else 1.dp)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AsyncImage(
                model = user.photo_url,
                contentDescription = null,
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                fallback = rememberVectorPainter(Icons.Default.Person)
            )
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    user.name,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    "@${user.username}",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            when {
                isFriend -> Text(
                    "Friends",
                    color = MaterialTheme.colorScheme.secondary,
                    fontWeight = FontWeight.Bold
                )
                isSent -> Text(
                    "Sent",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                else -> Button(
                    onClick = onAdd,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    )
                ) {
                    Text("Add")
                }
            }
        }
    }
}

@Composable
fun rememberVectorPainter(image: androidx.compose.ui.graphics.vector.ImageVector) =
    androidx.compose.ui.graphics.vector.rememberVectorPainter(image)