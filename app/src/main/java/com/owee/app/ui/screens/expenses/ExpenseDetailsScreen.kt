package com.owee.app.ui.screens.expenses

import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.owee.app.data.remote.model.ExpenseWithParticipants
import com.owee.app.data.remote.model.User
import com.owee.app.viewmodel.ExpensesViewModel
import java.util.*

@Composable
fun ExpenseDetailsScreen(
    currentUserId: String,
    expensesViewModel: ExpensesViewModel,
    onNavigateBack: () -> Unit
) {
    val state by expensesViewModel.uiState.collectAsState()
    val expense = state.selectedExpense

    if (expense == null) {
        onNavigateBack()
        return
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(bottom = 24.dp)
    ) {
        // Summary Header
        item {
            ExpenseHeader(expense, currentUserId)
        }

        item {
            SectionHeader("Paid by")
        }

        item {
            PaidByItem(expense.paidByUser, expense.expense.amount, expense.expense.paid_by == currentUserId)
        }

        item {
            SectionHeader("Split between")
        }

        items(
            expense.participants,
            key = { it.user_id }
        ) { participant ->
            val user = expense.participantUsers.find { it.id == participant.user_id }
            if (user != null) {
                ParticipantListItem(
                    user = user,
                    amount = participant.share_amount,
                    isCurrentUser = participant.user_id == currentUserId
                )
            }
        }
        
        item {
            Spacer(Modifier.height(20.dp))
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
                horizontalArrangement = Arrangement.Center
            ) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                ) {
                    Text(
                        text = if (expense.expense.split_type == "equal") "⚖️  Split equally" else "✏️  Custom split",
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
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
private fun ExpenseHeader(ewp: ExpenseWithParticipants, currentUserId: String) {
    val isDark = isSystemInDarkTheme()
    val yourShare = ewp.participants.find { it.user_id == currentUserId }?.share_amount ?: 0.0
    val youPaid = ewp.expense.paid_by == currentUserId

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
                text = ewp.expense.title.uppercase(),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = "₹${String.format(Locale.US, "%.2f", ewp.expense.amount)}",
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(Modifier.height(16.dp))
            
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = if (youPaid) MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.4f) else MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.4f)
            ) {
                Text(
                    text = if (youPaid) 
                        "You lent ₹${String.format(Locale.US, "%.2f", ewp.expense.amount - yourShare)}"
                    else 
                        "You owe ₹${String.format(Locale.US, "%.2f", yourShare)}",
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (youPaid) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

@Composable
private fun PaidByItem(user: User, amount: Double, isCurrentUser: Boolean) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AsyncImage(
            model = user.photo_url,
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
            Text(
                text = if (isCurrentUser) "You" else user.name,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold
            )
            Text(text = "Paid total amount", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f))
        }
        Text(
            text = "₹${String.format(Locale.US, "%.2f", amount)}",
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.ExtraBold,
            color = MaterialTheme.colorScheme.primary
        )
    }
}

@Composable
private fun ParticipantListItem(user: User, amount: Double, isCurrentUser: Boolean) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AsyncImage(
            model = user.photo_url,
            contentDescription = null,
            modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceVariant),
            contentScale = ContentScale.Crop,
            fallback = androidx.compose.ui.graphics.vector.rememberVectorPainter(Icons.Default.Person)
        )
        Spacer(Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = if (isCurrentUser) "You" else user.name,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold
            )
            Text(text = "Their share", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f))
        }
        Text(
            text = "₹${String.format(Locale.US, "%.2f", amount)}",
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}
