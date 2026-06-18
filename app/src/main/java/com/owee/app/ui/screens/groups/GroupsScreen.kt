package com.owee.app.ui.screens.groups

import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Tune
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
import com.owee.app.data.remote.model.GroupWithMembers
import com.owee.app.ui.components.MemberAvatars
import com.owee.app.viewmodel.AuthViewModel
import com.owee.app.viewmodel.BalanceViewModel
import com.owee.app.viewmodel.GroupsViewModel
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GroupsScreen(
    authViewModel: AuthViewModel,
    groupsViewModel: GroupsViewModel,
    balanceViewModel: BalanceViewModel,
    onNavigateToCreate: () -> Unit,
    onNavigateToDetails: (String) -> Unit
) {
    val uiState by groupsViewModel.uiState.collectAsState()
    val balanceState by balanceViewModel.uiState.collectAsState()
    val user by authViewModel.user.collectAsState()

    var groupToDelete by remember { mutableStateOf<GroupWithMembers?>(null) }

    // Delete confirmation dialog
    groupToDelete?.let { group ->
        AlertDialog(
            onDismissRequest = { groupToDelete = null },
            title = { Text("Delete Group") },
            text = { Text("Are you sure you want to delete \"${group.group.name}\"? This cannot be undone.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        groupsViewModel.deleteGroup(group.group.id ?: "")
                        groupToDelete = null
                    }
                ) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { groupToDelete = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    PullToRefreshBox(
        isRefreshing = uiState.isLoading,
        onRefresh = { 
            groupsViewModel.refresh()
            balanceViewModel.refresh()
        },
        modifier = Modifier.fillMaxSize()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
        ) {
            // Summary Header (Clean, Matching Home Style)
            GroupsSummaryHeader(balanceState)

            // Search Bar (Pill style matching PeopleScreen)
            GroupsSearchBar(
                query = uiState.searchQuery,
                onQueryChange = { groupsViewModel.onSearchQueryChanged(it) }
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "My Groups",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = "${uiState.groups.size} TOTAL",
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.Bold,
                        fontSize = 10.sp
                    )
                }
            }

            if (uiState.isLoading && uiState.groups.isEmpty()) {
                Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                }
            } else if (uiState.groups.isEmpty()) {
                EmptyGroupsState()
            } else {
                val filteredGroups = remember(uiState.groups, uiState.searchQuery) {
                    if (uiState.searchQuery.isBlank()) uiState.groups
                    else uiState.groups.filter { it.group.name.contains(uiState.searchQuery, ignoreCase = true) }
                }

                LazyColumn(
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(bottom = 80.dp)
                ) {
                    items(filteredGroups, key = { it.group.id ?: "" }) { groupWithMembers ->
                        val groupBalance = balanceState.overallBalance?.groupBalances?.find { it.groupId == groupWithMembers.group.id }
                        val userBalance = groupBalance?.balances?.find { it.userId == user.dbId }?.amount ?: 0.0

                        GroupListItem(
                            groupWithMembers = groupWithMembers,
                            balanceAmount = userBalance,
                            onClick = {
                                groupsViewModel.selectGroup(groupWithMembers)
                                onNavigateToDetails(groupWithMembers.group.id ?: "")
                            },
                            onDeleteClick = { groupToDelete = groupWithMembers }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun GroupsSummaryHeader(balanceState: com.owee.app.viewmodel.BalanceUiState) {
    val overall = balanceState.overallBalance
    val totalOwed = overall?.totalOwed ?: 0.0
    val totalOwe = overall?.totalOwe ?: 0.0
    val isDark = isSystemInDarkTheme()

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isDark) MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isDark) 0.dp else 2.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                text = "TOTAL GROUP BALANCE",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.5.sp
            )
            
            Spacer(Modifier.height(8.dp))
            
            val total = totalOwed - totalOwe
            Text(
                text = String.format(Locale.US, "₹%.2f", total),
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.ExtraBold,
                color = when {
                    total > 0.01 -> MaterialTheme.colorScheme.secondary
                    total < -0.01 -> MaterialTheme.colorScheme.error
                    else -> MaterialTheme.colorScheme.onSurface
                }
            )
            
            Spacer(Modifier.height(16.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                SummarySubCard(
                    title = "YOU'RE OWED",
                    amount = totalOwed,
                    color = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.weight(1f)
                )
                SummarySubCard(
                    title = "YOU OWE",
                    amount = totalOwe,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun SummarySubCard(title: String, amount: Double, color: Color, modifier: Modifier) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                fontWeight = FontWeight.Bold,
                fontSize = 9.sp
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = String.format(Locale.US, "₹%.2f", amount),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = color
            )
        }
    }
}

@Composable
private fun GroupsSearchBar(query: String, onQueryChange: (String) -> Unit) {
    TextField(
        value = query,
        onValueChange = onQueryChange,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 8.dp),
        placeholder = {
            Text(
                "Search groups...",
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
private fun GroupListItem(
    groupWithMembers: GroupWithMembers,
    balanceAmount: Double,
    onClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    var menuExpanded by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = onClick,
                onLongClick = {
                    if (groupWithMembers.currentUserRole == "owner") {
                        menuExpanded = true
                    }
                }
            )
            .padding(horizontal = 20.dp, vertical = 12.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            // Group Avatar - Show creator's photo
            val creator = groupWithMembers.members.find { it.id == groupWithMembers.group.created_by }
            val displayUser = creator ?: groupWithMembers.members.firstOrNull()

            Box(
                modifier = Modifier
                    .size(52.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                if (displayUser?.photo_url != null) {
                    AsyncImage(
                        model = displayUser.photo_url,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.Group,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(28.dp)
                    )
                }
            }

            Spacer(Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = groupWithMembers.group.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    
                    Spacer(Modifier.width(8.dp))
                    
                    // Role Pill
                    val isOwner = groupWithMembers.currentUserRole == "owner"
                    Surface(
                        color = if (isOwner) 
                            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f) 
                        else 
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = groupWithMembers.currentUserRole.uppercase(),
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Black,
                            fontSize = 8.sp,
                            color = if (isOwner) 
                                MaterialTheme.colorScheme.primary 
                            else 
                                MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                
                Spacer(Modifier.height(4.dp))

                MemberAvatars(
                    members = groupWithMembers.members,
                    avatarSize = 18.dp,
                    overlap = 6.dp
                )
            }

            Column(horizontalAlignment = Alignment.End) {
                val isOwed = balanceAmount > 0.01
                val isOwe = balanceAmount < -0.01
                
                val statusText = when {
                    isOwed -> "OWED"
                    isOwe -> "YOU OWE"
                    else -> "SETTLED UP"
                }
                
                val amountColor = when {
                    isOwed -> MaterialTheme.colorScheme.secondary
                    isOwe -> MaterialTheme.colorScheme.error
                    else -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                }
                
                Text(
                    text = if (balanceAmount != 0.0) "₹${String.format(Locale.US, "%.2f", kotlin.math.abs(balanceAmount))}" else "Settled",
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

        DropdownMenu(
            expanded = menuExpanded,
            onDismissRequest = { menuExpanded = false }
        ) {
            DropdownMenuItem(
                text = { Text("Delete Group", color = MaterialTheme.colorScheme.error) },
                leadingIcon = { Icon(Icons.Default.Delete, null, tint = MaterialTheme.colorScheme.error) },
                onClick = {
                    menuExpanded = false
                    onDeleteClick()
                }
            )
        }
    }
}

@Composable
private fun EmptyGroupsState() {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                imageVector = Icons.Default.Group,
                contentDescription = null,
                modifier = Modifier.size(72.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.2f)
            )
            Spacer(Modifier.height(16.dp))
            Text(
                text = "No groups yet",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = "Tap + to create a new group",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
            )
        }
    }
}
