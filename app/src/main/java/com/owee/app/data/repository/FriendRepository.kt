package com.owee.app.data.repository

import com.owee.app.OweeApp
import com.owee.app.data.local.FriendCacheManager
import com.owee.app.data.remote.SupabaseProvider
import com.owee.app.data.remote.model.Friend
import com.owee.app.data.remote.model.FriendRequest
import com.owee.app.data.remote.model.FriendRequestUi
import com.owee.app.data.remote.model.User
import io.github.jan.supabase.postgrest.from

class FriendRepository {

    private val cache = FriendCacheManager(OweeApp.instance)
    private val userCache = mutableMapOf<String, User>()

    fun getCachedFriends(): List<User> = cache.getFriends()
    fun getCachedSentRequests(): List<User> = cache.getSentRequests()
    fun getCachedSentRequestIds(): Set<String> = cache.getSentRequestIds()

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
        receiverId: String,
        receiverUser: User // Pass the user profile to cache it
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
            
            // Persist to local cache immediately
            val currentSent = cache.getSentRequests().toMutableList()
            if (currentSent.none { it.id == receiverUser.id }) {
                currentSent.add(receiverUser)
                cache.saveSentRequests(currentSent)
            }
            
            true
        } catch (e: Exception) {
            false
        }
    }

    suspend fun getPendingRequests(
        currentUserId: String
    ): List<FriendRequestUi> {
        return try {
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

            val senderIds = requests.map { it.sender_id }
            val senders = SupabaseProvider.client
                .from("users")
                .select {
                    filter {
                        isIn("id", senderIds)
                    }
                }
                .decodeList<User>()

            requests.mapNotNull { request ->
                val sender = senders.find { it.id == request.sender_id }
                if (sender != null) {
                    FriendRequestUi(
                        requestId = request.id ?: "",
                        senderId = sender.id ?: "",
                        senderName = sender.name,
                        senderUsername = sender.username,
                        senderPhotoUrl = sender.photo_url
                    )
                } else null
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun acceptFriendRequest(
        requestId: String,
        senderId: String,
        receiverId: String
    ): Boolean {
        return try {
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
            
            val friendsList = friendsRaw.decodeList<Friend>()
            
            if (friendsList.isEmpty()) {
                cache.saveFriends(emptyList())
                return emptyList()
            }

            val friendIds = friendsList.map { friend ->
                if (friend.user_one == currentUserId) friend.user_two else friend.user_one
            }

            val friends = SupabaseProvider.client
                .from("users")
                .select {
                    filter {
                        isIn("id", friendIds)
                    }
                }
                .decodeList<User>()
            
            cache.saveFriends(friends)
            friends
        } catch (e: Exception) {
            cache.getFriends()
        }
    }

    suspend fun getSentRequests(currentUserId: String): List<User> {
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
            
            if (requests.isEmpty()) {
                cache.saveSentRequests(emptyList())
                return emptyList()
            }

            val receiverIds = requests.map { it.receiver_id }
            val users = SupabaseProvider.client
                .from("users")
                .select {
                    filter {
                        isIn("id", receiverIds)
                    }
                }
                .decodeList<User>()
            
            cache.saveSentRequests(users)
            users
        } catch (e: Exception) {
            cache.getSentRequests()
        }
    }

    fun removeSentRequest(userId: String) {
        val current = cache.getSentRequests().toMutableList()
        current.removeAll { it.id == userId }
        cache.saveSentRequests(current)
    }
}
