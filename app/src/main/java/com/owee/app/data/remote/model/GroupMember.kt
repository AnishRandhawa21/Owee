package com.owee.app.data.remote.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class GroupMember(
    val id: String? = null,
    @SerialName("group_id") val group_id: String,
    @SerialName("user_id") val user_id: String,
    @SerialName("role") val role: String,
    val joined_at: String? = null
)