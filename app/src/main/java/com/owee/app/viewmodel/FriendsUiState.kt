package com.owee.app.viewmodel

import com.owee.app.data.remote.model.FriendRequestUi
import com.owee.app.data.remote.model.User

data class FriendsUiState(
    val searchQuery: String = "",
    val searchResults: List<User> = emptyList(),
    val friendRequests: List<FriendRequestUi> = emptyList(), // Incoming
    val sentRequests: List<User> = emptyList(),             // Outgoing
    val friends: List<User> = emptyList(),
    val sentRequestIds: Set<String> = emptySet(),           // For quick lookup
    val isLoading: Boolean = false,
    val error: String? = null,
    val message: String? = null
)
