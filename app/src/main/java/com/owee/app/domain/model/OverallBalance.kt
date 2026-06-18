package com.owee.app.domain.model

data class OverallBalance(
    val totalOwed: Double,  // Amount others owe me (Positive)
    val totalOwe: Double,   // Amount I owe others (Negative)
    val groupBalances: List<GroupBalance>
)
