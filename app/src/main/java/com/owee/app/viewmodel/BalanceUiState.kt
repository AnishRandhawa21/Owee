package com.owee.app.viewmodel

import com.owee.app.domain.model.OverallBalance
import com.owee.app.domain.model.Settlement
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ReceiptLong

@Serializable
data class BalanceUiState(
    val overallBalance: OverallBalance? = null,
    val settlements: List<Settlement> = emptyList(),
    val friendBalances: Map<String, Double> = emptyMap(),
    val recentActivity: List<ActivityItemData> = emptyList(),
    val monthlySpent: Double = 0.0,
    val mostActiveGroupName: String = "None",
    @Transient val isLoading: Boolean = false,
    @Transient val error: String? = null
)

@Serializable
data class ActivityItemData(
    val title: String,
    val subtitle: String,
    val amount: Double,
    val status: String,
    @Transient val icon: androidx.compose.ui.graphics.vector.ImageVector = Icons.Default.ReceiptLong
)
