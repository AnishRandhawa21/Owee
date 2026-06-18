package com.owee.app.data.repository

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ReceiptLong
import com.owee.app.data.remote.model.GroupWithMembers
import com.owee.app.domain.calculator.BalanceCalculator
import com.owee.app.domain.calculator.SettlementCalculator
import com.owee.app.domain.model.GroupBalance
import com.owee.app.domain.model.OverallBalance
import com.owee.app.domain.model.Settlement
import com.owee.app.viewmodel.ActivityItemData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

class BalanceRepository(
    private val groupRepository: GroupRepository = GroupRepository(),
    private val expenseRepository: ExpenseRepository = ExpenseRepository()
) {

    suspend fun getOverallBalance(userId: String): OverallBalance = withContext(Dispatchers.IO) {
        val groups = groupRepository.getGroupsForUser(userId)
        val groupBalances = groups.map { groupWithMembers ->
            getGroupBalance(groupWithMembers)
        }
        BalanceCalculator.calculateOverallBalance(userId, groupBalances)
    }

    suspend fun getGroupBalance(groupWithMembers: GroupWithMembers): GroupBalance = withContext(Dispatchers.IO) {
        val expenses = expenseRepository.getExpensesForGroup(groupWithMembers.group.id ?: "")
        BalanceCalculator.calculateGroupBalance(
            groupId = groupWithMembers.group.id ?: "",
            groupName = groupWithMembers.group.name,
            members = groupWithMembers.members,
            expenses = expenses
        )
    }

    suspend fun getRecentActivity(userId: String): List<ActivityItemData> = withContext(Dispatchers.IO) {
        val groups = groupRepository.getGroupsForUser(userId)
        val allExpenses = groups.flatMap { group ->
            expenseRepository.getExpensesForGroup(group.group.id ?: "")
        }.sortedByDescending { it.expense.created_at }
        
        allExpenses.take(10).map { ewp ->
            val isPaidByMe = ewp.expense.paid_by == userId
            val myShare = ewp.participants.find { it.user_id == userId }?.share_amount ?: 0.0
            
            ActivityItemData(
                title = ewp.expense.title,
                subtitle = if (isPaidByMe) "You paid" else "${ewp.paidByUser.name} paid",
                amount = if (isPaidByMe) ewp.expense.amount - myShare else -myShare,
                status = if (isPaidByMe) "LENT" else "OWE",
                icon = Icons.Default.ReceiptLong
            )
        }
    }

    suspend fun getMonthlySpent(userId: String): Double = withContext(Dispatchers.IO) {
        val groups = groupRepository.getGroupsForUser(userId)
        val allExpenses = groups.flatMap { group ->
            expenseRepository.getExpensesForGroup(group.group.id ?: "")
        }
        
        val now = LocalDateTime.now()
        val currentMonth = now.monthValue
        val currentYear = now.year
        
        allExpenses.filter { ewp ->
            val date = LocalDateTime.parse(ewp.expense.created_at, DateTimeFormatter.ISO_DATE_TIME)
            date.monthValue == currentMonth && date.year == currentYear
        }.sumOf { ewp ->
            ewp.participants.find { it.user_id == userId }?.share_amount ?: 0.0
        }
    }

    suspend fun getMostActiveGroup(userId: String): String = withContext(Dispatchers.IO) {
        val groups = groupRepository.getGroupsForUser(userId)
        if (groups.isEmpty()) return@withContext "None"
        
        val groupActivity = groups.associate { group ->
            group.group.name to expenseRepository.getExpensesForGroup(group.group.id ?: "").size
        }
        
        groupActivity.maxByOrNull { it.value }?.key ?: "None"
    }

    fun getSettlements(groupBalance: GroupBalance): List<Settlement> {
        return SettlementCalculator.calculateSettlements(groupBalance.balances)
    }
}
