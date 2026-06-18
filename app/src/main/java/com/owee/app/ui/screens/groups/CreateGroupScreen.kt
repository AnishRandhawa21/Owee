package com.owee.app.ui.screens.groups

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
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
import com.owee.app.data.remote.model.User
import com.owee.app.viewmodel.GroupsViewModel

@Composable
fun CreateGroupScreen(
    groupsViewModel: GroupsViewModel,
    onGroupCreated: () -> Unit
) {
    val uiState by groupsViewModel.uiState.collectAsState()

    // Refresh friends list when screen is opened
    LaunchedEffect(Unit) {
        groupsViewModel.refreshFriends()
    }

    LaunchedEffect(uiState.message) {
        if (uiState.message == "Group created!") {
            groupsViewModel.clearMessage()
            onGroupCreated()
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
        ) {
            // Group Identity Section
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Surface(
                        modifier = Modifier.size(80.dp),
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                Icons.Default.Group, 
                                null, 
                                modifier = Modifier.size(40.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                    Spacer(Modifier.height(16.dp))
                    TextField(
                        value = uiState.groupNameInput,
                        onValueChange = { groupsViewModel.onGroupNameChanged(it) },
                        placeholder = { Text("Enter group name", color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)) },
                        modifier = Modifier.fillMaxWidth(),
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent,
                            focusedIndicatorColor = MaterialTheme.colorScheme.primary,
                            unfocusedIndicatorColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                        ),
                        textStyle = LocalTextStyle.current.copy(
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        ),
                        singleLine = true
                    )
                }
            }

            // Friend Selection Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Add members",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                if (uiState.selectedFriends.isNotEmpty()) {
                    Surface(
                        color = MaterialTheme.colorScheme.primaryContainer,
                        shape = CircleShape
                    ) {
                        Text(
                            text = "${uiState.selectedFriends.size} selected",
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // Friends List
            if (uiState.friends.isEmpty()) {
                Box(
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "Add friends first to create a group",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(bottom = 80.dp)
                ) {
                    items(uiState.friends, key = { it.id ?: "" }) { friend ->
                        FriendSelectListItem(
                            friend = friend,
                            isSelected = friend.id in uiState.selectedFriends,
                            onToggle = { friend.id?.let { groupsViewModel.onFriendSelectionToggled(it) } }
                        )
                    }
                }
            }
        }

        // Bottom Button
        val canCreate = uiState.groupNameInput.isNotBlank() && uiState.selectedFriends.isNotEmpty()
        Button(
            onClick = { groupsViewModel.createGroup() },
            enabled = canCreate && !uiState.isCreating,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(20.dp)
                .height(56.dp),
            shape = RoundedCornerShape(16.dp)
        ) {
            if (uiState.isCreating) {
                CircularProgressIndicator(modifier = Modifier.size(24.dp), color = MaterialTheme.colorScheme.onPrimary)
            } else {
                Text("Create Group", fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun FriendSelectListItem(
    friend: User,
    isSelected: Boolean,
    onToggle: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onToggle() }
            .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AsyncImage(
            model = friend.photo_url,
            contentDescription = null,
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceVariant),
            contentScale = ContentScale.Crop,
            fallback = androidx.compose.ui.graphics.vector.rememberVectorPainter(Icons.Default.Person)
        )

        Spacer(Modifier.width(16.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = friend.name,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "@${friend.username}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
            )
        }

        Box(
            modifier = Modifier
                .size(24.dp)
                .clip(CircleShape)
                .background(
                    if (isSelected) MaterialTheme.colorScheme.primary
                    else Color.Transparent
                )
                .border(
                    width = if (isSelected) 0.dp else 1.5.dp,
                    color = if (isSelected) Color.Transparent else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                    shape = CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            if (isSelected) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}
