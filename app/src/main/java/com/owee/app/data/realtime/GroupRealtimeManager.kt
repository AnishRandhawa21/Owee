package com.owee.app.data.realtime

import android.util.Log
import com.owee.app.data.remote.SupabaseProvider
import io.github.jan.supabase.realtime.PostgresAction
import io.github.jan.supabase.realtime.Realtime
import io.github.jan.supabase.realtime.RealtimeChannel
import io.github.jan.supabase.realtime.channel
import io.github.jan.supabase.realtime.postgresChangeFlow
import io.github.jan.supabase.realtime.realtime
import kotlinx.coroutines.flow.Flow

class GroupRealtimeManager {

    private val client = SupabaseProvider.client
    private var channel: RealtimeChannel? = null

    suspend fun connect() {
        try {
            if (client.realtime.status.value != Realtime.Status.CONNECTED) {
                Log.d("GroupRealtime", "Connecting to Supabase Realtime...")
                client.realtime.connect()
            }
        } catch (e: Exception) {
            Log.e("GroupRealtime", "Error connecting to Realtime", e)
        }
    }

    private fun getOrCreateChannel(): RealtimeChannel {
        if (channel == null) {
            // Use a stable but instance-unique name
            val channelName = "group_updates_${hashCode()}"
            channel = client.realtime.channel(channelName)
            Log.d("GroupRealtime", "Created channel: $channelName")
        }
        return channel!!
    }

    fun watchGroups(): Flow<PostgresAction> {
        Log.d("GroupRealtime", "Setting up watch for 'groups' table")
        return getOrCreateChannel().postgresChangeFlow<PostgresAction>(schema = "public") {
            table = "groups"
        }
    }

    fun watchGroupMembers(): Flow<PostgresAction> {
        Log.d("GroupRealtime", "Setting up watch for 'group_members' table")
        return getOrCreateChannel().postgresChangeFlow<PostgresAction>(schema = "public") {
            table = "group_members"
        }
    }

    suspend fun subscribe() {
        try {
            val ch = channel
            if (ch != null) {
                Log.d("GroupRealtime", "Subscribing to channel ${ch.topic}...")
                ch.subscribe()
                Log.d("GroupRealtime", "Channel status: ${ch.status.value}")
            } else {
                Log.w("GroupRealtime", "No channel to subscribe to. Call watchGroups/watchGroupMembers first.")
            }
        } catch (e: Exception) {
            Log.e("GroupRealtime", "Error subscribing to channel", e)
        }
    }

    suspend fun disconnect() {
        try {
            channel?.let {
                Log.d("GroupRealtime", "Disconnecting channel ${it.topic}...")
                client.realtime.removeChannel(it)
                channel = null
            }
        } catch (e: Exception) {
            Log.e("GroupRealtime", "Error disconnecting channel", e)
        }
    }
}
