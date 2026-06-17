package com.owee.app.data.remote.model

import kotlinx.serialization.Serializable


@Serializable
data class Friend(
    val id: String? = null,
    val user_one: String,
    val user_two: String,
    val created_at: String? = null
)