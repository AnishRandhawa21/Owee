package com.owee.app.data.realtime

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

    private var groupsChannel: RealtimeChannel? = null
    private var membersChannel: RealtimeChannel? = null

    suspend fun connect() {
        if (client.realtime.status.value != Realtime.Status.CONNECTED) {
            client.realtime.connect()
        }
    }

    fun watchGroups(): Flow<PostgresAction>? {
        val channel = client.realtime.channel("groups_realtime_${System.currentTimeMillis()}")

        val flow = channel.postgresChangeFlow<PostgresAction>(schema = "public") {
            table = "groups"
        }
        groupsChannel = channel
        return flow
    }

    fun watchGroupMembers(): Flow<PostgresAction>? {
        val channel = client.realtime.channel("group_members_realtime_${System.currentTimeMillis()}")

        val flow = channel.postgresChangeFlow<PostgresAction>(schema = "public") {
            table = "group_members"
        }
        membersChannel = channel
        return flow
    }

    suspend fun subscribe() {
        if (groupsChannel?.status?.value != RealtimeChannel.Status.SUBSCRIBED) {
            groupsChannel?.subscribe()
        }
        if (membersChannel?.status?.value != RealtimeChannel.Status.SUBSCRIBED) {
            membersChannel?.subscribe()
        }
    }

    suspend fun disconnect() {
        groupsChannel?.let {
            client.realtime.removeChannel(it)
            groupsChannel = null
        }
        membersChannel?.let {
            client.realtime.removeChannel(it)
            membersChannel = null
        }
    }
}