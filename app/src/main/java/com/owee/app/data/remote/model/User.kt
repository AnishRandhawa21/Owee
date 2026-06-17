package com.owee.app.data.remote.model

import kotlinx.serialization.Serializable

@Serializable
data class User(
    val id: String? = null, // Database Primary Key (UUID)
    val auth_id: String,    // Supabase Auth UID
    val name: String,
    val username: String,
    val photo_url: String? = null,
    val created_at: String? = null
)
