package com.owee.app.ui.screens.people

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
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
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PeopleScreen(
    authViewModel: AuthViewModel,
    friendsViewModel: FriendsViewModel,
    balanceViewModel: BalanceViewModel
) {
    val authUser by authViewModel.user.collectAsState()
    val uiState by friendsViewModel.uiState.collectAsState()

    PullToRefreshBox(
        isRefreshing = uiState.isLoading,
        onRefresh = { 
            friendsViewModel.refresh()
            balanceViewModel.refresh()
        },
        modifier = Modifier.fillMaxSize()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 80.dp)
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
                            badgeText = "${uiState.friendRequests.size} NEW"
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
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
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
}

@Composable
fun SearchBar(query: String, onQueryChange: (String) -> Unit) {
    TextField(
        value = query,
        onValueChange = onQueryChange,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 16.dp),
        placeholder = {
            Text(
                "Search friends or people...",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
            )
        },
        leadingIcon = {
            Icon(
                Icons.Default.Search,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
            )
        },
        shape = RoundedCornerShape(16.dp),
        colors = TextFieldDefaults.colors(
            focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
            disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
            focusedIndicatorColor = Color.Transparent,
            unfocusedIndicatorColor = Color.Transparent,
            disabledIndicatorColor = Color.Transparent,
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
            .padding(horizontal = 20.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )
        if (badgeText != null) {
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    text = badgeText,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.Bold,
                    fontSize = 10.sp
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
        modifier = Modifier.width(280.dp),
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                AsyncImage(
                    model = request.senderPhotoUrl,
                    contentDescription = null,
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                    contentScale = ContentScale.Crop,
                    fallback = androidx.compose.ui.graphics.vector.rememberVectorPainter(Icons.Default.Person)
                )
                Spacer(Modifier.width(16.dp))
                Column {
                    Text(
                        request.senderName,
                        fontWeight = FontWeight.Bold,
                        fontSize = 17.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        "@${request.senderUsername}",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                }
            }
            
            Spacer(Modifier.height(20.dp))
            
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Button(
                    onClick = onConfirm,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.6f),
                        contentColor = MaterialTheme.colorScheme.secondary
                    ),
                    shape = RoundedCornerShape(12.dp),
                    contentPadding = PaddingValues(vertical = 10.dp)
                ) {
                    Text("ACCEPT", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }
                Button(
                    onClick = onIgnore,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant,
                        contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                    ),
                    shape = RoundedCornerShape(12.dp),
                    contentPadding = PaddingValues(vertical = 10.dp)
                ) {
                    Text("IGNORE", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }
            }
        }
    }
}

@Composable
fun FriendListItem(friend: User, balance: Double) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 12.dp),
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
            fallback = androidx.compose.ui.graphics.vector.rememberVectorPainter(Icons.Default.Person)
        )
        
        Spacer(Modifier.width(16.dp))
        
        Column(modifier = Modifier.weight(1f)) {
            Text(
                friend.name,
                fontWeight = FontWeight.SemiBold,
                fontSize = 16.sp,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                "@${friend.username}",
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
            )
        }
        
        Column(horizontalAlignment = Alignment.End) {
            val isOwed = balance > 0.01
            val isOwe = balance < -0.01
            
            val statusText = when {
                isOwed -> "OWES YOU"
                isOwe -> "YOU OWE"
                else -> "ALL CLEAR"
            }
            
            val amountColor = when {
                isOwed -> MaterialTheme.colorScheme.secondary
                isOwe -> MaterialTheme.colorScheme.error
                else -> MaterialTheme.colorScheme.onSurface
            }
            
            Text(
                text = when {
                    isOwed -> "+₹${String.format(Locale.US, "%.2f", balance)}"
                    isOwe -> "-₹${String.format(Locale.US, "%.2f", kotlin.math.abs(balance))}"
                    else -> "Settled"
                },
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = amountColor
            )
            
            Text(
                statusText,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                letterSpacing = 0.5.sp
            )
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
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AsyncImage(
            model = user.photo_url,
            contentDescription = null,
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceVariant),
            fallback = androidx.compose.ui.graphics.vector.rememberVectorPainter(Icons.Default.Person)
        )
        Spacer(Modifier.width(16.dp))
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
                ),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp)
            ) {
                Text("Add", fontSize = 12.sp)
            }
        }
    }
}
