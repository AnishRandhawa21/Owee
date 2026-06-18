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

/**
 * Manages a single shared Realtime channel for expense-related table changes.
 *
 * IMPORTANT: Use [ExpenseRealtimeManager.shared] instead of constructing a new instance.
 * Both BalanceViewModel and ExpensesViewModel must share the same instance so that
 * only one channel subscription is open at a time. Creating separate instances
 * produces duplicate channel subscriptions on the same tables, wastes connections,
 * and can deliver duplicate events to each ViewModel.
 *
 * If you are using a DI framework (e.g. Hilt), bind this as a @Singleton.
 */
class ExpenseRealtimeManager private constructor() {

    private val client = SupabaseProvider.client
    private var channel: RealtimeChannel? = null
    private var subscriberCount = 0

    // ─── Lifecycle ────────────────────────────────────────────────────────────

    suspend fun connect() {
        try {
            if (client.realtime.status.value != Realtime.Status.CONNECTED) {
                Log.d(TAG, "Connecting to Supabase Realtime…")
                client.realtime.connect()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error connecting to Realtime", e)
        }
    }

    /**
     * Returns the shared channel, creating it only once.
     * Callers: call [connect] first, then [watchExpenses]/[watchExpenseParticipants],
     * then [subscribe].
     */
    private fun getOrCreateChannel(): RealtimeChannel {
        if (channel == null) {
            // A stable, fixed name ensures both ViewModels see the same channel object.
            channel = client.realtime.channel("expense_updates_shared")
            Log.d(TAG, "Created shared channel: expense_updates_shared")
        }
        return channel!!
    }

    /**
     * Returns a [Flow] of [PostgresAction] for the `expenses` table.
     * Must be called before [subscribe].
     */
    fun watchExpenses(): Flow<PostgresAction> {
        Log.d(TAG, "Registering watch for 'expenses' table")
        return getOrCreateChannel().postgresChangeFlow<PostgresAction>(schema = "public") {
            table = "expenses"
        }
    }

    /**
     * Returns a [Flow] of [PostgresAction] for the `expense_participants` table.
     * Must be called before [subscribe].
     */
    fun watchExpenseParticipants(): Flow<PostgresAction> {
        Log.d(TAG, "Registering watch for 'expense_participants' table")
        return getOrCreateChannel().postgresChangeFlow<PostgresAction>(schema = "public") {
            table = "expense_participants"
        }
    }

    /**
     * Subscribes the channel to Supabase Realtime.
     * Reference-counted: the channel is subscribed on the first call and is a no-op
     * for subsequent callers (BalanceViewModel + ExpensesViewModel both call this).
     */
    suspend fun subscribe() {
        subscriberCount++
        if (subscriberCount == 1) {
            try {
                channel?.subscribe()
                Log.d(TAG, "Channel subscribed (subscriber count: $subscriberCount)")
            } catch (e: Exception) {
                Log.e(TAG, "Error subscribing to channel", e)
                subscriberCount-- // Roll back on failure
            }
        } else {
            Log.d(TAG, "Channel already subscribed (subscriber count: $subscriberCount)")
        }
    }

    /**
     * Decrements the subscriber count and removes the channel only when the last
     * subscriber disconnects. Call this from each ViewModel's [onCleared].
     */
    suspend fun disconnect() {
        subscriberCount = (subscriberCount - 1).coerceAtLeast(0)
        Log.d(TAG, "disconnect() called (subscriber count after: $subscriberCount)")
        if (subscriberCount == 0) {
            try {
                channel?.let {
                    client.realtime.removeChannel(it)
                    Log.d(TAG, "Channel removed — no more subscribers")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error disconnecting channel", e)
            } finally {
                channel = null
            }
        }
    }

    // ─── Singleton ────────────────────────────────────────────────────────────

    companion object {
        private const val TAG = "ExpenseRealtime"

        @Volatile
        private var instance: ExpenseRealtimeManager? = null

        /**
         * Returns the process-wide singleton. Inject this into both
         * [BalanceViewModel] and [ExpensesViewModel].
         */
        fun shared(): ExpenseRealtimeManager =
            instance ?: synchronized(this) {
                instance ?: ExpenseRealtimeManager().also { instance = it }
            }
    }
}