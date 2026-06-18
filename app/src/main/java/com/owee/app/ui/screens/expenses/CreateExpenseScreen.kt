package com.owee.app.ui.screens.expenses

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.owee.app.data.remote.model.User
import com.owee.app.viewmodel.ExpensesViewModel

@Composable
fun CreateExpenseScreen(
    currentUserId: String,
    groupId: String,
    members: List<User>,
    expensesViewModel: ExpensesViewModel,
    balanceViewModel: com.owee.app.viewmodel.BalanceViewModel,
    onExpenseCreated: () -> Unit
) {
    val state by expensesViewModel.uiState.collectAsState()

    LaunchedEffect(state.message) {
        if (state.message == "Expense added!") {
            // Trigger optimistic update in BalanceViewModel
            val amount = state.amountInput.toDoubleOrNull() ?: 0.0
            val count = state.selectedParticipantIds.size
            val selectedGroupId = groupId

            if (amount > 0) {
                val shares = state.selectedParticipantIds.associateWith { id ->
                    if (state.splitType == "custom") state.customAmounts[id]?.toDoubleOrNull() ?: 0.0
                    else amount / count
                }
                
                balanceViewModel.optimisticUpdate(
                    groupId = selectedGroupId,
                    paidBy = currentUserId,
                    amount = amount,
                    participantShares = shares
                )
            }

            expensesViewModel.clearMessage()
            onExpenseCreated()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .imePadding() // Ensures UI reacts to keyboard
    ) {
        LazyColumn(
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(20.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // Amount Display (Now at the top)
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 10.dp),
                    horizontalAlignment = Alignment.Start
                ) {
                    TextField(
                        value = state.amountInput,
                        onValueChange = { if (it.all { c -> c.isDigit() || c == '.' }) expensesViewModel.onAmountChanged(it) },
                        textStyle = MaterialTheme.typography.displayMedium.copy(
                            fontWeight = FontWeight.ExtraBold,
                            textAlign = TextAlign.Start
                        ),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent,
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent,
                            focusedTextColor = MaterialTheme.colorScheme.primary,
                            unfocusedTextColor = MaterialTheme.colorScheme.primary
                        ),
                        placeholder = { 
                            Text(
                                "0", 
                                style = MaterialTheme.typography.displayMedium,
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f),
                                textAlign = TextAlign.Start
                            ) 
                        },
                        prefix = { Text("₹", style = MaterialTheme.typography.displayMedium.copy(fontWeight = FontWeight.ExtraBold)) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    Text(
                        text = "Total Amount",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                        modifier = Modifier.padding(start = 12.dp) // Align with prefix
                    )
                }
            }

            // Title/Note Section
            item {
                TextField(
                    value = state.titleInput,
                    onValueChange = expensesViewModel::onTitleChanged,
                    placeholder = { Text("What was it for? (e.g. Dinner)", color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent
                    ),
                    shape = RoundedCornerShape(16.dp),
                    singleLine = true
                )
            }

            // Split Type Selection
            item {
                Column {
                    Text("Split options", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        listOf("equal", "custom").forEach { type ->
                            val selected = state.splitType == type
                            Surface(
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable { expensesViewModel.onSplitTypeChanged(type) },
                                shape = RoundedCornerShape(14.dp),
                                color = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                border = if (selected) androidx.compose.foundation.BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else null
                            ) {
                                Box(modifier = Modifier.padding(16.dp), contentAlignment = Alignment.Center) {
                                    Text(
                                        text = if (type == "equal") "Split Equally" else "Exact Amounts",
                                        style = MaterialTheme.typography.labelLarge,
                                        fontWeight = FontWeight.Bold,
                                        color = if (selected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Participants row
            item {
                Column {
                    Text("Between who?", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(16.dp))
                    LazyRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        contentPadding = PaddingValues(horizontal = 4.dp)
                    ) {
                        items(members, key = { it.id ?: "" }) { member ->
                            val isSelected = member.id in state.selectedParticipantIds
                            val isCurrentUser = member.id == currentUserId

                            ParticipantAvatarItem(
                                user = member,
                                isSelected = isSelected,
                                isCurrentUser = isCurrentUser,
                                onToggle = { member.id?.let { expensesViewModel.onParticipantToggled(it) } }
                            )
                        }
                    }
                }
            }

            // Exact Amounts inputs (show only if splitType is custom)
            if (state.splitType == "custom") {
                item {
                    Text(
                        "Enter exact amounts",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
                
                items(members.filter { it.id in state.selectedParticipantIds }, key = { "input-${it.id}" }) { member ->
                    CustomAmountInputItem(
                        user = member,
                        amount = state.customAmounts[member.id] ?: "",
                        onAmountChanged = { amount -> member.id?.let { expensesViewModel.onCustomAmountChanged(it, amount) } }
                    )
                }
            }

            // Dynamic Info Banner
            item {
                val amount = state.amountInput.toDoubleOrNull() ?: 0.0
                val count = state.selectedParticipantIds.size
                
                if (state.splitType == "equal" && amount > 0 && count > 0) {
                    InfoBanner(
                        text = "Each person pays ₹${"%.2f".format(amount / count)}",
                        color = MaterialTheme.colorScheme.secondary
                    )
                } else if (state.splitType == "custom" && amount > 0) {
                    val entered = state.selectedParticipantIds.sumOf { state.customAmounts[it]?.toDoubleOrNull() ?: 0.0 }
                    val remaining = amount - entered
                    val isBalanced = kotlin.math.abs(remaining) < 0.01
                    
                    InfoBanner(
                        text = if (isBalanced) "✓ Amounts balanced" else "Remaining to assign: ₹${"%.2f".format(kotlin.math.abs(remaining))}",
                        color = if (isBalanced) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.error
                    )
                }
            }

            if (state.error != null) {
                item {
                    Text(state.error!!, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                }
            }
        }

        // Save Button
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surface)
                .padding(16.dp)
        ) {
            Button(
                onClick = { expensesViewModel.validateAndCreate() },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                enabled = !state.isCreating,
                shape = RoundedCornerShape(16.dp)
            ) {
                if (state.isCreating) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp), color = MaterialTheme.colorScheme.onPrimary)
                } else {
                    Text("Add Expense", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun ParticipantAvatarItem(
    user: User,
    isSelected: Boolean,
    isCurrentUser: Boolean,
    onToggle: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .width(60.dp)
            .clickable(enabled = !isCurrentUser) { onToggle() }
    ) {
        // Fixed size container for the avatar and ring to prevent size shifts
        Box(
            modifier = Modifier.size(56.dp),
            contentAlignment = Alignment.Center
        ) {
            // Selection Ring (Always exists but color/border changes)
            Surface(
                modifier = Modifier.size(54.dp),
                shape = CircleShape,
                color = Color.Transparent,
                border = if (isSelected) androidx.compose.foundation.BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else null
            ) {
                Box(contentAlignment = Alignment.Center) {
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
                }
            }

            if (isSelected) {
                Box(
                    modifier = Modifier
                        .size(16.dp)
                        .align(Alignment.BottomEnd)
                        .background(MaterialTheme.colorScheme.primary, CircleShape)
                        .border(1.5.dp, MaterialTheme.colorScheme.surface, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.Check, 
                        null, 
                        tint = MaterialTheme.colorScheme.onPrimary, 
                        modifier = Modifier.size(10.dp)
                    )
                }
            }
        }
        Spacer(Modifier.height(8.dp))
        Text(
            text = if (isCurrentUser) "You" else user.name.split(" ").first(),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1
        )
    }
}

@Composable
private fun CustomAmountInputItem(
    user: User,
    amount: String,
    onAmountChanged: (String) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AsyncImage(
            model = user.photo_url,
            contentDescription = null,
            modifier = Modifier
                .size(32.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceVariant),
            contentScale = ContentScale.Crop,
            fallback = androidx.compose.ui.graphics.vector.rememberVectorPainter(Icons.Default.Person)
        )
        Spacer(Modifier.width(12.dp))
        Text(
            text = user.name,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.weight(1f)
        )
        OutlinedTextField(
            value = amount,
            onValueChange = { if (it.all { c -> c.isDigit() || c == '.' }) onAmountChanged(it) },
            modifier = Modifier.width(110.dp),
            prefix = { Text("₹") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
            ),
            singleLine = true,
            shape = RoundedCornerShape(12.dp),
            textStyle = LocalTextStyle.current.copy(textAlign = TextAlign.End, fontWeight = FontWeight.Bold)
        )
    }
}

@Composable
private fun InfoBanner(text: String, color: Color) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = color.copy(alpha = 0.1f)
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(16.dp),
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold,
            color = color
        )
    }
}
