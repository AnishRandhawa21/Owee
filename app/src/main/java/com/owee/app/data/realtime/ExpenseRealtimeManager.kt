package com.owee.app.data.realtime

import com.owee.app.data.remote.SupabaseProvider
import io.github.jan.supabase.realtime.PostgresAction
import io.github.jan.supabase.realtime.Realtime
import io.github.jan.supabase.realtime.RealtimeChannel
import io.github.jan.supabase.realtime.channel
import io.github.jan.supabase.realtime.postgresChangeFlow
import io.github.jan.supabase.realtime.realtime
import kotlinx.coroutines.flow.Flow

class ExpenseRealtimeManager {

    private val client = SupabaseProvider.client

    private var expensesChannel: RealtimeChannel? = null
    private var participantsChannel: RealtimeChannel? = null

    // ─── Connect ──────────────────────────────────────────────────────────────

    suspend fun connect() {
        if (client.realtime.status.value != Realtime.Status.CONNECTED) {
            client.realtime.connect()
        }
    }

    // ─── Watch ────────────────────────────────────────────────────────────────

    fun watchExpenses(): Flow<PostgresAction>? {
        val channel = client.realtime.channel(
            "expenses_realtime_${System.currentTimeMillis()}"
        )
        val flow = channel.postgresChangeFlow<PostgresAction>(schema = "public") {
            table = "expenses"
        }
        expensesChannel = channel
        return flow
    }

    fun watchExpenseParticipants(): Flow<PostgresAction>? {
        val channel = client.realtime.channel(
            "expense_participants_realtime_${System.currentTimeMillis()}"
        )
        val flow = channel.postgresChangeFlow<PostgresAction>(schema = "public") {
            table = "expense_participants"
        }
        participantsChannel = channel
        return flow
    }

    // ─── Subscribe ────────────────────────────────────────────────────────────

    suspend fun subscribe() {
        if (expensesChannel?.status?.value != RealtimeChannel.Status.SUBSCRIBED) {
            expensesChannel?.subscribe()
        }
        if (participantsChannel?.status?.value != RealtimeChannel.Status.SUBSCRIBED) {
            participantsChannel?.subscribe()
        }
    }

    // ─── Disconnect ───────────────────────────────────────────────────────────

    suspend fun disconnect() {
        expensesChannel?.let {
            client.realtime.removeChannel(it)
            expensesChannel = null
        }
        participantsChannel?.let {
            client.realtime.removeChannel(it)
            participantsChannel = null
        }
    }
}