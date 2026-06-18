package com.owee.app.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class GroupBalance(
    val groupId: String,
    val groupName: String,
    val balances: List<UserBalance>,
    val totalExpenses: Double
)
