package com.owee.app.data.remote.model

data class ExpenseWithParticipants(
    val expense: Expense,
    val paidByUser: User,
    val participants: List<ExpenseParticipant>,
    val participantUsers: List<User>
)