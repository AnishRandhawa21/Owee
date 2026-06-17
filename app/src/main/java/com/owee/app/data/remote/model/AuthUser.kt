package com.owee.app.data.remote.model

data class AuthUser(
    val dbId: String? = null,      // users.id (UUID)
    val authId: String? = null,    // Supabase Auth UID
    val token: String? = null,
    val email: String? = null,
    val photoUrl: String? = null,
    val name: String? = null,
    val username: String? = null,
    val joinedDate: String? = null
)
