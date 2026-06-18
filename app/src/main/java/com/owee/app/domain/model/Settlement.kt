package com.owee.app.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class Settlement(
    val fromUserId: String,
    val fromUserName: String,
    val fromUserPhotoUrl: String? = null,
    val toUserId: String,
    val toUserName: String,
    val toUserPhotoUrl: String? = null,
    val amount: Double
)
