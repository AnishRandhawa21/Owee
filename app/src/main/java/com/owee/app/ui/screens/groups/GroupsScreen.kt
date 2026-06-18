package com.owee.app.ui.screens.groups

import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.owee.app.data.remote.model.GroupWithMembers
import com.owee.app.ui.components.MemberAvatars
import com.owee.app.viewmodel.AuthViewModel
import com.owee.app.viewmodel.BalanceViewModel
import com.owee.app.viewmodel.GroupsViewModel
import java.util.*

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

    LaunchedEffect(user.dbId) {
        groupsViewModel.initialize(user.dbId?.ifBlank { null })
    }

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

    Box(modifier = Modifier.fillMaxSize()) {

        when {
            uiState.isLoading -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }

            uiState.groups.isEmpty() -> {
                EmptyGroupsState()
            }

            else -> {
                val filteredGroups = remember(uiState.groups, uiState.searchQuery) {
                    if (uiState.searchQuery.isBlank()) uiState.groups
                    else uiState.groups.filter { it.group.name.contains(uiState.searchQuery, ignoreCase = true) }
                }

                Column(modifier = Modifier.fillMaxSize()) {
                    // Summary Section (Matching image)
                    GroupsSummaryHeader(balanceState)

                    // Search Bar
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = uiState.searchQuery,
                            onValueChange = { groupsViewModel.onSearchQueryChanged(it) },
                            modifier = Modifier
                                .weight(1f)
                                .heightIn(min = 52.dp),
                            placeholder = { Text("Search groups...") },
                            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                            shape = RoundedCornerShape(24.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                                focusedBorderColor = Color.Transparent,
                                unfocusedBorderColor = Color.Transparent
                            ),
                            singleLine = true
                        )
                        Spacer(Modifier.width(8.dp))
                        IconButton(
                            onClick = { /* Filter */ },
                            modifier = Modifier
                                .size(52.dp)
                                .background(
                                    MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f),
                                    RoundedCornerShape(12.dp)
                                )
                        ) {
                            Icon(Icons.Default.Tune, contentDescription = "Filter", tint = MaterialTheme.colorScheme.primary)
                        }
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Active Groups",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    LazyColumn(
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        items(filteredGroups, key = { it.group.id ?: "" }) { groupWithMembers ->
                            val groupBalance = balanceState.overallBalance?.groupBalances?.find { it.groupId == groupWithMembers.group.id }
                            val userBalance = groupBalance?.balances?.find { it.userId == user.dbId }?.amount ?: 0.0

                            GroupCard(
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
}

@Composable
private fun GroupsSummaryHeader(balanceState: com.owee.app.viewmodel.BalanceUiState) {
    val overall = balanceState.overallBalance
    val totalReceivable = overall?.totalOwed ?: 0.0
    val totalOwe = overall?.totalOwe ?: 0.0
    val totalOwed = overall?.totalOwed ?: 0.0

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSystemInDarkTheme()) MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isSystemInDarkTheme()) 0.dp else 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            Text(
                text = "TOTAL RECEIVABLE",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = String.format(Locale.US, "₹%.2f", totalReceivable),
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            
            Spacer(Modifier.height(12.dp))
            
            Row(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "YOU OWE",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = String.format(Locale.US, "₹%.2f", totalOwe),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "YOU ARE OWED",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = String.format(Locale.US, "₹%.2f", totalOwed),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.secondary
                    )
                }
            }
        }
    }
}

@Composable
private fun GroupCard(
    groupWithMembers: GroupWithMembers,
    balanceAmount: Double,
    onClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    var menuExpanded by remember { mutableStateOf(false) }

    Box {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .combinedClickable(
                    onClick = onClick,
                    onLongClick = {
                        if (groupWithMembers.currentUserRole == "owner") {
                            menuExpanded = true
                        }
                    }
                ),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(
                containerColor = if (isSystemInDarkTheme()) MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.surface
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // First Member Avatar as Group Icon
                val firstMember = groupWithMembers.members.firstOrNull()
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    if (firstMember?.photo_url != null) {
                        AsyncImage(
                            model = firstMember.photo_url,
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.Group,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }

                Spacer(Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = groupWithMembers.group.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    
                    Spacer(Modifier.height(4.dp))
                    
                    MemberAvatars(
                        members = groupWithMembers.members,
                        avatarSize = 20.dp,
                        overlap = 6.dp
                    )
                }

                // Balance Info
                Column(horizontalAlignment = Alignment.End) {
                    when {
                        balanceAmount > 0 -> {
                            Text(
                                text = "OWED",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = String.format(Locale.US, "₹%.2f", balanceAmount),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.secondary // Green-ish from theme
                            )
                        }
                        balanceAmount < 0 -> {
                            Text(
                                text = "YOU OWE",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = String.format(Locale.US, "₹%.2f", -balanceAmount),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                        else -> {
                            Text(
                                text = "Settled up",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                            )
                        }
                    }
                }
            }
        }

        // Dropdown menu on long press
        DropdownMenu(
            expanded = menuExpanded,
            onDismissRequest = { menuExpanded = false }
        ) {
            DropdownMenuItem(
                text = {
                    Text(
                        text = "Delete Group",
                        color = MaterialTheme.colorScheme.error
                    )
                },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error
                    )
                },
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
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
            )
            Spacer(Modifier.height(16.dp))
            Text(
                text = "No groups yet",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = "Tap + to create your first group",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
            )
        }
    }
}
