package com.owee.app.data.remote.model

import kotlinx.serialization.Serializable

@Serializable
data class FriendRequest(
    val id: String? = null, // Database will generate UUID if null
    val sender_id: String,
    val receiver_id: String,
    val status: String,
    val created_at: String? = null
)
