package com.owee.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.owee.app.data.remote.model.User
import com.owee.app.viewmodel.ExpensesViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateExpenseScreen(
    currentUserId: String,
    members: List<User>,
    expensesViewModel: ExpensesViewModel,
    onNavigateBack: () -> Unit,
    onExpenseCreated: () -> Unit
) {
    val state by expensesViewModel.uiState.collectAsState()

    // Auto-dismiss on success
    LaunchedEffect(state.message) {
        if (state.message != null) {
            expensesViewModel.clearMessage()
            onExpenseCreated()
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {

        TopAppBar(
            title = { Text("Add Expense", fontWeight = FontWeight.SemiBold) },
            navigationIcon = {
                IconButton(onClick = {
                    expensesViewModel.resetForm()
                    onNavigateBack()
                }) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                }
            }
        )

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {

            // ─── Title ────────────────────────────────────────────────────────
            item {
                OutlinedTextField(
                    value = state.titleInput,
                    onValueChange = expensesViewModel::onTitleChanged,
                    label = { Text("What was it for?") },
                    placeholder = { Text("e.g. Dinner, Hotel, Fuel") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp)
                )
            }

            // ─── Amount ───────────────────────────────────────────────────────
            item {
                OutlinedTextField(
                    value = state.amountInput,
                    onValueChange = expensesViewModel::onAmountChanged,
                    label = { Text("Amount (₹)") },
                    placeholder = { Text("0.00") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    shape = RoundedCornerShape(12.dp)
                )
            }

            // ─── Paid By (current user, read-only) ────────────────────────────
            item {
                val currentUser = members.find { it.id == currentUserId }
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.primaryContainer
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Paid By",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                            )
                            Text(
                                text = currentUser?.name ?: "You",
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                        ) {
                            Text(
                                text = "You",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }
                    }
                }
            }

            // ─── Split Type Toggle ────────────────────────────────────────────
            item {
                Text(
                    text = "Split Type",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf("equal", "custom").forEach { type ->
                        val selected = state.splitType == type
                        Surface(
                            modifier = Modifier
                                .weight(1f)
                                .clickable { expensesViewModel.onSplitTypeChanged(type) },
                            shape = RoundedCornerShape(10.dp),
                            color = if (selected)
                                MaterialTheme.colorScheme.primary
                            else
                                MaterialTheme.colorScheme.surfaceVariant
                        ) {
                            Text(
                                text = if (type == "equal") "Equal Split" else "Custom Split",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium,
                                color = if (selected)
                                    MaterialTheme.colorScheme.onPrimary
                                else
                                    MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier
                                    .padding(vertical = 12.dp)
                                    .wrapContentWidth(Alignment.CenterHorizontally)
                            )
                        }
                    }
                }
            }

            // ─── Participants ─────────────────────────────────────────────────
            item {
                Text(
                    text = "Split Between",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(Modifier.height(8.dp))
            }

            items(members, key = { it.id ?: "" }) { member ->
                val isSelected = member.id in state.selectedParticipantIds
                val isCurrentUser = member.id == currentUserId
                val shareText = state.customAmounts[member.id]

                ParticipantRow(
                    user = member,
                    isSelected = isSelected,
                    isCurrentUser = isCurrentUser,
                    splitType = state.splitType,
                    shareAmount = shareText,
                    onToggle = {
                        member.id?.let { expensesViewModel.onParticipantToggled(it) }
                    },
                    onCustomAmountChanged = { amount ->
                        member.id?.let {
                            expensesViewModel.onCustomAmountChanged(it, amount)
                        }
                    }
                )
            }

            // ─── Share Preview (equal split) ──────────────────────────────────
            val amount = state.amountInput.toDoubleOrNull() ?: 0.0
            val count = state.selectedParticipantIds.size
            if (state.splitType == "equal" && amount > 0 && count > 0) {
                item {
                    SharePreviewBanner(
                        shareAmount = amount / count,
                        participantCount = count,
                        totalAmount = amount
                    )
                }
            }

            // ─── Custom Split Remaining ───────────────────────────────────────
            if (state.splitType == "custom" && amount > 0) {
                item {
                    val entered = state.selectedParticipantIds.sumOf {
                        state.customAmounts[it]?.toDoubleOrNull() ?: 0.0
                    }
                    val remaining = amount - entered
                    CustomSplitStatus(
                        totalAmount = amount,
                        enteredAmount = entered,
                        remaining = remaining
                    )
                }
            }

            // ─── Error ────────────────────────────────────────────────────────
            if (state.error != null) {
                item {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.errorContainer
                    ) {
                        Text(
                            text = state.error!!,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(12.dp)
                        )
                    }
                }
            }

            // ─── Save Button ──────────────────────────────────────────────────
            item {
                Button(
                    onClick = { expensesViewModel.validateAndCreate() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    enabled = !state.isCreating,
                    shape = RoundedCornerShape(14.dp)
                ) {
                    if (state.isCreating) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = MaterialTheme.colorScheme.onPrimary,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text(
                            text = "Add Expense",
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                Spacer(Modifier.height(32.dp))
            }
        }
    }
}

// ─── Participant Row ──────────────────────────────────────────────────────────

@Composable
private fun ParticipantRow(
    user: User,
    isSelected: Boolean,
    isCurrentUser: Boolean,
    splitType: String,
    shareAmount: String?,
    onToggle: () -> Unit,
    onCustomAmountChanged: (String) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(
                if (isSelected)
                    MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                else
                    MaterialTheme.colorScheme.surfaceVariant
            )
            .clickable(enabled = !isCurrentUser) { onToggle() }
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Checkbox
        Box(
            modifier = Modifier
                .size(24.dp)
                .clip(CircleShape)
                .background(
                    if (isSelected)
                        MaterialTheme.colorScheme.primary
                    else
                        MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                ),
            contentAlignment = Alignment.Center
        ) {
            if (isSelected) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.size(14.dp)
                )
            }
        }

        Spacer(Modifier.width(12.dp))

        // Name
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = user.name,
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
                text = "@${user.username}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // Share amount
        if (isSelected) {
            Spacer(Modifier.width(8.dp))
            if (splitType == "custom") {
                OutlinedTextField(
                    value = shareAmount ?: "",
                    onValueChange = onCustomAmountChanged,
                    modifier = Modifier.width(90.dp),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    prefix = { Text("₹") },
                    shape = RoundedCornerShape(8.dp),
                    textStyle = MaterialTheme.typography.bodyMedium
                )
            } else {
                Text(
                    text = "₹${shareAmount ?: "0.00"}",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

// ─── Share Preview Banner ─────────────────────────────────────────────────────

@Composable
private fun SharePreviewBanner(
    shareAmount: Double,
    participantCount: Int,
    totalAmount: Double
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.secondaryContainer
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Each person pays",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f)
                )
                Text(
                    text = "₹${"%.2f".format(shareAmount)}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )
            }
            Text(
                text = "÷ $participantCount people",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.6f)
            )
        }
    }
}

// ─── Custom Split Status ──────────────────────────────────────────────────────

@Composable
private fun CustomSplitStatus(
    totalAmount: Double,
    enteredAmount: Double,
    remaining: Double
) {
    val isBalanced = Math.abs(remaining) < 0.01
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = if (isBalanced)
            MaterialTheme.colorScheme.secondaryContainer
        else
            MaterialTheme.colorScheme.errorContainer
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = if (isBalanced) "✓ Amounts balanced" else "Remaining to assign",
                style = MaterialTheme.typography.bodySmall,
                color = if (isBalanced)
                    MaterialTheme.colorScheme.onSecondaryContainer
                else
                    MaterialTheme.colorScheme.onErrorContainer
            )
            Text(
                text = if (isBalanced) "₹${"%.2f".format(totalAmount)}"
                else "₹${"%.2f".format(Math.abs(remaining))}",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                color = if (isBalanced)
                    MaterialTheme.colorScheme.onSecondaryContainer
                else
                    MaterialTheme.colorScheme.onErrorContainer
            )
        }
    }
}