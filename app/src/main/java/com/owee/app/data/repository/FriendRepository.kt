package com.owee.app.data.repository

import com.owee.app.data.remote.SupabaseProvider
import com.owee.app.data.remote.model.Friend
import com.owee.app.data.remote.model.FriendRequest
import com.owee.app.data.remote.model.FriendRequestUi
import com.owee.app.data.remote.model.User
import io.github.jan.supabase.postgrest.from

class FriendRepository {

    suspend fun searchUsers(query: String): List<User> {
        return try {
            val response = SupabaseProvider.client
                .from("users")
                .select {
                    filter {
                        ilike("username", "%$query%")
                    }
                }

            response.decodeList<User>()
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun sendFriendRequest(
        senderId: String,
        receiverId: String
    ): Boolean {
        return try {
            val request = FriendRequest(
                sender_id = senderId,
                receiver_id = receiverId,
                status = "pending"
            )

            SupabaseProvider.client
                .from("friend_requests")
                .insert(request)
            true
        } catch (e: Exception) {
            false
        }
    }

    suspend fun getPendingRequests(
        currentUserId: String
    ): List<FriendRequestUi> {
        return try {
            // 1. Fetch pending requests
            val requests = SupabaseProvider.client
                .from("friend_requests")
                .select {
                    filter {
                        eq("receiver_id", currentUserId)
                        eq("status", "pending")
                    }
                }
                .decodeList<FriendRequest>()

            if (requests.isEmpty()) return emptyList()

            // 2. Fetch sender profiles
            val senderIds = requests.map { it.sender_id }
            val senders = SupabaseProvider.client
                .from("users")
                .select {
                    filter {
                        isIn("id", senderIds)
                    }
                }
                .decodeList<User>()

            // 3. Map to UI model
            requests.mapNotNull { request ->
                val sender = senders.find { it.id == request.sender_id }
                if (sender != null) {
                    FriendRequestUi(
                        requestId = request.id ?: "",
                        senderId = sender.id ?: "",
                        senderName = sender.name,
                        senderUsername = sender.username
                    )
                } else null
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun getSentRequestIds(currentUserId: String): Set<String> {
        return try {
            val requests = SupabaseProvider.client
                .from("friend_requests")
                .select {
                    filter {
                        eq("sender_id", currentUserId)
                        eq("status", "pending")
                    }
                }
                .decodeList<FriendRequest>()
            requests.map { it.receiver_id }.toSet()
        } catch (e: Exception) {
            emptySet()
        }
    }

    suspend fun acceptFriendRequest(
        requestId: String,
        senderId: String,
        receiverId: String
    ): Boolean {
        return try {
            // 1. Update request status
            SupabaseProvider.client
                .from("friend_requests")
                .update(
                    {
                        set("status", "accepted")
                    }
                ) {
                    filter {
                        eq("id", requestId)
                    }
                }

            // 2. Insert into friends table
            SupabaseProvider.client
                .from("friends")
                .insert(
                    mapOf(
                        "user_one" to senderId,
                        "user_two" to receiverId
                    )
                )
            true
        } catch (e: Exception) {
            false
        }
    }

    suspend fun rejectFriendRequest(
        requestId: String
    ): Boolean {
        return try {
            SupabaseProvider.client
                .from("friend_requests")
                .update(
                    {
                        set("status", "rejected")
                    }
                ) {
                    filter {
                        eq("id", requestId)
                    }
                }
            true
        } catch (e: Exception) {
            false
        }
    }

    private val userCache = mutableMapOf<String, User>()

    suspend fun getUserById(userId: String): User? {
        userCache[userId]?.let { return it }
        return try {
            val user = SupabaseProvider.client
                .from("users")
                .select {
                    filter {
                        eq("id", userId)
                    }
                }
                .decodeSingleOrNull<User>()

            if (user != null) {
                userCache[userId] = user
            }
            user
        } catch (e: Exception) {
            null
        }
    }

    suspend fun getFriends(
        currentUserId: String
    ): List<User> {
        return try {
            // 1. Fetch friend relationships
            val friendsRaw = SupabaseProvider.client
                .from("friends")
                .select {
                    filter {
                        or {
                            eq("user_one", currentUserId)
                            eq("user_two", currentUserId)
                        }
                    }
                }
            
            // We'll use a generic map because we don't have a Friend model with user_one/user_two yet
            val friendsList = friendsRaw.decodeList<Friend>()
            
            if (friendsList.isEmpty()) return emptyList()

            // 2. Extract friend IDs (the one that isn't currentUserId)
            val friendIds = friendsList.map { friend ->

                if (friend.user_one == currentUserId) {
                    friend.user_two
                } else {
                    friend.user_one
                }

            }

            if (friendIds.isEmpty()) return emptyList()

            // 3. Fetch friend profiles
            SupabaseProvider.client
                .from("users")
                .select {
                    filter {
                        isIn("id", friendIds)
                    }
                }
                .decodeList<User>()
        } catch (e: Exception) {
            emptyList()
        }
    }
}
