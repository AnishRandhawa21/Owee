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

    private var currentUserId: String? = null

    private val _uiState = MutableStateFlow(FriendsUiState())
    val uiState: StateFlow<FriendsUiState> = _uiState.asStateFlow()

    init {
        // Load from local cache immediately on startup
        _uiState.update { it.copy(
            friends = repository.getCachedFriends(),
            sentRequests = repository.getCachedSentRequests(),
            sentRequestIds = repository.getCachedSentRequestIds()
        ) }
    }

    fun searchUsers(query: String) {
        _uiState.update { it.copy(searchQuery = query) }

        if (query.isBlank()) {
            _uiState.update { it.copy(searchResults = emptyList()) }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val results = repository.searchUsers(query)
            _uiState.update { it.copy(
                searchResults = results,
                isLoading = false
            ) }
        }
    }

    fun loadPendingRequests(currentUserId: String?) {
        if (currentUserId.isNullOrBlank()) return
        viewModelScope.launch {
            val requests = repository.getPendingRequests(currentUserId)
            _uiState.update { it.copy(friendRequests = requests) }
        }
    }

    fun loadFriends(currentUserId: String?) {
        if (currentUserId.isNullOrBlank()) return
        viewModelScope.launch {
            val friends = repository.getFriends(currentUserId)
            _uiState.update { it.copy(friends = friends) }
        }
    }

    fun sendFriendRequest(senderId: String?, receiverUser: User) {
        val receiverId = receiverUser.id
        if (senderId.isNullOrBlank() || receiverId.isNullOrBlank()) return
        
        // Prevent duplicate clicks or adding if already exists
        if (_uiState.value.sentRequestIds.contains(receiverId)) return

        // Optimistic update with deduplication
        _uiState.update { state ->
            state.copy(
                sentRequestIds = state.sentRequestIds + receiverId,
                sentRequests = (state.sentRequests + receiverUser).distinctBy { it.id }
            )
        }

        viewModelScope.launch {
            val success = repository.sendFriendRequest(senderId, receiverId, receiverUser)
            if (success) {
                _uiState.update { it.copy(message = "Friend request sent!") }
            } else {
                // Rollback if failed
                _uiState.update { it.copy(
                    error = "Failed to send request",
                    sentRequestIds = it.sentRequestIds - receiverId,
                    sentRequests = it.sentRequests.filter { it.id != receiverId }
                ) }
            }
        }
    }

    fun acceptRequest(request: FriendRequestUi, currentUserId: String?) {
        if (currentUserId.isNullOrBlank()) return
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
            } else {
                _uiState.update { it.copy(error = "Failed to accept request") }
            }
        }
    }

    fun rejectRequest(requestId: String, currentUserId: String?) {
        if (currentUserId.isNullOrBlank()) return
        viewModelScope.launch {
            val success = repository.rejectFriendRequest(requestId)
            if (success) {
                _uiState.update { state ->
                    state.copy(
                        message = "Request rejected",
                        friendRequests = state.friendRequests.filter { it.requestId != requestId }
                    )
                }
            } else {
                _uiState.update { it.copy(error = "Failed to reject request") }
            }
        }
    }

    fun clearMessage() {
        _uiState.update { it.copy(message = null, error = null) }
    }

    fun initialize(userId: String?) {
        if (userId.isNullOrBlank()) return
        
        // If already initialized for this user, don't re-run full init
        if (currentUserId == userId) return

        currentUserId = userId

        // Background sync
        loadPendingRequests(userId)
        loadSentRequests(userId)
        loadFriends(userId)
        startRealtimeSubscriptions(userId)
    }

    private fun loadSentRequests(userId: String) {
        viewModelScope.launch {
            val sent = repository.getSentRequests(userId)
            _uiState.update { it.copy(
                sentRequests = sent,
                sentRequestIds = sent.mapNotNull { it.id }.toSet()
            ) }
        }
    }

    private fun startRealtimeSubscriptions(userId: String) {
        viewModelScope.launch {
            try {
                realtimeManager.connect()

                val requestFlow = realtimeManager.watchFriendRequests()
                val friendsFlow = realtimeManager.watchFriends()

                realtimeManager.subscribe()

                requestFlow?.let { flow ->
                    launch {
                        flow.collect { action -> handleFriendRequestAction(action, userId) }
                    }
                }

                friendsFlow?.let { flow ->
                    launch {
                        flow.collect { action -> handleFriendAction(action, userId) }
                    }
                }

            } catch (e: Exception) {
                // Silently ignore or log - don't crash the UI state
            }
        }
    }

    private suspend fun handleFriendRequestAction(action: PostgresAction, userId: String) {
        when (action) {
            is PostgresAction.Insert -> {
                val request = action.decodeRecord<FriendRequest>()
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
                            else state.copy(friendRequests = (state.friendRequests + uiRequest).distinctBy { it.requestId })
                        }
                    }
                }
                if (request.sender_id == userId && request.status == "pending") {
                    val receiver = repository.getUserById(request.receiver_id)
                    if (receiver != null) {
                        _uiState.update { state ->
                             state.copy(
                                sentRequestIds = state.sentRequestIds + request.receiver_id,
                                sentRequests = (state.sentRequests + receiver).distinctBy { it.id }
                            )
                        }
                    }
                }
            }
            is PostgresAction.Update -> {
                val request = action.decodeRecord<FriendRequest>()
                if (request.receiver_id == userId && request.status != "pending") {
                    _uiState.update { state ->
                        state.copy(friendRequests = state.friendRequests.filter { it.requestId != request.id })
                    }
                }
                if (request.sender_id == userId && request.status != "pending") {
                    _uiState.update { state ->
                        repository.removeSentRequest(request.receiver_id)
                        state.copy(
                            sentRequestIds = state.sentRequestIds - request.receiver_id,
                            sentRequests = state.sentRequests.filter { it.id != request.receiver_id }
                        )
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
                            else {
                                val updatedFriends = (state.friends + friendProfile).distinctBy { it.id }
                                repository.getFriends(userId) // Update cache in background
                                state.copy(friends = updatedFriends)
                            }
                        }
                    }
                }
            }
            is PostgresAction.Delete -> {
                loadFriends(userId)
            }
            else -> {}
        }
    }

    override fun onCleared() {
        super.onCleared()
        viewModelScope.launch { realtimeManager.disconnect() }
    }
}
