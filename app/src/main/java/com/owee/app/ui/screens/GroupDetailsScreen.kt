package com.owee.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.owee.app.data.remote.model.ExpenseWithParticipants
import com.owee.app.data.remote.model.MemberBalance
import com.owee.app.data.remote.model.User
import com.owee.app.viewmodel.ExpensesViewModel
import com.owee.app.viewmodel.GroupsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GroupDetailsScreen(
    currentUserId: String,
    groupsViewModel: GroupsViewModel,
    expensesViewModel: ExpensesViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToCreateExpense: () -> Unit,
    onNavigateToExpenseDetails: () -> Unit
) {
    val groupState by groupsViewModel.uiState.collectAsState()
    val expenseState by expensesViewModel.uiState.collectAsState()

    val group = groupState.selectedGroup

    if (group == null) {
        onNavigateBack()
        return
    }

    // Initialize expenses when screen opens
    LaunchedEffect(group.group.id) {
        group.group.id?.let { groupId ->
            expensesViewModel.initialize(
                groupId = groupId,
                currentUserId = currentUserId,
                members = group.members
            )
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = group.group.name,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                navigationIcon = {
                    IconButton(onClick = {
                        groupsViewModel.clearSelectedGroup()
                        onNavigateBack()
                    }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onNavigateToCreateExpense,
                containerColor = MaterialTheme.colorScheme.primary
            ) {
                Icon(
                    Icons.Default.Add,
                    contentDescription = "Add Expense",
                    tint = MaterialTheme.colorScheme.onPrimary
                )
            }
        }
    ) { padding ->

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {

            // ─── Group Summary ────────────────────────────────────────────────
            item {
                GroupSummaryCard(
                    groupName = group.group.name,
                    memberCount = group.memberCount
                )
            }

            // ─── Balances Section ─────────────────────────────────────────────
            item {
                Text(
                    text = "Balances",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(vertical = 4.dp)
                )
            }

            if (expenseState.isLoading) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp))
                    }
                }
            } else if (expenseState.memberBalances.isEmpty()) {
                item { BalancesEmptyState() }
            } else {
                items(expenseState.memberBalances, key = { "balance_${it.user.id}" }) { balance ->
                    BalanceRow(
                        balance = balance,
                        isCurrentUser = balance.user.id == currentUserId
                    )
                }
            }

            // ─── Expenses Section ─────────────────────────────────────────────
            item {
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "Expenses",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(vertical = 4.dp)
                )
            }

            if (!expenseState.isLoading && expenseState.expenses.isEmpty()) {
                item { ExpensesEmptyState() }
            } else {
                items(
                    expenseState.expenses,
                    key = { "expense_${it.expense.id}" }
                ) { expenseWithParticipants ->
                    ExpenseRow(
                        expenseWithParticipants = expenseWithParticipants,
                        currentUserId = currentUserId,
                        onClick = {
                            expensesViewModel.selectExpense(expenseWithParticipants)
                            onNavigateToExpenseDetails()
                        }
                    )
                }
            }

            // ─── Members Section ──────────────────────────────────────────────
            item {
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "Members",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(vertical = 4.dp)
                )
            }

            items(group.members, key = { "member_${it.id}" }) { member ->
                val role = if (member.id == group.group.created_by) "owner" else "member"
                MemberRow(user = member, role = role)
            }

            item { Spacer(Modifier.height(80.dp)) }
        }
    }
}

// ─── Group Summary Card ───────────────────────────────────────────────────────

@Composable
private fun GroupSummaryCard(groupName: String, memberCount: Int) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                text = groupName,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = "$memberCount Members",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
            )
        }
    }
}

// ─── Balance Row ──────────────────────────────────────────────────────────────

@Composable
private fun BalanceRow(balance: MemberBalance, isCurrentUser: Boolean) {
    val isPositive = balance.netBalance > 0.01
    val isNegative = balance.netBalance < -0.01
    val isSettled = !isPositive && !isNegative

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Avatar
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.secondary.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Person,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.secondary,
                modifier = Modifier.size(20.dp)
            )
        }

        Spacer(Modifier.width(12.dp))

        // Name
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = balance.user.name,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium
                )
                if (isCurrentUser) {
                    Spacer(Modifier.width(6.dp))
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                    ) {
                        Text(
                            text = "You",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                        )
                    }
                }
            }
            Text(
                text = when {
                    isPositive -> "gets back ₹${"%.2f".format(balance.netBalance)}"
                    isNegative -> "owes ₹${"%.2f".format(-balance.netBalance)}"
                    else -> "settled up"
                },
                style = MaterialTheme.typography.bodySmall,
                color = when {
                    isPositive -> MaterialTheme.colorScheme.primary
                    isNegative -> MaterialTheme.colorScheme.error
                    else -> MaterialTheme.colorScheme.onSurfaceVariant
                }
            )
        }

        // Net amount
        Text(
            text = when {
                isSettled -> "✓"
                isPositive -> "+₹${"%.2f".format(balance.netBalance)}"
                else -> "-₹${"%.2f".format(-balance.netBalance)}"
            },
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold,
            color = when {
                isPositive -> MaterialTheme.colorScheme.primary
                isNegative -> MaterialTheme.colorScheme.error
                else -> MaterialTheme.colorScheme.onSurfaceVariant
            }
        )
    }
}

// ─── Expense Row ──────────────────────────────────────────────────────────────

@Composable
private fun ExpenseRow(
    expenseWithParticipants: ExpenseWithParticipants,
    currentUserId: String,
    onClick: () -> Unit
) {
    val expense = expenseWithParticipants.expense
    val paidByYou = expense.paid_by == currentUserId
    val yourShare = expenseWithParticipants.participants
        .find { it.user_id == currentUserId }?.share_amount ?: 0.0

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surface)
            .clickable { onClick() }
            .padding(horizontal = 14.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Icon
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center
        ) {
            Text(text = "💸", style = MaterialTheme.typography.titleMedium)
        }

        Spacer(Modifier.width(12.dp))

        // Details
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = expense.title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = if (paidByYou) "You paid" else "Paid by ${expenseWithParticipants.paidByUser.name}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Spacer(Modifier.width(8.dp))

        // Amount
        Column(horizontalAlignment = Alignment.End) {
            Text(
                text = "₹${"%.2f".format(expense.amount)}",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold
            )
            if (yourShare > 0) {
                Text(
                    text = if (paidByYou)
                        "you lent ₹${"%.2f".format(expense.amount - yourShare)}"
                    else
                        "you owe ₹${"%.2f".format(yourShare)}",
                    style = MaterialTheme.typography.labelSmall,
                    color = if (paidByYou)
                        MaterialTheme.colorScheme.primary
                    else
                        MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

// ─── Member Row ───────────────────────────────────────────────────────────────

@Composable
private fun MemberRow(user: User, role: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.secondary.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Person,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.secondary,
                modifier = Modifier.size(22.dp)
            )
        }

        Spacer(Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = user.name,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = "@${user.username}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        if (role == "owner") {
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.primaryContainer
            ) {
                Text(
                    text = "Creator",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                )
            }
        }
    }
}

// ─── Empty States ─────────────────────────────────────────────────────────────

@Composable
private fun BalancesEmptyState() {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "No balances yet — add an expense to get started",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
            )
        }
    }
}

@Composable
private fun ExpensesEmptyState() {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "💸  No expenses yet",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
            )
        }
    }
}