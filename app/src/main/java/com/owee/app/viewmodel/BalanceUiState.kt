package com.owee.app.viewmodel

import com.owee.app.domain.model.OverallBalance
import com.owee.app.domain.model.Settlement

data class BalanceUiState(
    val overallBalance: OverallBalance? = null,
    val settlements: List<Settlement> = emptyList(),
    val friendBalances: Map<String, Double> = emptyMap(),
    val isLoading: Boolean = false,
    val error: String? = null
)
