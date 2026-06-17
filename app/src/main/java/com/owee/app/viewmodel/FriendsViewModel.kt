package com.owee.app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.owee.app.data.realtime.FriendRealtimeManager
import com.owee.app.data.remote.model.Friend
import com.owee.app.data.remote.model.FriendRequest
import com.owee.app.data.remote.model.FriendRequestUi
import com.owee.app.data.remote.model.User
import com.owee.app.data.repository.FriendRepository
import io.github.jan.supabase.realtime.PostgresAction
import io.github.jan.supabase.realtime.decodeRecord
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.serialization.json.jsonPrimitive

class FriendsViewModel(
    private val repository: FriendRepository = FriendRepository(),
    private val realtimeManager: FriendRealtimeManager = FriendRealtimeManager()
) : ViewModel() {


    private var isInitialized = false
    private var currentUserId: String? = null

    private val _uiState = MutableStateFlow(FriendsUiState())
    val uiState: StateFlow<FriendsUiState> =
        _uiState.asStateFlow()

    fun searchUsers(query: String) {
        _uiState.value = _uiState.value.copy(searchQuery = query)

        if (query.isBlank()) {
            _uiState.value = _uiState.value.copy(searchResults = emptyList())
            return
        }

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            val results = repository.searchUsers(query)
            _uiState.value = _uiState.value.copy(
                searchResults = results,
                isLoading = false
            )
        }
    }

    fun loadPendingRequests(currentUserId: String?) {
        if (currentUserId == null) return
        viewModelScope.launch {
            val requests = repository.getPendingRequests(currentUserId)
            _uiState.value = _uiState.value.copy(friendRequests = requests)
        }
    }

    fun loadFriends(currentUserId: String?) {
        if (currentUserId == null) return
        viewModelScope.launch {
            val friends = repository.getFriends(currentUserId)
            _uiState.value = _uiState.value.copy(friends = friends)
        }
    }

    fun sendFriendRequest(senderId: String?, receiverId: String?) {
        if (senderId == null || receiverId == null) return
        viewModelScope.launch {
            val success = repository.sendFriendRequest(senderId, receiverId)
            if (success) {
                _uiState.value = _uiState.value.copy(
                    message = "Friend request sent!",
                    sentRequestIds = _uiState.value.sentRequestIds + receiverId
                )
            } else {
                _uiState.value = _uiState.value.copy(error = "Failed to send request")
            }
        }
    }

    fun acceptRequest(request: FriendRequestUi, currentUserId: String?) {
        if (currentUserId == null) return
        viewModelScope.launch {
            val success = repository.acceptFriendRequest(
                requestId = request.requestId,
                senderId = request.senderId,
                receiverId = currentUserId
            )
            if (success) {
                _uiState.update { state ->
                    state.copy(
                        message = "Request accepted!",
                        friendRequests = state.friendRequests.filter { it.requestId != request.requestId }
                    )
                }
                // No need to call loadFriends() because Realtime handles it
            } else {
                _uiState.value = _uiState.value.copy(error = "Failed to accept request")
            }
        }
    }

    fun rejectRequest(requestId: String, currentUserId: String?) {
        if (currentUserId == null) return
        viewModelScope.launch {
            val success = repository.rejectFriendRequest(requestId)
            if (success) {
                _uiState.value = _uiState.value.copy(message = "Request rejected")
                _uiState.value = _uiState.value.copy(
                    friendRequests =
                        _uiState.value.friendRequests.filter {
                            it.requestId != requestId
                        }
                )
            } else {
                _uiState.value = _uiState.value.copy(error = "Failed to reject request")
            }
        }
    }

    fun clearMessage() {
        _uiState.value = _uiState.value.copy(message = null, error = null)
    }

    fun initialize(userId: String?) {

        if (userId == null) return

        if (isInitialized && currentUserId == userId) {
            return
        }

        currentUserId = userId
        isInitialized = true

        loadPendingRequests(userId)
        loadSentRequests(userId)
        loadFriends(userId)
        startRealtimeSubscriptions(userId)
    }

    private fun loadSentRequests(userId: String) {
        viewModelScope.launch {
            val sentIds = repository.getSentRequestIds(userId)
            _uiState.update { it.copy(sentRequestIds = sentIds) }
        }
    }

    private fun startRealtimeSubscriptions(userId: String) {
        viewModelScope.launch {
            try {
                realtimeManager.connect()

                // 1. Register filters BEFORE subscribing
                val requestFlow = realtimeManager.watchFriendRequests()
                val friendsFlow = realtimeManager.watchFriends()

                // 2. Subscribe to the channels
                realtimeManager.subscribe()

                // 3. Start collecting updates (only if registration was successful)
                requestFlow?.let { flow ->
                    launch {
                        flow.collect { action ->
                            handleFriendRequestAction(action, userId)
                        }
                    }
                }

                friendsFlow?.let { flow ->
                    launch {
                        flow.collect { action ->
                            handleFriendAction(action, userId)
                        }
                    }
                }

            } catch (e: Exception) {
                _uiState.update { it.copy(error = "Realtime connection failed") }
            }
        }
    }

    private suspend fun handleFriendRequestAction(action: PostgresAction, userId: String) {
        when (action) {
            is PostgresAction.Insert -> {
                val request = action.decodeRecord<FriendRequest>()
                // 1. Someone sent ME a request
                if (request.receiver_id == userId && request.status == "pending") {
                    val sender = repository.getUserById(request.sender_id)
                    if (sender != null) {
                        val uiRequest = FriendRequestUi(
                            requestId = request.id ?: "",
                            senderId = sender.id ?: "",
                            senderName = sender.name,
                            senderUsername = sender.username
                        )
                        _uiState.update { state ->
                            if (state.friendRequests.any { it.requestId == uiRequest.requestId }) state
                            else state.copy(friendRequests = state.friendRequests + uiRequest)
                        }
                    }
                }
                // 2. I sent someone else a request (Sync other devices)
                if (request.sender_id == userId && request.status == "pending") {
                    _uiState.update { it.copy(sentRequestIds = it.sentRequestIds + request.receiver_id) }
                }
            }
            is PostgresAction.Update -> {
                val request = action.decodeRecord<FriendRequest>()
                // 1. Incoming request was processed
                if (request.receiver_id == userId && request.status != "pending") {
                    _uiState.update { state ->
                        state.copy(friendRequests = state.friendRequests.filter { it.requestId != request.id })
                    }
                }
                // 2. Outgoing request was Accepted/Rejected by the other user
                if (request.sender_id == userId && request.status != "pending") {
                    _uiState.update { state ->
                        state.copy(sentRequestIds = state.sentRequestIds - request.receiver_id)
                    }
                }
            }
            else -> {}
        }
    }

    private suspend fun handleFriendAction(action: PostgresAction, userId: String) {
        when (action) {
            is PostgresAction.Insert -> {
                val friend = action.decodeRecord<Friend>()
                if (friend.user_one == userId || friend.user_two == userId) {
                    val friendId = if (friend.user_one == userId) friend.user_two else friend.user_one
                    val friendProfile = repository.getUserById(friendId)
                    if (friendProfile != null) {
                        _uiState.update { state ->
                            if (state.friends.any { it.id == friendProfile.id }) state
                            else state.copy(friends = state.friends + friendProfile)
                        }
                    }
                }
            }
            is PostgresAction.Delete -> {
                // For Delete, the record is usually empty in older Supabase versions, 
                // but 3.2.5 might provide the old_record if configured.
                // However, the simplest is to reload friends or use the id if available.
                val oldRecord = action.oldRecord
                val deletedId = oldRecord["id"]?.jsonPrimitive?.content
                if (deletedId != null) {
                    // We don't know which user was the friend from just the record ID easily without the full record,
                    // but we can just reload the list for consistency on delete.
                    loadFriends(userId)
                }
            }
            else -> {}
        }
    }

    override fun onCleared() {
        super.onCleared()
        viewModelScope.launch {
            realtimeManager.disconnect()
        }
    }
}
