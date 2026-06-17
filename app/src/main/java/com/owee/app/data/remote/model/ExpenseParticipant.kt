package com.owee.app.data.remote.model

import kotlinx.serialization.Serializable

@Serializable
data class ExpenseParticipant(
    val id: String? = null,
    val expense_id: String,
    val user_id: String,
    val share_amount: Double
)