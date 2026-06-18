package com.owee.app.domain.model

data class GroupBalance(
    val groupId: String,
    val groupName: String,
    val balances: List<UserBalance>,
    val totalExpenses: Double
)
