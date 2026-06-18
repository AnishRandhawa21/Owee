package com.owee.app.ui.screens.groups

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.owee.app.data.remote.model.ExpenseWithParticipants
import com.owee.app.viewmodel.BalanceViewModel
import com.owee.app.viewmodel.ExpensesViewModel
import com.owee.app.viewmodel.GroupsViewModel
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GroupDetailsScreen(
    currentUserId: String,
    groupsViewModel: GroupsViewModel,
    expensesViewModel: ExpensesViewModel,
    balanceViewModel: BalanceViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToCreateExpense: () -> Unit,
    onNavigateToExpenseDetails: () -> Unit,
    onNavigateToSettlement: () -> Unit
) {
    val groupState by groupsViewModel.uiState.collectAsState()
    val expenseState by expensesViewModel.uiState.collectAsState()

    val group = groupState.selectedGroup

    if (group == null) {
        onNavigateBack()
        return
    }

    LaunchedEffect(group) {
        group.group.id?.let { groupId ->
            expensesViewModel.initialize(
                groupId = groupId,
                currentUserId = currentUserId,
                members = group.members
            )
            balanceViewModel.initialize(currentUserId)
        }
    }

    if (expenseState.error != null) {
        AlertDialog(
            onDismissRequest = { expensesViewModel.clearMessage() },
            title = { Text("Error") },
            text = { Text(expenseState.error ?: "An unknown error occurred") },
            confirmButton = { TextButton(onClick = { expensesViewModel.clearMessage() }) { Text("OK") } }
        )
    }

    PullToRefreshBox(
        isRefreshing = expenseState.isLoading,
        onRefresh = { 
            expensesViewModel.refresh()
            balanceViewModel.refresh()
        },
        modifier = Modifier.fillMaxSize()
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .imePadding(),
            contentPadding = PaddingValues(bottom = 24.dp)
        ) {
            // Group Header Summary
            item {
                val totalExpenses = expenseState.expenses.sumOf { it.expense.amount }
                GroupHeaderCard(
                    groupName = group.group.name,
                    memberCount = group.memberCount,
                    totalExpenses = totalExpenses
                )
            }

            // Tabs / Section Headers
            item {
                SectionHeader("Members & Balances")
            }

            if (expenseState.isLoading && expenseState.memberBalances.isEmpty()) {
                item { LoadingState() }
            } else if (expenseState.memberBalances.isEmpty()) {
                item { BalancesEmptyState() }
            } else {
                items(expenseState.memberBalances, key = { "balance_${it.user.id}" }) { mb ->
                    val isOwner = mb.user.id == group.group.created_by
                    MemberBalanceItem(
                        name = mb.user.name,
                        photoUrl = mb.user.photo_url,
                        amount = mb.netBalance,
                        isCurrentUser = mb.user.id == currentUserId,
                        isOwner = isOwner
                    )
                }
            }

            item {
                SectionHeader("Expenses")
            }

            if (!expenseState.isLoading && expenseState.expenses.isEmpty()) {
                item { ExpensesEmptyState() }
            } else {
                items(
                    expenseState.expenses,
                    key = { "expense_${it.expense.id}" }
                ) { ewp ->
                    ExpenseListItem(
                        ewp = ewp,
                        currentUserId = currentUserId,
                        onClick = {
                            expensesViewModel.selectExpense(ewp)
                            onNavigateToExpenseDetails()
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun SectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp)
    )
}

@Composable
private fun GroupHeaderCard(groupName: String, memberCount: Int, totalExpenses: Double) {
    val isDark = isSystemInDarkTheme()
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isDark) MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isDark) 0.dp else 2.dp)
    ) {
        Column(modifier = Modifier.padding(24.dp)) {
            Text(
                text = groupName.uppercase(),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = "₹${String.format(Locale.US, "%.2f", totalExpenses)}",
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(Modifier.height(12.dp))
            Surface(
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    text = "$memberCount Members",
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

@Composable
private fun MemberBalanceItem(
    name: String, 
    photoUrl: String?, 
    amount: Double, 
    isCurrentUser: Boolean,
    isOwner: Boolean
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AsyncImage(
            model = photoUrl,
            contentDescription = null,
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceVariant),
            contentScale = ContentScale.Crop,
            fallback = androidx.compose.ui.graphics.vector.rememberVectorPainter(Icons.Default.Person)
        )

        Spacer(Modifier.width(16.dp))

        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = if (isCurrentUser) "You" else name,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                
                if (isOwner) {
                    Spacer(Modifier.width(8.dp))
                    Surface(
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = "OWNER",
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Black,
                            fontSize = 8.sp,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                } else {
                    Spacer(Modifier.width(8.dp))
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = "MEMBER",
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Black,
                            fontSize = 8.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            val isOwed = amount > 0.01
            val isOwe = amount < -0.01
            Text(
                text = when {
                    isOwed -> "gets back ₹${String.format(Locale.US, "%.2f", amount)}"
                    isOwe -> "owes ₹${String.format(Locale.US, "%.2f", kotlin.math.abs(amount))}"
                    else -> "settled up"
                },
                style = MaterialTheme.typography.bodySmall,
                color = when {
                    isOwed -> MaterialTheme.colorScheme.secondary
                    isOwe -> MaterialTheme.colorScheme.error
                    else -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                }
            )
        }

        if (kotlin.math.abs(amount) > 0.01) {
            Text(
                text = (if (amount > 0) "+" else "-") + "₹${String.format(Locale.US, "%.2f", kotlin.math.abs(amount))}",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                color = if (amount > 0) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.error
            )
        }
    }
}

@Composable
private fun ExpenseListItem(
    ewp: ExpenseWithParticipants,
    currentUserId: String,
    onClick: () -> Unit
) {
    val expense = ewp.expense
    val paidByYou = expense.paid_by == currentUserId
    
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            modifier = Modifier.size(48.dp),
            shape = RoundedCornerShape(14.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text("💸", fontSize = 20.sp)
            }
        }

        Spacer(Modifier.width(16.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = expense.title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = if (paidByYou) "You paid" else "Paid by ${ewp.paidByUser.name}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
            )
        }

        Column(horizontalAlignment = Alignment.End) {
            Text(
                text = "₹${String.format(Locale.US, "%.2f", expense.amount)}",
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Bold
            )
            
            val yourShare = ewp.participants.find { it.user_id == currentUserId }?.share_amount ?: 0.0
            if (yourShare > 0) {
                Text(
                    text = if (paidByYou) "you lent" else "you owe",
                    style = MaterialTheme.typography.labelSmall,
                    color = if (paidByYou) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.error,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun LoadingState() {
    Box(Modifier.fillMaxWidth().padding(40.dp), contentAlignment = Alignment.Center) {
        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
    }
}

@Composable
private fun BalancesEmptyState() {
    Text(
        "No balances yet",
        modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
    )
}

@Composable
private fun ExpensesEmptyState() {
    Text(
        "No expenses recorded yet",
        modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
    )
}
