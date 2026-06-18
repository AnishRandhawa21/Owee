package com.owee.app.viewmodel

import com.owee.app.data.remote.model.Group
import com.owee.app.data.remote.model.GroupWithMembers
import com.owee.app.data.remote.model.User

data class GroupUiState(
    val groups: List<GroupWithMembers> = emptyList(),
    val selectedGroup: GroupWithMembers? = null,
    val friends: List<User> = emptyList(),
    val selectedFriends: Set<String> = emptySet(),
    val groupNameInput: String = "",
    val searchQuery: String = "",
    val isLoading: Boolean = false,
    val isCreating: Boolean = false,
    val error: String? = null,
    val message: String? = null
)