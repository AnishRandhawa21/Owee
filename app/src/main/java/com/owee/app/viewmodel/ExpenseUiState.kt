package com.owee.app.viewmodel

import com.owee.app.data.remote.model.ExpenseWithParticipants
import com.owee.app.data.remote.model.MemberBalance
import com.owee.app.data.remote.model.User

data class ExpenseUiState(

    // ─── Expense List ─────────────────────────────────────────────────────────
    val expenses: List<ExpenseWithParticipants> = emptyList(),
    val selectedExpense: ExpenseWithParticipants? = null,

    // ─── Balances ─────────────────────────────────────────────────────────────
    val memberBalances: List<MemberBalance> = emptyList(),

    // ─── Create Form ──────────────────────────────────────────────────────────
    val titleInput: String = "",
    val amountInput: String = "",

    // Payer is always current user — no field needed

    // Participants — current user always auto-included
    val selectedParticipantIds: Set<String> = emptySet(),

    // Split type
    val splitType: String = "equal",   // "equal" or "custom"

    // Custom split — map of userId → amount string they entered
    val customAmounts: Map<String, String> = emptyMap(),

    // ─── Status ───────────────────────────────────────────────────────────────
    val isLoading: Boolean = false,
    val isCreating: Boolean = false,
    val error: String? = null,
    val message: String? = null
)