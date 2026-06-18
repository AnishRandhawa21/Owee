package com.owee.app.viewmodel

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.owee.app.data.local.BalanceCacheManager
import com.owee.app.data.realtime.ExpenseEventBus
import com.owee.app.data.realtime.ExpenseRealtimeManager
import com.owee.app.data.realtime.GroupRealtimeManager
import com.owee.app.data.repository.BalanceRepository
import com.owee.app.domain.model.GroupBalance
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

class BalanceViewModel(
    application: Application,
    private val repository: BalanceRepository = BalanceRepository(),
    // FIX: use the shared singleton so this ViewModel and ExpensesViewModel
    //      share one channel — not two separate subscriptions.
    private val realtimeManager: ExpenseRealtimeManager = ExpenseRealtimeManager.shared(),
    private val groupRealtimeManager: GroupRealtimeManager = GroupRealtimeManager(),
) : AndroidViewModel(application) {

    private val cacheManager = BalanceCacheManager(application)
    private val _uiState = MutableStateFlow(cacheManager.getBalanceState() ?: BalanceUiState())
    val uiState: StateFlow<BalanceUiState> = _uiState.asStateFlow()

    private var loadBalancesJob: Job? = null
    private var realtimeJob: Job? = null
    private var currentUserId: String? = null

    /**
     * Internal reload trigger.  All realtime collectors and the event-bus listener
     * funnel into this single shared flow so that bursts of events (e.g. one expense
     * insert + 4 participant inserts) are debounced into a single server reload.
     *
     * replay=0, extraBufferCapacity=8  → emitters never block even if the debounce
     * window is still open.
     */
    private val reloadTrigger = MutableSharedFlow<Unit>(
        replay = 0,
        extraBufferCapacity = 8
    )

    /** How long to wait after the last event before issuing a reload (ms). */
    private val DEBOUNCE_MS = 300L

    // ─── Public API ───────────────────────────────────────────────────────────

    fun initialize(userId: String) {
        if (currentUserId == userId) return
        currentUserId = userId
        loadBalances(userId)
        startRealtimeSubscriptions(userId)
        observeEventBus()
    }

    /**
     * Optimistically updates the balance state locally before server confirmation.
     * Called by [ExpensesViewModel] immediately after the user taps "Create Expense",
     * so the Home screen reflects the change with zero perceived latency.
     *
     * A silent server reload is scheduled via [reloadTrigger] after
     * [DEBOUNCE_MS] ms to reconcile any rounding or calculation differences.
     */
    fun optimisticUpdate(
        groupId: String,
        paidBy: String,
        amount: Double,
        participantShares: Map<String, Double>  // userId → share amount
    ) {
        val userId = currentUserId ?: return

        _uiState.update { state ->
            val overall = state.overallBalance ?: return@update state

            // --- Update the specific group balance ---
            val updatedGroupBalances = overall.groupBalances.map { gb ->
                if (gb.groupId != groupId) return@map gb

                val updatedUserBalances = gb.balances.map { ub ->
                    val share = participantShares[ub.userId] ?: 0.0
                    val paid  = if (ub.userId == paidBy) amount else 0.0
                    ub.copy(amount = ub.amount + (paid - share))
                }
                gb.copy(
                    balances      = updatedUserBalances,
                    totalExpenses = gb.totalExpenses + amount
                )
            }

            // --- Recalculate overall totals from updated group balances ---
            var totalOwed = 0.0
            var totalOwe  = 0.0
            updatedGroupBalances.forEach { gb ->
                val myBalance = gb.balances.find { it.userId == userId }?.amount ?: 0.0
                when {
                    myBalance > 0 -> totalOwed += myBalance
                    myBalance < 0 -> totalOwe  += kotlin.math.abs(myBalance)
                }
            }

            val newState = state.copy(
                overallBalance = overall.copy(
                    totalOwed    = totalOwed,
                    totalOwe     = totalOwe,
                    groupBalances = updatedGroupBalances
                )
            )
            cacheManager.saveBalanceState(newState)
            newState
        }

        // Schedule a quiet server reconciliation shortly after the optimistic paint.
        viewModelScope.launch { reloadTrigger.emit(Unit) }
    }

    fun getBalanceWithFriend(friendId: String): Double =
        _uiState.value.friendBalances[friendId] ?: 0.0

    fun getSettlementsForGroup(groupId: String): List<com.owee.app.domain.model.Settlement> {
        val groupBalance = _uiState.value.overallBalance
            ?.groupBalances
            ?.find { it.groupId == groupId }
        return if (groupBalance != null) repository.getSettlements(groupBalance) else emptyList()
    }

    fun refresh() {
        currentUserId?.let { loadBalances(it) }
    }

    // ─── Load ─────────────────────────────────────────────────────────────────

    private fun loadBalances(userId: String, isSilent: Boolean = false) {
        loadBalancesJob?.cancel()
        loadBalancesJob = viewModelScope.launch {
            if (!isSilent) _uiState.update { it.copy(isLoading = true) }
            try {
                val overallBalance  = repository.getOverallBalance(userId)
                val friendBalances  = calculateFriendBalances(userId, overallBalance.groupBalances)
                val recentActivity  = repository.getRecentActivity(userId)
                val monthlySpent    = repository.getMonthlySpent(userId)
                val mostActiveGroup = repository.getMostActiveGroup(userId)

                _uiState.update {
                    it.copy(
                        overallBalance      = overallBalance,
                        friendBalances      = friendBalances,
                        recentActivity      = recentActivity,
                        monthlySpent        = monthlySpent,
                        mostActiveGroupName = mostActiveGroup,
                        isLoading           = false
                    )
                }
                cacheManager.saveBalanceState(_uiState.value)
            } catch (e: Exception) {
                if (e is kotlinx.coroutines.CancellationException) throw e
                _uiState.update { it.copy(error = e.message, isLoading = false) }
            }
        }
    }

    private fun calculateFriendBalances(
        userId: String,
        groupBalances: List<GroupBalance>
    ): Map<String, Double> {
        val balances = mutableMapOf<String, Double>()
        groupBalances.forEach { gb ->
            try {
                repository.getSettlements(gb).forEach { settlement ->
                    when (userId) {
                        settlement.fromUserId -> {
                            balances[settlement.toUserId] =
                                (balances[settlement.toUserId] ?: 0.0) - settlement.amount
                        }
                        settlement.toUserId -> {
                            balances[settlement.fromUserId] =
                                (balances[settlement.fromUserId] ?: 0.0) + settlement.amount
                        }
                    }
                }
            } catch (e: Exception) {
                if (e is kotlinx.coroutines.CancellationException) throw e
                Log.e(TAG, "Error calculating friend balances for group ${gb.groupId}", e)
            }
        }
        return balances
    }

    // ─── Realtime ─────────────────────────────────────────────────────────────

    /**
     * Subscribes to the debounced reload trigger.
     * All callers (realtime events, event-bus signals, optimistic reconciliation)
     * simply emit into [reloadTrigger]; this single collector handles the rest.
     */
    private fun startDebounceCollector(userId: String) {
        viewModelScope.launch {
            reloadTrigger
                .debounce(DEBOUNCE_MS)
                .collect {
                    Log.d(TAG, "Debounce window elapsed — reloading balances")
                    loadBalances(userId, isSilent = true)
                }
        }
    }

    private fun startRealtimeSubscriptions(userId: String) {
        realtimeJob?.cancel()
        realtimeJob = viewModelScope.launch {
            try {
                realtimeManager.connect()
                val expensesFlow     = realtimeManager.watchExpenses()
                val participantsFlow = realtimeManager.watchExpenseParticipants()
                realtimeManager.subscribe()

                groupRealtimeManager.connect()
                val groupsFlow  = groupRealtimeManager.watchGroups()
                val membersFlow = groupRealtimeManager.watchGroupMembers()
                groupRealtimeManager.subscribe()

                // Start the single debounce collector that does the actual reload.
                startDebounceCollector(userId)

                // Expense inserts/updates/deletes → enqueue a debounced reload.
                launch {
                    expensesFlow.collect { action ->
                        Log.d(TAG, "Realtime: expense action → $action")
                        // FIX: no delay here — we just signal the debounce trigger.
                        // A burst of participant inserts following this will all be
                        // absorbed into the same debounce window.
                        reloadTrigger.emit(Unit)
                    }
                }

                // Participant inserts/updates/deletes → same debounce bucket.
                launch {
                    participantsFlow.collect { action ->
                        Log.d(TAG, "Realtime: participant action → $action")
                        reloadTrigger.emit(Unit)
                    }
                }

                // Group-level changes (membership) — reload only when relevant.
                launch {
                    groupsFlow.collect {
                        // Group creation alone doesn't change balances until a member/expense
                        // is added; no reload needed here.
                    }
                }

                launch {
                    membersFlow.collect { action ->
                        Log.d(TAG, "Realtime: member action → $action")
                        if (action is PostgresAction.Insert) {
                            try {
                                val member = action.decodeRecord<com.owee.app.data.remote.model.GroupMember>()
                                val isMe = member.user_id == userId
                                val isExistingGroup = _uiState.value.overallBalance
                                    ?.groupBalances
                                    ?.any { it.groupId == member.group_id } == true
                                if (isMe || isExistingGroup) {
                                    reloadTrigger.emit(Unit)
                                }
                            } catch (e: Exception) {
                                Log.e(TAG, "Realtime: error decoding member", e)
                                reloadTrigger.emit(Unit)
                            }
                        } else {
                            reloadTrigger.emit(Unit)
                        }
                    }
                }

            } catch (e: Exception) {
                if (e is kotlinx.coroutines.CancellationException) throw e
                Log.e(TAG, "Realtime setup error", e) // Realtime is best-effort
            }
        }
    }

    /**
     * Listens to [ExpenseEventBus] so that when [ExpensesViewModel] successfully
     * creates an expense on the server, BalanceViewModel immediately schedules a
     * silent reload — even if the Supabase realtime event hasn't arrived yet.
     *
     * This covers the race condition where the realtime event is delayed or the
     * user's device has a brief websocket hiccup.
     */
    private fun observeEventBus() {
        viewModelScope.launch {
            ExpenseEventBus.expenseChanged.collect {
                Log.d(TAG, "EventBus: expense change signal received — enqueueing reload")
                reloadTrigger.emit(Unit)
            }
        }
    }

    // ─── Lifecycle ────────────────────────────────────────────────────────────

    override fun onCleared() {
        super.onCleared()
        viewModelScope.launch {
            realtimeManager.disconnect()
            groupRealtimeManager.disconnect()
        }
    }

    private companion object {
        private const val TAG = "BalanceViewModel"
    }
}