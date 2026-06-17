package com.owee.app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.owee.app.data.realtime.ExpenseRealtimeManager
import com.owee.app.data.remote.model.ExpenseWithParticipants
import com.owee.app.data.remote.model.User
import com.owee.app.data.repository.ExpenseRepository
import io.github.jan.supabase.realtime.PostgresAction
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class ExpensesViewModel(
    private val repository: ExpenseRepository = ExpenseRepository(),
    private val realtimeManager: ExpenseRealtimeManager = ExpenseRealtimeManager()
) : ViewModel() {

    private var currentGroupId: String? = null
    private var currentUserId: String? = null
    private var groupMembers: List<User> = emptyList()

    private val _uiState = MutableStateFlow(ExpenseUiState())
    val uiState: StateFlow<ExpenseUiState> = _uiState.asStateFlow()

    // ─── Init ─────────────────────────────────────────────────────────────────

    fun initialize(
        groupId: String,
        currentUserId: String,
        members: List<User>
    ) {
        if (groupId.isBlank() || currentUserId.isBlank()) return
        if (currentGroupId == groupId) return

        currentGroupId = groupId
        this.currentUserId = currentUserId
        this.groupMembers = members

        // Auto-include current user as participant
        _uiState.update {
            it.copy(selectedParticipantIds = setOf(currentUserId))
        }

        loadExpenses(groupId, members)
        startRealtimeSubscriptions(groupId, members)
    }

    // ─── Load ─────────────────────────────────────────────────────────────────

    private fun loadExpenses(groupId: String, members: List<User>) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val expenses = repository.getExpensesForGroup(groupId)
            val balances = repository.calculateBalances(members, expenses)
            _uiState.update {
                it.copy(
                    expenses = expenses,
                    memberBalances = balances,
                    isLoading = false
                )
            }
        }
    }

    // ─── Create Form ──────────────────────────────────────────────────────────

    fun onTitleChanged(title: String) {
        _uiState.update { it.copy(titleInput = title) }
    }

    fun onAmountChanged(amount: String) {
        _uiState.update { state ->
            // Recalculate equal shares when amount changes
            val parsed = amount.toDoubleOrNull() ?: 0.0
            val updatedCustom = if (state.splitType == "equal") {
                recalculateEqualAmounts(
                    state.selectedParticipantIds,
                    parsed
                )
            } else {
                state.customAmounts
            }
            state.copy(amountInput = amount, customAmounts = updatedCustom)
        }
    }

    fun onParticipantToggled(userId: String) {
        val currentUserId = this.currentUserId ?: return

        _uiState.update { state ->
            // Current user cannot be removed
            if (userId == currentUserId) return@update state

            val updated = state.selectedParticipantIds.toMutableSet()
            if (userId in updated) updated.remove(userId) else updated.add(userId)

            val amount = state.amountInput.toDoubleOrNull() ?: 0.0

            // Recalculate if equal split
            val updatedCustom = if (state.splitType == "equal") {
                recalculateEqualAmounts(updated, amount)
            } else {
                // Remove custom amount for deselected user
                state.customAmounts.toMutableMap().also {
                    if (userId !in updated) it.remove(userId)
                }
            }

            state.copy(
                selectedParticipantIds = updated,
                customAmounts = updatedCustom
            )
        }
    }

    fun onSplitTypeChanged(type: String) {
        _uiState.update { state ->
            val amount = state.amountInput.toDoubleOrNull() ?: 0.0
            val updatedCustom = if (type == "equal") {
                recalculateEqualAmounts(state.selectedParticipantIds, amount)
            } else {
                // Seed custom amounts with equal split as starting point
                recalculateEqualAmounts(state.selectedParticipantIds, amount)
            }
            state.copy(splitType = type, customAmounts = updatedCustom)
        }
    }

    fun onCustomAmountChanged(userId: String, amount: String) {
        _uiState.update { state ->
            val updated = state.customAmounts.toMutableMap()
            updated[userId] = amount
            state.copy(customAmounts = updated)
        }
    }

    // ─── Validation ───────────────────────────────────────────────────────────

    fun validateAndCreate() {
        val state = _uiState.value
        val groupId = currentGroupId ?: return
        val paidBy = currentUserId ?: return

        val title = state.titleInput.trim()
        val amount = state.amountInput.toDoubleOrNull()
        val participants = state.selectedParticipantIds.toList()

        // Basic validation
        if (title.isBlank()) {
            _uiState.update { it.copy(error = "Please enter a title") }
            return
        }
        if (amount == null || amount <= 0) {
            _uiState.update { it.copy(error = "Please enter a valid amount") }
            return
        }
        if (participants.isEmpty()) {
            _uiState.update { it.copy(error = "Select at least one participant") }
            return
        }

        // Custom split validation — must sum to total
        if (state.splitType == "custom") {
            val customSum = participants.sumOf {
                state.customAmounts[it]?.toDoubleOrNull() ?: 0.0
            }
            val diff = Math.abs(customSum - amount)
            if (diff > 0.01) {
                _uiState.update {
                    it.copy(
                        error = "Custom amounts must sum to ₹${"%.2f".format(amount)} " +
                                "(currently ₹${"%.2f".format(customSum)})"
                    )
                }
                return
            }
        }

        createExpense(
            groupId = groupId,
            title = title,
            amount = amount,
            paidBy = paidBy,
            participants = participants,
            splitType = state.splitType,
            customAmounts = state.customAmounts.mapValues {
                it.value.toDoubleOrNull() ?: 0.0
            }
        )
    }

    private fun createExpense(
        groupId: String,
        title: String,
        amount: Double,
        paidBy: String,
        participants: List<String>,
        splitType: String,
        customAmounts: Map<String, Double>
    ) {
        viewModelScope.launch {
            _uiState.update { it.copy(isCreating = true, error = null) }

            val expense = repository.createExpense(
                groupId = groupId,
                title = title,
                amount = amount,
                paidBy = paidBy,
                participantIds = participants,
                splitType = splitType,
                customAmounts = customAmounts
            )

            if (expense != null) {
                val updated = repository.getExpensesForGroup(groupId)
                val balances = repository.calculateBalances(groupMembers, updated)
                _uiState.update {
                    it.copy(
                        expenses = updated,
                        memberBalances = balances,
                        isCreating = false,
                        titleInput = "",
                        amountInput = "",
                        selectedParticipantIds = setOf(currentUserId ?: ""),
                        splitType = "equal",
                        customAmounts = emptyMap(),
                        message = "Expense added!"
                    )
                }
            } else {
                _uiState.update {
                    it.copy(isCreating = false, error = "Failed to add expense")
                }
            }
        }
    }

    // ─── Expense Details ──────────────────────────────────────────────────────

    fun selectExpense(expense: ExpenseWithParticipants) {
        _uiState.update { it.copy(selectedExpense = expense) }
    }

    fun clearSelectedExpense() {
        _uiState.update { it.copy(selectedExpense = null) }
    }

    // ─── Realtime ─────────────────────────────────────────────────────────────

    private fun startRealtimeSubscriptions(groupId: String, members: List<User>) {
        viewModelScope.launch {
            try {
                realtimeManager.connect()

                val expensesFlow = realtimeManager.watchExpenses()
                val participantsFlow = realtimeManager.watchExpenseParticipants()

                realtimeManager.subscribe()

                expensesFlow?.let { flow ->
                    launch {
                        flow.collect { action ->
                            handleExpenseAction(action, groupId, members)
                        }
                    }
                }

                participantsFlow?.let { flow ->
                    launch {
                        flow.collect { action ->
                            handleParticipantAction(action, groupId, members)
                        }
                    }
                }

            } catch (e: Exception) {
                // Don't crash UI — realtime is best-effort
            }
        }
    }

    private suspend fun handleExpenseAction(
        action: PostgresAction,
        groupId: String,
        members: List<User>
    ) {
        when (action) {
            is PostgresAction.Insert,
            is PostgresAction.Delete,
            is PostgresAction.Update -> {
                val updated = repository.getExpensesForGroup(groupId)
                val balances = repository.calculateBalances(members, updated)
                _uiState.update { it.copy(expenses = updated, memberBalances = balances) }
            }
            else -> {}
        }
    }

    private suspend fun handleParticipantAction(
        action: PostgresAction,
        groupId: String,
        members: List<User>
    ) {
        when (action) {
            is PostgresAction.Insert -> {
                val updated = repository.getExpensesForGroup(groupId)
                val balances = repository.calculateBalances(members, updated)
                _uiState.update { it.copy(expenses = updated, memberBalances = balances) }
            }
            else -> {}
        }
    }

    // ─── Helpers ──────────────────────────────────────────────────────────────

    private fun recalculateEqualAmounts(
        participantIds: Set<String>,
        totalAmount: Double
    ): Map<String, String> {
        if (participantIds.isEmpty()) return emptyMap()
        val share = totalAmount / participantIds.size
        return participantIds.associateWith { "%.2f".format(share) }
    }

    fun resetForm() {
        _uiState.update {
            it.copy(
                titleInput = "",
                amountInput = "",
                selectedParticipantIds = setOf(currentUserId ?: ""),
                splitType = "equal",
                customAmounts = emptyMap(),
                error = null
            )
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
}