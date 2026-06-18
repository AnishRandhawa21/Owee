package com.owee.app.ui.screens.home

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.owee.app.data.remote.model.User
import com.owee.app.viewmodel.*
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    authViewModel: AuthViewModel,
    balanceViewModel: BalanceViewModel,
    friendsViewModel: FriendsViewModel
) {
    val uiState by balanceViewModel.uiState.collectAsState()
    val friendsState by friendsViewModel.uiState.collectAsState()

    if (uiState.error != null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Error loading balances", color = MaterialTheme.colorScheme.error)
                Button(onClick = { balanceViewModel.refresh() }) {
                    Text("Retry")
                }
            }
        }
    }

    val pullToRefreshState = rememberPullToRefreshState()

    PullToRefreshBox(
        isRefreshing = uiState.isLoading,
        onRefresh = { 
            balanceViewModel.refresh()
            friendsViewModel.refresh()
        },
        state = pullToRefreshState,
        indicator = {}, // Hide default indicator; we'll animate the card itself
        modifier = Modifier.fillMaxSize()
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background),
            contentPadding = PaddingValues(bottom = 24.dp)
        ) {
            // ─── Header Summary Card ──────────────────────────────────────
            item {
                HomeHeaderCard(uiState, pullToRefreshState.distanceFraction)
            }

            // ─── Quick Settle (Friends) ───────────────────────────────────
            item {
                SectionHeader(title = "Quick Settle", onActionClick = {})
                if (friendsState.friends.isEmpty()) {
                    Text(
                        "Add friends to see them here",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                } else {
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(20.dp),
                        modifier = Modifier.padding(vertical = 8.dp)
                    ) {
                        items(friendsState.friends) { friend ->
                            val balance = uiState.friendBalances[friend.id] ?: 0.0
                            QuickSettleItem(friend, balance)
                        }
                    }
                }
            }

            // ─── Recent Activity ────────────────────────────────────────
            item {
                SectionHeader(title = "Recent Activity", showViewAll = true)
            }

            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                ) {
                    Column(modifier = Modifier.padding(vertical = 8.dp)) {
                        if (uiState.recentActivity.isEmpty()) {
                            Text(
                                "No recent activity",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                modifier = Modifier.padding(16.dp)
                            )
                        } else {
                            uiState.recentActivity.take(4).forEach { activity ->
                                RecentActivityItem(activity)
                            }
                        }
                    }
                }
            }

            // ─── Bottom Stats Cards ───────────────────────────────────────
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    StatCard(
                        title = "THIS MONTH",
                        subtitle = "spent",
                        value = "₹%.2f".format(uiState.monthlySpent),
                        icon = Icons.Default.TrendingDown,
                        modifier = Modifier.weight(1f)
                    )
                    StatCard(
                        title = "MOST OWED BY",
                        subtitle = uiState.friendBalances.maxByOrNull { it.value }?.let { friendsState.friends.find { f -> f.id == it.key }?.name } ?: "None",
                        value = "Friend",
                        icon = Icons.Default.Person,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@Composable
fun HomeHeaderCard(uiState: BalanceUiState, pullProgress: Float = 0f) {
    val overall = uiState.overallBalance
    val totalBalance = (overall?.totalOwed ?: 0.0) - (overall?.totalOwe ?: 0.0)
    val isDark = isSystemInDarkTheme()
    
     Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isDark) MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isDark) 0.dp else 2.dp)
    ) {
        Box {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "TOTAL BALANCE",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.5.sp
                )
                
                Spacer(Modifier.height(8.dp))
                
                Text(
                    text = String.format(Locale.US, "₹%.2f", totalBalance),
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.ExtraBold,
                    color = when {
                        totalBalance > 0.01 -> MaterialTheme.colorScheme.secondary
                        totalBalance < -0.01 -> MaterialTheme.colorScheme.error
                        else -> MaterialTheme.colorScheme.onSurface
                    }
                )
                
                Spacer(Modifier.height(16.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    BalanceHeaderSubCard(
                        title = "YOU ARE OWED",
                        amount = overall?.totalOwed ?: 0.0,
                        isPositive = true,
                        modifier = Modifier.weight(1f)
                    )
                    BalanceHeaderSubCard(
                        title = "YOU OWE",
                        amount = overall?.totalOwe ?: 0.0,
                        isPositive = false,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
            
            if (uiState.isLoading) {
                LinearProgressIndicator(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.TopCenter)
                        .height(2.dp),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = Color.Transparent
                )
            } else if (pullProgress > 0.01f) {
                LinearProgressIndicator(
                    progress = { pullProgress.coerceIn(0f, 1f) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.TopCenter)
                        .height(2.dp),
                    color = MaterialTheme.colorScheme.primary.copy(alpha = pullProgress.coerceIn(0.2f, 1f)),
                    trackColor = Color.Transparent
                )
            }
        }
    }
}

@Composable
fun BalanceHeaderSubCard(title: String, amount: Double, isPositive: Boolean, modifier: Modifier) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = if (isPositive) Icons.Default.ArrowDownward else Icons.Default.ArrowUpward,
                    contentDescription = null,
                    modifier = Modifier.size(12.dp),
                    tint = if (isPositive) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.error
                )
                Spacer(Modifier.width(4.dp))
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                    fontWeight = FontWeight.Bold,
                    fontSize = 8.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Spacer(Modifier.height(4.dp))
            Text(
                text = String.format(Locale.US, "₹%.2f", amount),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.ExtraBold,
                color = if (isPositive) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.error,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun SectionHeader(title: String, showViewAll: Boolean = true, onActionClick: () -> Unit = {}) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 16.dp, top = 20.dp, bottom = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        if (showViewAll) {
            Text(
                text = "VIEW ALL",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.clickable { onActionClick() }
            )
        }
    }
}

@Composable
fun QuickSettleItem(friend: User, balance: Double) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.width(64.dp)) {
        Box(contentAlignment = Alignment.BottomEnd) {
            AsyncImage(
                model = friend.photo_url,
                contentDescription = null,
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f), CircleShape),
                contentScale = ContentScale.Crop
            )
            if (balance != 0.0) {
                Surface(
                    modifier = Modifier.size(16.dp),
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primary,
                    border = BorderStroke(2.dp, MaterialTheme.colorScheme.background)
                ) {
                    Icon(Icons.Default.FlashOn, null, modifier = Modifier.padding(2.dp), tint = MaterialTheme.colorScheme.onPrimary)
                }
            }
        }
        Spacer(Modifier.height(8.dp))
        Text(
            text = friend.name.split(" ").first(),
            style = MaterialTheme.typography.labelSmall,
            maxLines = 1,
            fontWeight = FontWeight.Medium
        )
        Text(
            text = if (balance > 0) "+₹${balance.toInt()}" else if (balance < 0) "-₹${(-balance).toInt()}" else "₹0",
            style = MaterialTheme.typography.labelSmall,
            color = if (balance > 0) MaterialTheme.colorScheme.secondary else if (balance < 0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
fun RecentActivityItem(activity: ActivityItemData) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            modifier = Modifier.size(40.dp),
            shape = CircleShape,
            color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.4f)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(activity.icon, null, modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.onSecondaryContainer)
            }
        }
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(text = activity.title, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
            Text(text = activity.subtitle, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Column(horizontalAlignment = Alignment.End) {
            Text(
                text = if (activity.amount > 0) "+₹${"%.2f".format(activity.amount)}" else "-₹${"%.2f".format(-activity.amount)}",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                color = if (activity.amount > 0) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.error
            )
            Text(
                text = activity.status,
                style = MaterialTheme.typography.labelSmall,
                color = if (activity.status == "OWE") MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.secondary,
                fontWeight = FontWeight.Black
            )
        }
    }
}

@Composable
fun StatCard(title: String, subtitle: String, value: String, icon: ImageVector, modifier: Modifier) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Icon(icon, null, modifier = Modifier.size(20.dp), tint = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.height(12.dp))
            Text(text = title, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(text = value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text(text = subtitle, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f))
        }
    }
}
