package com.owee.app.data.repository

import com.owee.app.data.remote.SupabaseProvider
import com.owee.app.data.remote.model.Expense
import com.owee.app.data.remote.model.ExpenseParticipant
import com.owee.app.data.remote.model.ExpenseWithParticipants
import com.owee.app.data.remote.model.MemberBalance
import com.owee.app.data.remote.model.User
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Order

class ExpenseRepository {

    // ─── Create ───────────────────────────────────────────────────────────────

    suspend fun createExpense(
        groupId: String,
        title: String,
        amount: Double,
        paidBy: String,
        participantIds: List<String>,       // always includes paidBy
        splitType: String,
        customAmounts: Map<String, Double>  // only used when splitType == "custom"
    ): Expense? {
        return try {
            // 1. Insert expense
            val expense = SupabaseProvider.client
                .from("expenses")
                .insert(
                    Expense(
                        group_id = groupId,
                        title = title.trim(),
                        amount = amount,
                        paid_by = paidBy,
                        split_type = splitType
                    )
                ) {
                    select()
                }
                .decodeSingle<Expense>()

            val expenseId = expense.id ?: return null

            // 2. Calculate share per participant
            val participants = participantIds.map { userId ->
                val share = when (splitType) {
                    "custom" -> customAmounts[userId] ?: 0.0
                    else -> amount / participantIds.size  // equal split
                }
                ExpenseParticipant(
                    expense_id = expenseId,
                    user_id = userId,
                    share_amount = share
                )
            }

            // 3. Insert participants
            SupabaseProvider.client
                .from("expense_participants")
                .insert(participants)

            android.util.Log.d("ExpenseRepository", "Expense created: $expenseId")
            expense

        } catch (e: Exception) {
        android.util.Log.e("ExpenseRepository", "createExpense failed: ${e.message}", e)
        android.util.Log.e("ExpenseRepository", "cause: ${e.cause}")
        android.util.Log.e("ExpenseRepository", "stacktrace: ${e.stackTraceToString()}")
        null
    }
    }

    // ─── Read ─────────────────────────────────────────────────────────────────

    suspend fun getExpensesForGroup(groupId: String): List<ExpenseWithParticipants> {
        return try {
            // 1. Fetch all expenses for the group
            val expenses = SupabaseProvider.client
                .from("expenses")
                .select {
                    filter { eq("group_id", groupId) }
                    order("created_at", Order.DESCENDING)
                }
                .decodeList<Expense>()

            if (expenses.isEmpty()) return emptyList()

            val expenseIds = expenses.mapNotNull { it.id }

            // 2. Fetch all participants in one query
            val allParticipants = SupabaseProvider.client
                .from("expense_participants")
                .select {
                    filter { isIn("expense_id", expenseIds) }
                }
                .decodeList<ExpenseParticipant>()

            // 3. Collect all unique user IDs
            val paidByIds = expenses.map { it.paid_by }
            val participantUserIds = allParticipants.map { it.user_id }
            val allUserIds = (paidByIds + participantUserIds).distinct()

            // 4. Fetch user profiles in one query
            val users = SupabaseProvider.client
                .from("users")
                .select {
                    filter { isIn("id", allUserIds) }
                }
                .decodeList<User>()

            val userMap = users.associateBy { it.id }

            // 5. Assemble
            expenses.mapNotNull { expense ->
                val paidByUser = userMap[expense.paid_by] ?: return@mapNotNull null
                val participants = allParticipants.filter { it.expense_id == expense.id }
                val participantUsers = participants.mapNotNull { userMap[it.user_id] }
                ExpenseWithParticipants(
                    expense = expense,
                    paidByUser = paidByUser,
                    participants = participants,
                    participantUsers = participantUsers
                )
            }

        } catch (e: Exception) {
            android.util.Log.e("ExpenseRepository", "getExpensesForGroup failed", e)
            emptyList()
        }
    }

    // ─── Balance Calculation ──────────────────────────────────────────────────

    fun calculateBalances(
        members: List<User>,
        expenses: List<ExpenseWithParticipants>
    ): List<MemberBalance> {

        // Track total paid and total owed per user
        val totalPaid = mutableMapOf<String, Double>()
        val totalOwed = mutableMapOf<String, Double>()

        // Initialize all members at 0
        members.forEach { user ->
            user.id?.let {
                totalPaid[it] = 0.0
                totalOwed[it] = 0.0
            }
        }

        // Accumulate across all expenses
        expenses.forEach { ewp ->
            val payerId = ewp.expense.paid_by

            // Payer gets credited the full amount
            totalPaid[payerId] = (totalPaid[payerId] ?: 0.0) + ewp.expense.amount

            // Each participant owes their share
            ewp.participants.forEach { participant ->
                totalOwed[participant.user_id] =
                    (totalOwed[participant.user_id] ?: 0.0) + participant.share_amount
            }
        }

        // Build MemberBalance list
        return members.mapNotNull { user ->
            val id = user.id ?: return@mapNotNull null
            val paid = totalPaid[id] ?: 0.0
            val owed = totalOwed[id] ?: 0.0
            MemberBalance(
                user = user,
                totalPaid = paid,
                totalOwed = owed,
                netBalance = paid - owed  // positive = owed back, negative = owes others
            )
        }.sortedByDescending { it.netBalance }
    }
}