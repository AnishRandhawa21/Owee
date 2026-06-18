package com.owee.app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.owee.app.data.realtime.GroupRealtimeManager
import com.owee.app.data.remote.model.GroupMember
import com.owee.app.data.remote.model.GroupWithMembers
import com.owee.app.data.repository.FriendRepository
import com.owee.app.data.repository.GroupRepository
import io.github.jan.supabase.realtime.PostgresAction
import io.github.jan.supabase.realtime.decodeRecord
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class GroupsViewModel(
    private val repository: GroupRepository = GroupRepository(),
    private val friendRepository: FriendRepository = FriendRepository(),
    private val realtimeManager: GroupRealtimeManager = GroupRealtimeManager()
) : ViewModel() {

    private var currentUserId: String? = null

    private val _uiState = MutableStateFlow(GroupUiState())
    val uiState: StateFlow<GroupUiState> = _uiState.asStateFlow()

    // ─── Init ─────────────────────────────────────────────────────────────────

    fun initialize(userId: String?) {
        if (userId.isNullOrBlank()) return
        if (currentUserId == userId) return

        currentUserId = userId

        loadGroups(userId)
        loadFriends(userId)
        startRealtimeSubscriptions(userId)
    }

    // ─── Load ─────────────────────────────────────────────────────────────────

    private fun loadGroups(userId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val groups = repository.getGroupsForUser(userId)
            _uiState.update { it.copy(groups = groups, isLoading = false) }
        }
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

    fun resetCreateForm() {
        _uiState.update { it.copy(groupNameInput = "", selectedFriends = emptySet()) }
    }

    // ─── Group Details ────────────────────────────────────────────────────────

    fun selectGroup(groupWithMembers: GroupWithMembers) {
        _uiState.update { it.copy(selectedGroup = groupWithMembers) }
    }

    fun clearSelectedGroup() {
        _uiState.update { it.copy(selectedGroup = null) }
    }

    // ─── Realtime ─────────────────────────────────────────────────────────────

    private fun startRealtimeSubscriptions(userId: String) {
        viewModelScope.launch {
            try {
                realtimeManager.connect()

                val groupsFlow = realtimeManager.watchGroups()
                val membersFlow = realtimeManager.watchGroupMembers()

                realtimeManager.subscribe()

                groupsFlow?.let { flow ->
                    launch {
                        flow.collect { action -> handleGroupAction(action, userId) }
                    }
                }

                membersFlow?.let { flow ->
                    launch {
                        flow.collect { action -> handleMemberAction(action, userId) }
                    }
                }

            } catch (e: Exception) {
                // Don't crash UI — realtime is best-effort
            }
        }
    }

    private suspend fun handleGroupAction(action: PostgresAction, userId: String) {
        when (action) {
            is PostgresAction.Insert -> {
                val updated = repository.getGroupsForUser(userId)
                _uiState.update { it.copy(groups = updated) }
            }
            is PostgresAction.Delete -> {
                // On delete, just reload — don't try to decode the deleted record
                val updated = repository.getGroupsForUser(userId)
                _uiState.update { it.copy(groups = updated) }
            }
            else -> {}
        }
    }

    private suspend fun handleMemberAction(action: PostgresAction, userId: String) {
        when (action) {
            is PostgresAction.Insert -> {
                val member = action.decodeRecord<GroupMember>()
                // Only refresh if it affects a group the current user is in
                val affected = _uiState.value.groups.any { it.group.id == member.group_id }
                if (affected) {
                    val updated = repository.getGroupsForUser(userId)
                    _uiState.update { it.copy(groups = updated) }
                }
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
        val userId = currentUserId ?: return
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