package com.owee.app.domain.model

data class Settlement(
    val fromUserId: String,
    val fromUserName: String,
    val toUserId: String,
    val toUserName: String,
    val amount: Double
)
