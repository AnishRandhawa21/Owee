package com.owee.app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.owee.app.data.realtime.ExpenseRealtimeManager
import com.owee.app.data.repository.BalanceRepository
import com.owee.app.domain.model.GroupBalance
import io.github.jan.supabase.realtime.PostgresAction
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class BalanceViewModel(
    private val repository: BalanceRepository = BalanceRepository(),
    private val realtimeManager: ExpenseRealtimeManager = ExpenseRealtimeManager()
) : ViewModel() {

    private val _uiState = MutableStateFlow(BalanceUiState())
    val uiState: StateFlow<BalanceUiState> = _uiState.asStateFlow()

    private var currentUserId: String? = null

    fun initialize(userId: String) {
        if (currentUserId == userId) return
        currentUserId = userId
        loadBalances(userId)
        startRealtimeSubscriptions(userId)
    }

    private fun loadBalances(userId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            try {
                val overallBalance = repository.getOverallBalance(userId)
                val friendBalances = calculateFriendBalances(userId, overallBalance.groupBalances)
                _uiState.update { it.copy(
                    overallBalance = overallBalance,
                    friendBalances = friendBalances,
                    isLoading = false
                ) }
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message, isLoading = false) }
            }
        }
    }

    private fun calculateFriendBalances(userId: String, groupBalances: List<GroupBalance>): Map<String, Double> {
        val balances = mutableMapOf<String, Double>()
        groupBalances.forEach { gb ->
            val settlements = repository.getSettlements(gb)
            settlements.forEach { settlement ->
                if (settlement.fromUserId == userId) {
                    // I owe settlement.toUserId
                    val current = balances.getOrDefault(settlement.toUserId, 0.0)
                    balances[settlement.toUserId] = current - settlement.amount
                } else if (settlement.toUserId == userId) {
                    // settlement.fromUserId owes me
                    val current = balances.getOrDefault(settlement.fromUserId, 0.0)
                    balances[settlement.fromUserId] = current + settlement.amount
                }
            }
        }
        return balances
    }

    private fun startRealtimeSubscriptions(userId: String) {
        viewModelScope.launch {
            try {
                realtimeManager.connect()
                val expensesFlow = realtimeManager.watchExpenses()
                val participantsFlow = realtimeManager.watchExpenseParticipants()
                realtimeManager.subscribe()

                launch {
                    expensesFlow?.collect { handleRealtimeUpdate(userId) }
                }
                launch {
                    participantsFlow?.collect { handleRealtimeUpdate(userId) }
                }
            } catch (e: Exception) {
                // Realtime is best effort
            }
        }
    }

    private fun handleRealtimeUpdate(userId: String) {
        loadBalances(userId)
    }

    fun getBalanceWithFriend(friendId: String): Double {
        return _uiState.value.friendBalances[friendId] ?: 0.0
    }

    fun getSettlementsForGroup(groupId: String): List<com.owee.app.domain.model.Settlement> {
        val groupBalance = _uiState.value.overallBalance?.groupBalances?.find { it.groupId == groupId }
        return if (groupBalance != null) {
            repository.getSettlements(groupBalance)
        } else {
            emptyList()
        }
    }

    override fun onCleared() {
        super.onCleared()
        viewModelScope.launch {
            realtimeManager.disconnect()
        }
    }
}
