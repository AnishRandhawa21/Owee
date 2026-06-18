package com.owee.app.data.repository

import com.owee.app.data.remote.model.GroupWithMembers
import com.owee.app.domain.calculator.BalanceCalculator
import com.owee.app.domain.calculator.SettlementCalculator
import com.owee.app.domain.model.GroupBalance
import com.owee.app.domain.model.OverallBalance
import com.owee.app.domain.model.Settlement
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

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

    fun getSettlements(groupBalance: GroupBalance): List<Settlement> {
        return SettlementCalculator.calculateSettlements(groupBalance.balances)
    }
}
