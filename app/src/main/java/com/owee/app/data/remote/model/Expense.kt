package com.owee.app.data.remote.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Expense(
    val id: String? = null,
    @SerialName("group_id") val group_id: String,
    val title: String,
    val amount: Double,
    @SerialName("paid_by") val paid_by: String,
    @SerialName("split_type") val split_type: String = "equal",
    @SerialName("created_at") val created_at: String? = null
)
