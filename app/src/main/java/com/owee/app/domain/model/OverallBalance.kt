package com.owee.app.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class OverallBalance(
    val totalOwed: Double,  // Amount others owe me (Positive)
    val totalOwe: Double,   // Amount I owe others (Positive)
    val groupBalances: List<GroupBalance>
)
