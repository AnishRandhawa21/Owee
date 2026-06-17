package com.owee.app.data.remote.model

import kotlinx.serialization.Serializable

@Serializable
data class Expense(
    val id: String? = null,
    val group_id: String,
    val title: String,
    val amount: Double,
    val paid_by: String,
    val split_type: String = "equal",
    val created_at: String? = null
)