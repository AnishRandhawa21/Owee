package com.owee.app.data.repository

import com.owee.app.data.remote.SupabaseProvider
import com.owee.app.data.remote.model.User
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Columns

class UserRepository {

    /**
     * Checks if a username already exists in the users table.
     */
    suspend fun isUsernameTaken(username: String): Boolean {
        return try {
            val response = SupabaseProvider.client.from("users")
                .select(columns = Columns.list("username")) {
                    filter {
                        eq("username", username)
                    }
                }
            response.decodeList<User>().isNotEmpty()
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Inserts a new user record into the public.users table.
     */
    suspend fun insertUser(user: User) {
        SupabaseProvider.client
            .from("users")
            .insert(user)
    }

    /**
     * Fetches a user profile by their auth_id.
     */
    suspend fun getUserProfile(authId: String): User? {
        return try {
            val response = SupabaseProvider.client.from("users")
                .select {
                    filter {
                        eq("auth_id", authId)
                    }
                }
            response.decodeSingleOrNull<User>()
        } catch (e: Exception) {
            null
        }
    }
}
