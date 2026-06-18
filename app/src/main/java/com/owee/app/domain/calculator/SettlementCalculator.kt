package com.owee.app.domain.calculator

import com.owee.app.domain.model.Settlement
import com.owee.app.domain.model.UserBalance
import kotlin.math.abs
import kotlin.math.min

object SettlementCalculator {

    fun calculateSettlements(balances: List<UserBalance>): List<Settlement> {
        val debtors = balances.filter { it.amount < -0.01 }
            .map { it.copy() }
            .sortedBy { it.amount } // Most negative first
            .toMutableList()
            
        val creditors = balances.filter { it.amount > 0.01 }
            .map { it.copy() }
            .sortedByDescending { it.amount } // Most positive first
            .toMutableList()

        val settlements = mutableListOf<Settlement>()

        var dIdx = 0
        var cIdx = 0

        while (dIdx < debtors.size && cIdx < creditors.size) {
            val debtor = debtors[dIdx]
            val creditor = creditors[cIdx]

            val amountToSettle = min(abs(debtor.amount), creditor.amount)
            
            if (amountToSettle > 0.01) {
                settlements.add(
                    Settlement(
                        fromUserId = debtor.userId,
                        fromUserName = debtor.userName,
                        toUserId = creditor.userId,
                        toUserName = creditor.userName,
                        amount = amountToSettle
                    )
                )
            }

            // Update remaining amounts
            val newDebtorAmount = debtor.amount + amountToSettle
            val newCreditorAmount = creditor.amount - amountToSettle

            debtors[dIdx] = debtor.copy(amount = newDebtorAmount)
            creditors[cIdx] = creditor.copy(amount = newCreditorAmount)

            if (abs(debtors[dIdx].amount) < 0.01) dIdx++
            if (abs(creditors[cIdx].amount) < 0.01) cIdx++
        }

        return settlements
    }
}
