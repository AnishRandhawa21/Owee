package com.owee.app.domain.calculator

import com.owee.app.data.remote.model.ExpenseWithParticipants
import com.owee.app.data.remote.model.User
import com.owee.app.domain.model.GroupBalance
import com.owee.app.domain.model.UserBalance
import com.owee.app.domain.model.OverallBalance

object BalanceCalculator {

    fun calculateGroupBalance(
        groupId: String,
        groupName: String,
        members: List<User>,
        expenses: List<ExpenseWithParticipants>
    ): GroupBalance {
        val balancesMap = mutableMapOf<String, Double>()
        var totalExpenses = 0.0

        // Initialize all members with 0.0 balance
        members.forEach { user ->
            user.id?.let { balancesMap[it] = 0.0 }
        }

        // Only calculate balances for expenses belonging to THIS group
        expenses.filter { it.expense.group_id == groupId }.forEach { ewp ->
            totalExpenses += ewp.expense.amount
            
            // Payer gets credited the full amount
            val payerId = ewp.expense.paid_by
            balancesMap[payerId] = (balancesMap[payerId] ?: 0.0) + ewp.expense.amount

            // Each participant owes their share
            ewp.participants.forEach { participant ->
                balancesMap[participant.user_id] = 
                    (balancesMap[participant.user_id] ?: 0.0) - participant.share_amount
            }
        }

        val userBalances = members.mapNotNull { user ->
            val id = user.id ?: return@mapNotNull null
            UserBalance(
                userId = id,
                userName = user.name,
                userPhotoUrl = user.photo_url,
                amount = balancesMap[id] ?: 0.0
            )
        }.sortedByDescending { it.amount }

        return GroupBalance(
            groupId = groupId,
            groupName = groupName,
            balances = userBalances,
            totalExpenses = totalExpenses
        )
    }

    fun calculateOverallBalance(
        currentUserId: String,
        groupBalances: List<GroupBalance>
    ): OverallBalance {
        var totalOwed = 0.0
        var totalOwe = 0.0

        groupBalances.forEach { gb ->
            val myBalance = gb.balances.find { it.userId == currentUserId }?.amount ?: 0.0
            if (myBalance > 0) {
                totalOwed += myBalance
            } else if (myBalance < 0) {
                totalOwe += kotlin.math.abs(myBalance)
            }
        }

        return OverallBalance(
            totalOwed = totalOwed,
            totalOwe = totalOwe,
            groupBalances = groupBalances
        )
    }
}
