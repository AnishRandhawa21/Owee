package com.owee.app.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.owee.app.data.realtime.GroupRealtimeManager
import com.owee.app.data.remote.model.GroupMember
import com.owee.app.data.remote.model.GroupWithMembers
import com.owee.app.data.repository.FriendRepository
import com.owee.app.data.repository.GroupRepository
import io.github.jan.supabase.realtime.PostgresAction
import io.github.jan.supabase.realtime.decodeRecord
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class GroupsViewModel(
    private val repository: GroupRepository = GroupRepository(),
    private val friendRepository: FriendRepository = FriendRepository(),
    private val realtimeManager: GroupRealtimeManager = GroupRealtimeManager(),
) : ViewModel() {

    private var currentUserId: String? = null

    private val _uiState = MutableStateFlow(GroupUiState())
    val uiState: StateFlow<GroupUiState> = _uiState.asStateFlow()

    private var loadGroupsJob: Job? = null

    // ─── Init ─────────────────────────────────────────────────────────────────

    fun initialize(userId: String?) {
        if (userId.isNullOrBlank()) return
        if (currentUserId == userId) return

        currentUserId = userId

        loadGroups(userId)
        loadFriends(userId)
        startRealtimeSubscriptions(userId)
    }

    fun refresh() {
        currentUserId?.let { loadGroups(it) }
    }

    // ─── Load ─────────────────────────────────────────────────────────────────

    private fun loadGroups(userId: String, isSilent: Boolean = false) {
        loadGroupsJob?.cancel()
        loadGroupsJob = viewModelScope.launch {
            if (!isSilent) _uiState.update { it.copy(isLoading = true) }
            val groups = repository.getGroupsForUser(userId)
            Log.d("GroupsViewModel", "Update state with ${groups.size} groups")
            _uiState.update { it.copy(groups = groups, isLoading = false) }
        }
    }

    fun refreshFriends() {
        currentUserId?.let { loadFriends(it) }
    }

    private fun loadFriends(userId: String) {
        viewModelScope.launch {
            val friends = friendRepository.getFriends(userId)
            _uiState.update { it.copy(friends = friends) }
        }
    }

    // ─── Create Group ─────────────────────────────────────────────────────────

    fun onGroupNameChanged(name: String) {
        _uiState.update { it.copy(groupNameInput = name) }
    }

    fun onSearchQueryChanged(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
    }

    fun onFriendSelectionToggled(friendId: String) {
        _uiState.update { state ->
            val updated = state.selectedFriends.toMutableSet()
            if (friendId in updated) updated.remove(friendId) else updated.add(friendId)
            state.copy(selectedFriends = updated)
        }
    }

    fun createGroup() {
        val userId = currentUserId ?: return
        val name = _uiState.value.groupNameInput.trim()
        val selectedIds = _uiState.value.selectedFriends.toList()

        if (name.isBlank() || selectedIds.isEmpty()) return

        viewModelScope.launch {
            _uiState.update { it.copy(isCreating = true) }

            val group = repository.createGroup(
                name = name,
                creatorId = userId,
                memberIds = selectedIds
            )

            if (group != null) {
                // Optimistic: reload groups to get full GroupWithMembers
                val updated = repository.getGroupsForUser(userId)
                _uiState.update {
                    it.copy(
                        groups = updated,
                        isCreating = false,
                        groupNameInput = "",
                        selectedFriends = emptySet(),
                        message = "Group created!"
                    )
                }
            } else {
                _uiState.update {
                    it.copy(
                        isCreating = false,
                        error = "Failed to create group"
                    )
                }
            }
        }
    }

    // ─── Group Details ────────────────────────────────────────────────────────

    fun selectGroup(groupWithMembers: GroupWithMembers) {
        _uiState.update { it.copy(selectedGroup = groupWithMembers) }
    }

    // ─── Realtime ─────────────────────────────────────────────────────────────

    private fun startRealtimeSubscriptions(userId: String) {
        viewModelScope.launch {
            try {
                realtimeManager.connect()

                val groupsFlow = realtimeManager.watchGroups()
                val membersFlow = realtimeManager.watchGroupMembers()

                realtimeManager.subscribe()

                launch {
                    groupsFlow.collect { action -> handleGroupAction(action, userId) }
                }

                launch {
                    membersFlow.collect { action -> handleMemberAction(action, userId) }
                }

            } catch (_: Exception) {
                // Don't crash UI — realtime is best-effort
            }
        }
    }

    private suspend fun handleGroupAction(action: PostgresAction, userId: String) {
        Log.d("GroupsViewModel", "Realtime: Group action $action for user $userId")
        when (action) {
            is PostgresAction.Delete -> {
                // Only reload on delete. For Insert, we wait for Member Insert
                loadGroups(userId, isSilent = true)
            }
            else -> {}
        }
    }

    private suspend fun handleMemberAction(action: PostgresAction, userId: String) {
        Log.d("GroupsViewModel", "Realtime: Member action $action for user $userId")
        when (action) {
            is PostgresAction.Insert -> {
                try {
                    val member = action.decodeRecord<GroupMember>()
                    val isMe = member.user_id == userId
                    val isExistingGroup = _uiState.value.groups.any { it.group.id == member.group_id }
                    
                    Log.d("GroupsViewModel", "Realtime: Member added. isMe=$isMe, isExistingGroup=$isExistingGroup")
                    
                    if (isMe || isExistingGroup) {
                        Log.d("GroupsViewModel", "Realtime: Relevant member insert! Reloading in 1s...")
                        kotlinx.coroutines.delay(1000L)
                        loadGroups(userId, isSilent = true)
                    }
                } catch (e: Exception) {
                    Log.e("GroupsViewModel", "Realtime: Error decoding member", e)
                    loadGroups(userId, isSilent = true)
                }
            }
            is PostgresAction.Delete -> {
                loadGroups(userId, isSilent = true)
            }
            else -> {}
        }
    }

    // ─── Misc ─────────────────────────────────────────────────────────────────

    fun clearMessage() {
        _uiState.update { it.copy(message = null, error = null) }
    }

    override fun onCleared() {
        super.onCleared()
        viewModelScope.launch { realtimeManager.disconnect() }
    }

    fun deleteGroup(groupId: String) {
        if (currentUserId == null) return
        viewModelScope.launch {
            val success = repository.deleteGroup(groupId)
            if (success) {
                _uiState.update { state ->
                    state.copy(
                        groups = state.groups.filter { it.group.id != groupId },
                        selectedGroup = null,
                        message = "Group deleted"
                    )
                }
            } else {
                _uiState.update { it.copy(error = "Failed to delete group") }
            }
        }
    }
}