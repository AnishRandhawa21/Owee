package com.owee.app.ui.screens.home

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.owee.app.viewmodel.AuthViewModel
import com.owee.app.viewmodel.BalanceViewModel

@Composable
fun HomeScreen(
    authViewModel: AuthViewModel,
    balanceViewModel: BalanceViewModel
) {
    val user by authViewModel.user.collectAsState()
    val uiState by balanceViewModel.uiState.collectAsState()

    LaunchedEffect(user.dbId) {
        user.dbId?.let { balanceViewModel.initialize(it) }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Summary Cards
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            BalanceCard(
                title = "You are owed",
                amount = uiState.overallBalance?.totalOwed ?: 0.0,
                color = MaterialTheme.colorScheme.secondary,
                modifier = Modifier.weight(1f)
            )
            BalanceCard(
                title = "You owe",
                amount = uiState.overallBalance?.totalOwe ?: 0.0,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "Group Balances",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(16.dp))

        if (uiState.isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(bottom = 16.dp)
            ) {
                val groups = uiState.overallBalance?.groupBalances ?: emptyList()
                if (groups.isEmpty()) {
                    item {
                        Text("No groups found. Create one to start tracking!", color = Color.Gray)
                    }
                } else {
                    items(groups) { groupBalance ->
                        val myBalance = groupBalance.balances.find { it.userId == user.dbId }?.amount ?: 0.0
                        GroupBalanceItem(groupBalance.groupName, myBalance)
                    }
                }
            }
        }
    }
}

@Composable
fun BalanceCard(title: String, amount: Double, color: Color, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.1f))
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(text = title, style = MaterialTheme.typography.labelMedium, color = color)
            Text(
                text = "₹${"%.2f".format(kotlin.math.abs(amount))}",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = color
            )
        }
    }
}

@Composable
fun GroupBalanceItem(groupName: String, balance: Double) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (androidx.compose.foundation.isSystemInDarkTheme()) MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = if (androidx.compose.foundation.isSystemInDarkTheme()) 0.dp else 1.dp)
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = groupName, fontWeight = FontWeight.SemiBold)
            val color = when {
                balance > 0 -> MaterialTheme.colorScheme.secondary
                balance < 0 -> MaterialTheme.colorScheme.error
                else -> Color.Gray
            }
            val text = when {
                balance > 0 -> "Owes you ₹${"%.2f".format(balance)}"
                balance < 0 -> "You owe ₹${"%.2f".format(kotlin.math.abs(balance))}"
                else -> "Settled"
            }
            Text(text = text, color = color, fontWeight = FontWeight.Bold)
        }
    }
}
