package com.owee.app.data.realtime

import com.owee.app.data.remote.SupabaseProvider
import io.github.jan.supabase.realtime.PostgresAction
import io.github.jan.supabase.realtime.Realtime
import io.github.jan.supabase.realtime.RealtimeChannel
import io.github.jan.supabase.realtime.channel
import io.github.jan.supabase.realtime.postgresChangeFlow
import io.github.jan.supabase.realtime.realtime
import kotlinx.coroutines.flow.Flow

class FriendRealtimeManager {

    private val client = SupabaseProvider.client
    
    private var requestsChannel: RealtimeChannel? = null
    private var friendsChannel: RealtimeChannel? = null

    suspend fun connect() {
        if (client.realtime.status.value != Realtime.Status.CONNECTED) {
            client.realtime.connect()
        }
    }

    fun watchFriendRequests(): Flow<PostgresAction>? {
        val channel = client.realtime.channel("friend_requests_realtime")
        
        // Return null if already subscribed to avoid IllegalStateException
        if (channel.status.value == RealtimeChannel.Status.SUBSCRIBED) {
            return null
        }

        val flow = channel.postgresChangeFlow<PostgresAction>(schema = "public") {
            table = "friend_requests"
        }
        requestsChannel = channel
        return flow
    }

    fun watchFriends(): Flow<PostgresAction>? {
        val channel = client.realtime.channel("friends_realtime")
        
        if (channel.status.value == RealtimeChannel.Status.SUBSCRIBED) {
            return null
        }

        val flow = channel.postgresChangeFlow<PostgresAction>(schema = "public") {
            table = "friends"
        }
        friendsChannel = channel
        return flow
    }

    suspend fun subscribe() {
        if (requestsChannel?.status?.value != RealtimeChannel.Status.SUBSCRIBED) {
            requestsChannel?.subscribe()
        }
        if (friendsChannel?.status?.value != RealtimeChannel.Status.SUBSCRIBED) {
            friendsChannel?.subscribe()
        }
    }

    suspend fun disconnect() {
        requestsChannel?.let { 
            client.realtime.removeChannel(it)
            requestsChannel = null
        }
        friendsChannel?.let { 
            client.realtime.removeChannel(it)
            friendsChannel = null
        }
    }
}
