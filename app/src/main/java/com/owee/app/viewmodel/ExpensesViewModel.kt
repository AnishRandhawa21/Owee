package com.owee.app.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.owee.app.data.realtime.ExpenseEventBus
import com.owee.app.data.realtime.ExpenseRealtimeManager
import com.owee.app.data.remote.model.Expense
import com.owee.app.data.remote.model.ExpenseParticipant
import com.owee.app.data.remote.model.ExpenseWithParticipants
import com.owee.app.data.remote.model.User
import com.owee.app.data.repository.ExpenseRepository
import io.github.jan.supabase.realtime.PostgresAction
import io.github.jan.supabase.realtime.decodeRecord
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.UUID

class ExpensesViewModel(
    private val repository: ExpenseRepository = ExpenseRepository(),
    // FIX: use the shared singleton so this ViewModel and BalanceViewModel
    //      share one channel — not two separate subscriptions.
    private val realtimeManager: ExpenseRealtimeManager = ExpenseRealtimeManager.shared(),
    // Optional: inject BalanceViewModel to call optimisticUpdate directly.
    // When null, we fall back to the event-bus-only path (still instant via debounce).
    private val balanceViewModel: BalanceViewModel? = null,
) : ViewModel() {

    private var currentGroupId: String? = null
    private var currentUserId: String? = null
    private var groupMembers: List<User> = emptyList()

    private val _uiState = MutableStateFlow(ExpenseUiState())
    val uiState: StateFlow<ExpenseUiState> = _uiState.asStateFlow()

    private var loadExpensesJob: Job? = null
    private var realtimeJob: Job? = null

    /**
     * Internal reload trigger for this ViewModel.
     * Realtime events for the current group funnel here so that a burst of
     * participant inserts collapses into a single loadExpenses call.
     */
    private val reloadTrigger = MutableSharedFlow<Unit>(
        replay = 0,
        extraBufferCapacity = 8
    )

    /** Debounce window — keep in sync with BalanceViewModel.DEBOUNCE_MS. */
    private val DEBOUNCE_MS = 300L

    // ─── Init ─────────────────────────────────────────────────────────────────

    fun initialize(
        groupId: String,
        currentUserId: String,
        members: List<User>
    ) {
        if (groupId.isBlank() || currentUserId.isBlank()) return

        val membersChanged = this.groupMembers != members
        if (currentGroupId == groupId && !membersChanged) return

        Log.d(TAG, "Initializing for group $groupId. Members changed: $membersChanged")
        currentGroupId = groupId
        this.currentUserId = currentUserId
        this.groupMembers = members

        if (_uiState.value.selectedParticipantIds.isEmpty()) {
            _uiState.update { it.copy(selectedParticipantIds = setOf(currentUserId)) }
        }

        loadExpenses(groupId, members)
        startRealtimeSubscriptions(groupId, members)
    }

    fun refresh() {
        val gid = currentGroupId ?: return
        loadExpenses(gid, groupMembers)
    }

    // ─── Load ─────────────────────────────────────────────────────────────────

    private fun loadExpenses(
        groupId: String,
        members: List<User>,
        isSilent: Boolean = false
    ) {
        loadExpensesJob?.cancel()
        loadExpensesJob = viewModelScope.launch {
            if (!isSilent) _uiState.update { it.copy(isLoading = true) }
            try {
                val expenses = repository.getExpensesForGroup(groupId)
                val balances = repository.calculateBalances(members, expenses)
                Log.d(TAG, "Loaded ${expenses.size} expenses, ${balances.size} member balances")
                _uiState.update {
                    it.copy(expenses = expenses, memberBalances = balances, isLoading = false)
                }
            } catch (e: Exception) {
                if (e is kotlinx.coroutines.CancellationException) throw e
                Log.e(TAG, "Error loading expenses", e)
                _uiState.update { it.copy(isLoading = false, error = e.message) }
            }
        }
    }

    // ─── Create Form ──────────────────────────────────────────────────────────

    fun onTitleChanged(title: String) {
        _uiState.update { it.copy(titleInput = title) }
    }

    fun onAmountChanged(amount: String) {
        _uiState.update { state ->
            val parsed = amount.toDoubleOrNull() ?: 0.0
            val updatedCustom = if (state.splitType == "equal") {
                recalculateEqualAmounts(state.selectedParticipantIds, parsed)
            } else {
                state.customAmounts
            }
            state.copy(amountInput = amount, customAmounts = updatedCustom)
        }
    }

    fun onParticipantToggled(userId: String) {
        val currentUserId = this.currentUserId ?: return
        _uiState.update { state ->
            if (userId == currentUserId) return@update state
            val updated = state.selectedParticipantIds.toMutableSet()
            if (userId in updated) updated.remove(userId) else updated.add(userId)
            val amount = state.amountInput.toDoubleOrNull() ?: 0.0
            val updatedCustom = if (state.splitType == "equal") {
                recalculateEqualAmounts(updated, amount)
            } else {
                state.customAmounts.toMutableMap().also {
                    if (userId !in updated) it.remove(userId)
                }
            }
            state.copy(selectedParticipantIds = updated, customAmounts = updatedCustom)
        }
    }

    fun onSplitTypeChanged(type: String) {
        _uiState.update { state ->
            val amount = state.amountInput.toDoubleOrNull() ?: 0.0
            val updatedCustom = recalculateEqualAmounts(state.selectedParticipantIds, amount)
            state.copy(splitType = type, customAmounts = updatedCustom)
        }
    }

    fun onCustomAmountChanged(userId: String, amount: String) {
        _uiState.update { state ->
            state.copy(customAmounts = state.customAmounts.toMutableMap().also { it[userId] = amount })
        }
    }

    // ─── Validation ───────────────────────────────────────────────────────────

    fun validateAndCreate() {
        val state        = _uiState.value
        val groupId      = currentGroupId ?: return
        val paidBy       = currentUserId  ?: return
        val title        = state.titleInput.trim()
        val amount       = state.amountInput.toDoubleOrNull()
        val participants = state.selectedParticipantIds.toList()

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
        if (state.splitType == "custom") {
            val customSum = participants.sumOf { state.customAmounts[it]?.toDoubleOrNull() ?: 0.0 }
            if (kotlin.math.abs(customSum - amount) > 0.01) {
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
            groupId      = groupId,
            title        = title,
            amount       = amount,
            paidBy       = paidBy,
            participants = participants,
            splitType    = state.splitType,
            customAmounts = state.customAmounts.mapValues { it.value.toDoubleOrNull() ?: 0.0 }
        )
    }

    // ─── Create ───────────────────────────────────────────────────────────────

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
            val tempId = UUID.randomUUID().toString()
            val now    = ZonedDateTime.now().format(DateTimeFormatter.ISO_OFFSET_DATE_TIME)

            // 1. Build optimistic object for this group's list
            val optimisticExpense = ExpenseWithParticipants(
                expense = Expense(
                    id         = tempId,
                    group_id   = groupId,
                    title      = title,
                    amount     = amount,
                    paid_by    = paidBy,
                    split_type = splitType,
                    created_at = now
                ),
                paidByUser = groupMembers.find { it.id == paidBy }
                    ?: User(id = paidBy, auth_id = "", name = "You", username = "you"),
                participants = participants.map { uid ->
                    ExpenseParticipant(
                        id         = UUID.randomUUID().toString(),
                        expense_id = tempId,
                        user_id    = uid,
                        share_amount = if (splitType == "custom") {
                            customAmounts[uid] ?: 0.0
                        } else {
                            amount / participants.size
                        }
                    )
                },
                participantUsers = groupMembers.filter { it.id in participants }
            )

            // 2. Snapshot for rollback
            val previousExpenses = _uiState.value.expenses
            val previousBalances = _uiState.value.memberBalances

            // 3. Optimistic update — group screen
            val optimisticList     = listOf(optimisticExpense) + previousExpenses
            val optimisticBalances = repository.calculateBalances(groupMembers, optimisticList)
            _uiState.update {
                it.copy(
                    expenses       = optimisticList,
                    memberBalances = optimisticBalances,
                    isCreating     = true,
                    error          = null,
                    // FIX: set message to trigger navigation IMMEDIATELY (before network call)
                    message        = "Expense added!"
                )
            }

            // 4. FIX: Propagate optimistic update to BalanceViewModel (Home/People screen)
            //    so they also reflect the change instantly without waiting for the server.
            val participantShares: Map<String, Double> = participants.associateWith { uid ->
                if (splitType == "custom") customAmounts[uid] ?: 0.0
                else amount / participants.size
            }
            balanceViewModel?.optimisticUpdate(
                groupId          = groupId,
                paidBy           = paidBy,
                amount           = amount,
                participantShares = participantShares
            )
            // If balanceViewModel is not injected, still signal via event bus so
            // BalanceViewModel can schedule a silent reload via its debounce collector.
            if (balanceViewModel == null) {
                ExpenseEventBus.notifyExpenseChanged()
            }

            // 5. Network call (happens in background; UI has already navigated back)
            val serverExpense = repository.createExpense(
                groupId       = groupId,
                title         = title,
                amount        = amount,
                paidBy        = paidBy,
                participantIds = participants,
                splitType     = splitType,
                customAmounts = customAmounts
            )

            if (serverExpense != null) {
                Log.d(TAG, "Expense created on server: ${serverExpense.id}")

                // 6. FIX: Notify event bus so BalanceViewModel does a reconciliation
                //    reload even if the realtime event is slow or missed.
                ExpenseEventBus.notifyExpenseChanged()

                // 7. Reconcile group-screen list with real server IDs
                val updatedList    = repository.getExpensesForGroup(groupId)
                val finalBalances  = repository.calculateBalances(groupMembers, updatedList)
                _uiState.update {
                    it.copy(
                        expenses              = updatedList,
                        memberBalances        = finalBalances,
                        isCreating            = false,
                        titleInput            = "",
                        amountInput           = "",
                        selectedParticipantIds = setOf(currentUserId ?: ""),
                        splitType             = "equal",
                        customAmounts         = emptyMap()
                    )
                }
            } else {
                // 8. Server rejected — roll back everything including BalanceViewModel
                Log.e(TAG, "createExpense failed — rolling back optimistic state")
                _uiState.update {
                    it.copy(
                        expenses       = previousExpenses,
                        memberBalances = previousBalances,
                        isCreating     = false,
                        // Clear the "Expense added!" message so the screen doesn't navigate away
                        message        = null,
                        error          = "Failed to sync with server"
                    )
                }
                // Signal BalanceViewModel to reload so its optimistic state is corrected
                ExpenseEventBus.notifyExpenseChanged()
            }
        }
    }

    // ─── Expense Details ──────────────────────────────────────────────────────

    fun selectExpense(expense: ExpenseWithParticipants) {
        _uiState.update { it.copy(selectedExpense = expense) }
    }

    // ─── Realtime ─────────────────────────────────────────────────────────────

    private fun startRealtimeSubscriptions(groupId: String, members: List<User>) {
        realtimeJob?.cancel()
        realtimeJob = viewModelScope.launch {
            try {
                realtimeManager.connect()
                val expensesFlow     = realtimeManager.watchExpenses()
                val participantsFlow = realtimeManager.watchExpenseParticipants()
                realtimeManager.subscribe()

                // Start the single debounce collector for this ViewModel.
                launch {
                    reloadTrigger
                        .debounce(DEBOUNCE_MS)
                        .collect {
                            Log.d(TAG, "Debounce elapsed — reloading expenses for group $groupId")
                            loadExpenses(groupId, members, isSilent = true)
                        }
                }

                // FIX: no delay() — just filter for the current group and emit to the trigger.
                launch {
                    expensesFlow.collect { action ->
                        Log.d(TAG, "Realtime: expense action → $action")
                        when (action) {
                            is PostgresAction.Insert -> {
                                try {
                                    val record = action.decodeRecord<Expense>()
                                    if (record.group_id == groupId) {
                                        // Only reload for events that belong to this group.
                                        reloadTrigger.emit(Unit)
                                    }
                                } catch (e: Exception) {
                                    // Decode failed — can't check group_id, reload to be safe.
                                    Log.e(TAG, "Error decoding expense record", e)
                                    reloadTrigger.emit(Unit)
                                }
                            }
                            is PostgresAction.Update,
                            is PostgresAction.Delete -> reloadTrigger.emit(Unit)
                            else -> {}
                        }
                    }
                }

                // FIX: participant inserts also go into the same debounce bucket.
                //      4 participants for one expense = 1 reload after 300 ms, not 4 reloads.
                launch {
                    participantsFlow.collect { action ->
                        Log.d(TAG, "Realtime: participant action → $action")
                        when (action) {
                            is PostgresAction.Insert,
                            is PostgresAction.Update,
                            is PostgresAction.Delete -> reloadTrigger.emit(Unit)
                            else -> {}
                        }
                    }
                }

            } catch (e: Exception) {
                if (e is kotlinx.coroutines.CancellationException) throw e
                Log.e(TAG, "Realtime setup error", e)
            }
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

    fun clearMessage() {
        _uiState.update { it.copy(message = null, error = null) }
    }

    // ─── Lifecycle ────────────────────────────────────────────────────────────

    override fun onCleared() {
        super.onCleared()
        viewModelScope.launch { realtimeManager.disconnect() }
    }

    private companion object {
        private const val TAG = "ExpensesViewModel"
    }
}