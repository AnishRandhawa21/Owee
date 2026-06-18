package com.owee.app.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class UserBalance(
    val userId: String,
    val userName: String,
    val userPhotoUrl: String? = null,
    val amount: Double // Positive = to receive, Negative = owes
)
