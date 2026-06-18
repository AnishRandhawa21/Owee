package com.owee.app.data.remote.model

data class FriendRequestUi(
    val requestId: String,
    val senderId: String,
    val senderName: String,
    val senderUsername: String,
    val senderPhotoUrl: String? = null
)