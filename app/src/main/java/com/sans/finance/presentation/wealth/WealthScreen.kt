package com.sans.finance.presentation.wealth

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.ShowChart
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.outlined.Circle
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sans.finance.presentation.components.AppTopBar
import com.sans.finance.presentation.components.GlassCard
import com.sans.finance.presentation.components.PrivacyText

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WealthScreen(
    contentPadding: PaddingValues = PaddingValues(12.dp),
    onOpenAccounts: () -> Unit,
    onOpenPortfolio: () -> Unit,
    onOpenDebts: () -> Unit,
    onOpenGoals: () -> Unit,
    onOpenBudgets: () -> Unit,
    onOpenForecasting: () -> Unit,
    onOpenMonthlyReview: () -> Unit,
    viewModel: WealthViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            AppTopBar(
                title = "Wealth"
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.surface,
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.05f)
                        )
                    )
                )
                .padding(paddingValues),
            contentPadding = contentPadding,
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item {
                WealthSummaryCard(
                    netWorth = state.cashAssets + state.portfolioValue - state.liabilities,
                    assets = state.cashAssets + state.portfolioValue,
                    liabilities = state.liabilities,
                    currencyCode = state.currencyCode,
                    isPrivacyModeEnabled = state.isPrivacyModeEnabled,
                    onTogglePrivacyMode = viewModel::togglePrivacyMode
                )
            }

            item {
                EmergencyRunwayCard(
                    runwayMonths = state.runwayMonths,
                    monthlyBurn = state.monthlyBurn,
                    cashAssets = state.cashAssets,
                    currencyCode = state.currencyCode,
                    isPrivacyModeEnabled = state.isPrivacyModeEnabled
                )
            }

            if (state.monthlyPassiveIncome > 0L) {
                item {
                    UpcomingInflowsCard(
                        monthlyPassiveIncome = state.monthlyPassiveIncome,
                        annualPassiveIncome = state.annualPassiveIncome,
                        nextPayoutDateStr = state.nextPayoutDateStr,
                        currencyCode = state.currencyCode,
                        isPrivacyModeEnabled = state.isPrivacyModeEnabled
                    )
                }
            }

            if (state.monthlyBurn > 0L) {
                item {
                    FinancialIndependenceMilestoneCard(
                        fiCoveragePct = state.fiCoveragePct,
                        fiStage = state.fiStage,
                        fiNextStageGap = state.fiNextStageGap,
                        monthlyPassiveIncome = state.monthlyPassiveIncome,
                        monthlyBurn = state.monthlyBurn,
                        runwayMonths = state.runwayMonths,
                        currencyCode = state.currencyCode,
                        isPrivacyModeEnabled = state.isPrivacyModeEnabled
                    )
                }
            }

            item {
                val context = androidx.compose.ui.platform.LocalContext.current
                CloudSyncCard(
                    lastSnapshotDate = state.lastSnapshotDate,
                    isSyncing = state.isSyncing,
                    onSyncClick = { viewModel.triggerCloudSync(context) }
                )
            }

            item { Spacer(modifier = Modifier.height(4.dp)) }

            item { SectionHeader("FINANCIAL HUB") }

            item {
                WealthNavCard(
                    title = "Accounts",
                    subtitle = "Manage cash, bank, & e-wallets",
                    leadingIcon = Icons.Default.AccountBalanceWallet,
                    onClick = onOpenAccounts
                )
            }
            item {
                WealthNavCard(
                    title = "Portfolio",
                    subtitle = "Investment holdings & performance",
                    leadingIcon = Icons.Default.PieChart,
                    onClick = onOpenPortfolio
                )
            }
            item {
                WealthNavCard(
                    title = "Debt Strategist",
                    subtitle = "Payoff planning & liabilities",
                    leadingIcon = Icons.Default.Payments,
                    onClick = onOpenDebts
                )
            }

            item { Spacer(modifier = Modifier.height(12.dp)) }
            item { SectionHeader("STRATEGY & PLANNING") }

            item {
                WealthNavCard(
                    title = "Financial Goals",
                    subtitle = "Track progress & future targets",
                    leadingIcon = Icons.Default.Flag,
                    onClick = onOpenGoals
                )
            }
            item {
                WealthNavCard(
                    title = "Budgeting",
                    subtitle = "Plan spending & monitor limits",
                    leadingIcon = Icons.AutoMirrored.Filled.ShowChart,
                    onClick = onOpenBudgets
                )
            }
            item {
                WealthNavCard(
                    title = "Net Worth Forecast",
                    subtitle = "Future wealth projection",
                    leadingIcon = Icons.AutoMirrored.Filled.ShowChart,
                    onClick = onOpenForecasting
                )
            }
            item {
                WealthNavCard(
                    title = "Monthly Review",
                    subtitle = "Monthly insights & closure",
                    leadingIcon = Icons.Default.Flag,
                    onClick = onOpenMonthlyReview
                )
            }

            item { Spacer(modifier = Modifier.height(32.dp)) }
        }
    }
}

@Composable
fun SectionHeader(text: String) {
    Text(
        text = text.uppercase(),
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.secondary,
        letterSpacing = 1.5.sp,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(horizontal = 4.dp, vertical = 4.dp)
    )
}

@Composable
private fun WealthSummaryCard(
    netWorth: Long,
    assets: Long,
    liabilities: Long,
    currencyCode: String,
    isPrivacyModeEnabled: Boolean,
    onTogglePrivacyMode: () -> Unit = {}
) {
    val haptic = androidx.compose.ui.platform.LocalHapticFeedback.current
    GlassCard(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.extraLarge)
            .clickable {
                haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                onTogglePrivacyMode()
            },
        containerColor = MaterialTheme.colorScheme.primary,
        alpha = 0.12f
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                "Net Worth",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )
            PrivacyText(
                amount = netWorth,
                currencyCode = currencyCode,
                isVisible = !isPrivacyModeEnabled,
                animate = true,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Black,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                WealthBreakdownItem(
                    "Total Assets",
                    assets,
                    MaterialTheme.colorScheme.tertiary,
                    currencyCode,
                    isPrivacyModeEnabled
                )
                VerticalDivider(
                    modifier = Modifier.height(32.dp),
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                )
                WealthBreakdownItem(
                    "Total Liabilities",
                    liabilities,
                    MaterialTheme.colorScheme.error,
                    currencyCode,
                    isPrivacyModeEnabled
                )
            }
        }
    }
}

@Composable
private fun WealthBreakdownItem(
    label: String,
    amount: Long,
    color: Color,
    currencyCode: String,
    isPrivacyModeEnabled: Boolean
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            label.uppercase(),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
            fontWeight = FontWeight.Bold
        )
        PrivacyText(
            amount = amount,
            currencyCode = currencyCode,
            isVisible = !isPrivacyModeEnabled,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Black,
            color = color
        )
    }
}

@Composable
private fun WealthNavCard(
    title: String,
    subtitle: String,
    leadingIcon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        onClick = onClick,
        shape = MaterialTheme.shapes.extraLarge,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
    ) {
        Row(
            modifier = Modifier
                .padding(14.dp)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = leadingIcon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(22.dp)
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.outline,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
private fun CloudSyncCard(
    lastSnapshotDate: Long?,
    isSyncing: Boolean,
    onSyncClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.extraLarge,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f)
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.2f))
    ) {
        Row(
            modifier = Modifier
                .padding(horizontal = 16.dp, vertical = 12.dp)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier.weight(1f),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Sync,
                        contentDescription = "Cloud Sync",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(22.dp)
                    )
                }
                Column {
                    Text(
                        text = "Cloud Sync & Backup",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = if (lastSnapshotDate != null) {
                            "Snapshot: ${com.sans.finance.core.util.DateFormatterUtils.getStandardFormatter().format(java.util.Date(lastSnapshotDate))}"
                        } else {
                            "Portfolio & DB in Sync"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            if (isSyncing) {
                androidx.compose.material3.CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    strokeWidth = 2.5.dp,
                    color = MaterialTheme.colorScheme.primary
                )
            } else {
                androidx.compose.material3.FilledTonalButton(
                    onClick = onSyncClick,
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                    shape = MaterialTheme.shapes.medium
                ) {
                    Text(
                        "Sync Now",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.ExtraBold
                    )
                }
            }
        }
    }
}

@Composable
fun EmergencyRunwayCard(
    runwayMonths: Double,
    monthlyBurn: Long,
    cashAssets: Long,
    currencyCode: String,
    isPrivacyModeEnabled: Boolean
) {
    val statusColor = when {
        runwayMonths >= 6.0 -> Color(0xFF4CAF50)
        runwayMonths >= 3.0 -> Color(0xFFFF9800)
        else -> Color(0xFFF44336)
    }
    val statusLabel = when {
        runwayMonths >= 6.0 -> "Healthy Buffer"
        runwayMonths >= 3.0 -> "Moderate Buffer"
        else -> "Needs Attention"
    }

    GlassCard(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(statusColor.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "🛡️",
                            fontSize = 16.sp
                        )
                    }
                    Column(modifier = Modifier.weight(1f, fill = false)) {
                        Text(
                            text = "Emergency Runway",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1
                        )
                        Text(
                            text = if (runwayMonths >= 99.0) "Over 99 months coverage" else "${String.format(java.util.Locale.US, "%.1f", runwayMonths)} months of living expenses",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1
                        )
                    }
                }

                Spacer(modifier = Modifier.size(8.dp))

                Box(
                    modifier = Modifier
                        .background(statusColor.copy(alpha = 0.15f), MaterialTheme.shapes.small)
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = statusLabel,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Black,
                        color = statusColor,
                        maxLines = 1,
                        softWrap = false
                    )
                }
            }

            androidx.compose.material3.LinearProgressIndicator(
                progress = { (runwayMonths.toFloat() / 6.0f).coerceIn(0f, 1f) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(CircleShape),
                color = statusColor,
                trackColor = MaterialTheme.colorScheme.surfaceVariant
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        "Liquid Cash Buffer",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    PrivacyText(
                        amount = cashAssets,
                        currencyCode = currencyCode,
                        isVisible = !isPrivacyModeEnabled,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        "Avg Monthly Burn (90d)",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    PrivacyText(
                        amount = monthlyBurn,
                        currencyCode = currencyCode,
                        isVisible = !isPrivacyModeEnabled,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
fun UpcomingInflowsCard(
    monthlyPassiveIncome: Long,
    annualPassiveIncome: Long,
    nextPayoutDateStr: String,
    currencyCode: String,
    isPrivacyModeEnabled: Boolean
) {
    GlassCard(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF4CAF50).copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "💰",
                            fontSize = 16.sp
                        )
                    }
                    Column(modifier = Modifier.weight(1f, fill = false)) {
                        Text(
                            text = "Passive Coupon Inflows",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1
                        )
                        Text(
                            text = "Next Payout: $nextPayoutDateStr",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1
                        )
                    }
                }

                Spacer(modifier = Modifier.size(8.dp))

                Box(
                    modifier = Modifier
                        .background(Color(0xFF4CAF50).copy(alpha = 0.15f), MaterialTheme.shapes.small)
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = "Every 10th",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Black,
                        color = Color(0xFF4CAF50),
                        maxLines = 1,
                        softWrap = false
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        "Monthly Est. Coupon",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    PrivacyText(
                        amount = monthlyPassiveIncome,
                        currencyCode = currencyCode,
                        isVisible = !isPrivacyModeEnabled,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Black,
                        color = Color(0xFF4CAF50)
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        "Annual Projected Yield",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    PrivacyText(
                        amount = annualPassiveIncome,
                        currencyCode = currencyCode,
                        isVisible = !isPrivacyModeEnabled,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    }
}

@Composable
fun FinancialIndependenceMilestoneCard(
    fiCoveragePct: Double,
    fiStage: String,
    fiNextStageGap: Long,
    monthlyPassiveIncome: Long,
    monthlyBurn: Long,
    runwayMonths: Double,
    currencyCode: String,
    isPrivacyModeEnabled: Boolean
) {
    GlassCard(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(text = "🎯", fontSize = 16.sp)
                    }
                    Column(modifier = Modifier.weight(1f, fill = false)) {
                        Text(
                            text = "Financial Independence",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1
                        )
                        Text(
                            text = fiStage,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1
                        )
                    }
                }

                Spacer(modifier = Modifier.size(8.dp))

                Box(
                    modifier = Modifier
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f), MaterialTheme.shapes.small)
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "${String.format(java.util.Locale.US, "%.1f", fiCoveragePct)}% Covered",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.primary,
                        maxLines = 1,
                        softWrap = false
                    )
                }
            }

            // Progress Indicator
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                androidx.compose.material3.LinearProgressIndicator(
                    progress = { (fiCoveragePct / 100.0).toFloat().coerceIn(0f, 1f) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(CircleShape),
                    color = if (fiCoveragePct >= 100.0) Color(0xFF4CAF50) else MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("0%", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("Lean FI (50%)", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("Full FI (100%)", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            // Milestone Tracker Items
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f), MaterialTheme.shapes.medium)
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                val isFortress = runwayMonths >= 12.0
                val isLeanFi = fiCoveragePct >= 50.0
                val isFullFi = fiCoveragePct >= 100.0

                MilestoneRowItem(
                    title = "1. Emergency Fortress (12+ Mo)",
                    status = if (isFortress) "${String.format(java.util.Locale.US, "%.0f", runwayMonths)} Mo Runway" else "In Progress",
                    isComplete = isFortress
                )
                MilestoneRowItem(
                    title = "2. Lean FI (50% Living Burn)",
                    status = if (isLeanFi) "Achieved" else "${String.format(java.util.Locale.US, "%.1f", fiCoveragePct)}% / 50%",
                    isComplete = isLeanFi
                )
                MilestoneRowItem(
                    title = "3. Full FI (100% Living Burn)",
                    status = if (isFullFi) "Achieved" else "${String.format(java.util.Locale.US, "%.1f", fiCoveragePct)}% / 100%",
                    isComplete = isFullFi
                )
            }

            if (fiNextStageGap > 0L) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.08f),
                            MaterialTheme.shapes.small
                        )
                        .padding(horizontal = 10.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(text = "💡", fontSize = 13.sp)
                    Text(
                        text = "Gap to next milestone:",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    PrivacyText(
                        amount = fiNextStageGap,
                        currencyCode = currencyCode,
                        isVisible = !isPrivacyModeEnabled,
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "/mo",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun MilestoneRowItem(
    title: String,
    status: String,
    isComplete: Boolean
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = if (isComplete) Icons.Default.CheckCircle else Icons.Outlined.Circle,
                contentDescription = null,
                tint = if (isComplete) Color(0xFF4CAF50) else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                modifier = Modifier.size(16.dp)
            )
            Text(
                text = title,
                style = MaterialTheme.typography.bodySmall,
                fontWeight = if (isComplete) FontWeight.SemiBold else FontWeight.Normal,
                color = if (isComplete) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1
            )
        }

        Spacer(modifier = Modifier.size(8.dp))

        Text(
            text = status,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = if (isComplete) Color(0xFF4CAF50) else MaterialTheme.colorScheme.primary,
            maxLines = 1,
            softWrap = false
        )
    }
}
