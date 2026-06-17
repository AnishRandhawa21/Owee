package com.owee.app.data.remote.model

data class MemberBalance(
    val user: User,
    val totalPaid: Double,      // how much this person paid across all expenses
    val totalOwed: Double,      // how much this person owes across all expenses
    val netBalance: Double      // totalPaid - totalOwed → positive = owed money back, negative = owes money
)