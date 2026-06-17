package com.owee.app.data.local

import android.content.Context
import android.content.SharedPreferences
import com.owee.app.data.remote.model.User
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class FriendCacheManager(context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences("friend_cache", Context.MODE_PRIVATE)
    private val json = Json { ignoreUnknownKeys = true }

    fun saveFriends(friends: List<User>) {
        val data = json.encodeToString(friends)
        prefs.edit().putString("friends_list", data).apply()
    }

    fun getFriends(): List<User> {
        val data = prefs.getString("friends_list", null) ?: return emptyList()
        return try {
            json.decodeFromString<List<User>>(data)
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun saveSentRequests(requests: List<User>) {
        val data = json.encodeToString(requests)
        prefs.edit().putString("sent_requests_list", data).apply()
        
        // Also update IDs for quick lookup
        val ids = requests.mapNotNull { it.id }.toSet()
        prefs.edit().putStringSet("sent_request_ids", ids).apply()
    }

    fun getSentRequests(): List<User> {
        val data = prefs.getString("sent_requests_list", null) ?: return emptyList()
        return try {
            json.decodeFromString<List<User>>(data)
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun getSentRequestIds(): Set<String> {
        return prefs.getStringSet("sent_request_ids", emptySet())?.toSet() ?: emptySet()
    }

    fun clear() {
        prefs.edit().clear().apply()
    }
}
