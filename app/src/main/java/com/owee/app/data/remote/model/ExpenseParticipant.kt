package com.owee.app.data.remote.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ExpenseParticipant(
    val id: String? = null,
    @SerialName("expense_id") val expense_id: String,
    @SerialName("user_id") val user_id: String,
    @SerialName("share_amount") val share_amount: Double
)
