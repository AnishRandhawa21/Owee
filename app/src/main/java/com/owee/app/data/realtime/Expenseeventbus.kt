package com.owee.app.data.realtime

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

/**
 * A process-wide event bus that lets [ExpensesViewModel] notify [BalanceViewModel]
 * when a local expense create/delete/update has succeeded on the server, without
 * creating a direct ViewModel-to-ViewModel dependency.
 *
 * Usage:
 *   Sender   → ExpenseEventBus.notifyExpenseChanged()
 *   Observer → ExpenseEventBus.expenseChanged.collect { … }
 *
 * The flow has no replay (replay = 0) so late subscribers never receive stale events.
 * extraBufferCapacity = 1 prevents the emitter from suspending if no collector is
 * currently active (e.g. the Home screen is in the back-stack but not yet resumed).
 */
object ExpenseEventBus {

    private val _expenseChanged = MutableSharedFlow<Unit>(
        replay = 0,
        extraBufferCapacity = 1
    )

    /** Observe this flow in BalanceViewModel to react to expense mutations. */
    val expenseChanged: SharedFlow<Unit> = _expenseChanged.asSharedFlow()

    /**
     * Call this from [ExpensesViewModel] after an expense is successfully created,
     * updated, or deleted on the server. This is a non-suspending, fire-and-forget
     * emit that will never block the caller.
     */
    fun notifyExpenseChanged() {
        _expenseChanged.tryEmit(Unit)
    }
}