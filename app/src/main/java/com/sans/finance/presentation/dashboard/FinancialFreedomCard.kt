package com.sans.finance.presentation.dashboard

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sans.finance.core.util.CurrencyFormatter
import com.sans.finance.presentation.components.PrivacyText
import java.util.Locale


@Composable
fun FinancialFreedomCard(
    yearsOfCover: Double,
    freedomScore: Float,
    totalAssets: Long,
    annualExpense: Long,
    currencyCode: String,
    isPrivacyModeEnabled: Boolean,
    isManualEnabled: Boolean,
    manualAnnualExpense: Long,
    onManualToggle: (Boolean) -> Unit,
    onManualAmountChange: (Long) -> Unit
) {
    var showHelp by remember { mutableStateOf(false) }
    var manualInput by remember(manualAnnualExpense) {
        mutableStateOf((manualAnnualExpense / 100).toString())
    }

    if (showHelp) {
        AlertDialog(
            onDismissRequest = { showHelp = false },
            title = {
                Text(
                    "Financial Freedom 101",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.tertiary
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Text(
                        "This score estimates how long your wealth lasts without a salary.",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium
                    )

                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            "Years of Cover",
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary
                        )
                        val assetsFormatted =
                            if (isPrivacyModeEnabled) "••••" else CurrencyFormatter.formatAmount(
                                totalAssets,
                                currencyCode
                            )
                        val expenseFormatted =
                            if (isPrivacyModeEnabled) "••••" else CurrencyFormatter.formatAmount(
                                annualExpense,
                                currencyCode
                            )
                        Text(
                            "$assetsFormatted ÷ $expenseFormatted (Annual Expense). Your wealth expressed in time.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            "Annual Expenses",
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary
                        )
                        val dailyFormatted =
                            if (isPrivacyModeEnabled) "••••" else CurrencyFormatter.formatAmount(
                                annualExpense / 365,
                                currencyCode
                            )
                        Text(
                            "Estimated at $dailyFormatted/day. We use your rolling 12-month spending to normalize this figure.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            "FIRE Goal (25x)",
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary
                        )
                        val fireTarget = annualExpense * 25
                        val targetFormatted =
                            if (isPrivacyModeEnabled) "••••" else CurrencyFormatter.formatAmount(
                                fireTarget,
                                currencyCode
                            )
                        Text(
                            "You are free when assets reach $targetFormatted. This allows for a safe 4% withdrawal rate.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "Manual Expense Override",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold
                            )
                            Switch(
                                checked = isManualEnabled,
                                onCheckedChange = { onManualToggle(it) }
                            )
                        }

                        if (isManualEnabled) {
                            OutlinedTextField(
                                value = manualInput,
                                onValueChange = {
                                    manualInput = it
                                    it.toLongOrNull()?.let { amount ->
                                        onManualAmountChange(amount * 100)
                                    }
                                },
                                label = { Text("Annual Expense ($currencyCode)") },
                                modifier = Modifier.fillMaxWidth(),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                singleLine = true,
                                shape = MaterialTheme.shapes.large
                            )
                            Text(
                                "Override auto-tracking to account for inflation or missing data.",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = { showHelp = false },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.tertiary)
                ) {
                    Text("Got it", fontWeight = FontWeight.Bold)
                }
            },
            shape = MaterialTheme.shapes.extraLarge,
            containerColor = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp
        )
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.extraLarge,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    brush = Brush.linearGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.2f),
                            MaterialTheme.colorScheme.surface
                        )
                    )
                )
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        "FINANCIAL FREEDOM",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.tertiary,
                        letterSpacing = 1.2.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        "Your wealth in time",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                IconButton(
                    onClick = { showHelp = true },
                    modifier = Modifier
                        .size(36.dp)
                        .background(
                            MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.5f),
                            CircleShape
                        )
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = "Help",
                        tint = MaterialTheme.colorScheme.tertiary,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                com.sans.finance.presentation.components.CircularGauge(
                    progress = freedomScore,
                    size = 80.dp,
                    strokeWidth = 10.dp,
                    color = MaterialTheme.colorScheme.tertiary,
                    trackColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.3f)
                )

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        "Years of Cover",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        if (isPrivacyModeEnabled) "••.• years" else String.format(
                            Locale.US,
                            "%.1f years",
                            yearsOfCover
                        ),
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    val statusText = when {
                        yearsOfCover >= 25.0 -> "You are Financially Free!"
                        yearsOfCover >= 10.0 -> "Decade of freedom secured."
                        yearsOfCover >= 1.0 -> "Over a year of cushion."
                        else -> "Building your foundation."
                    }

                    Text(
                        statusText,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.tertiary,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Progress towards FIRE Target (25x)
            val fireProgress = (yearsOfCover / 25.0).coerceIn(0.0, 1.0).toFloat()
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        "FIRE Progress (25x Expenses)",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        "${(fireProgress * 100).toInt()}%",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                LinearProgressIndicator(
                    progress = { fireProgress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(10.dp)
                        .clip(CircleShape),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant,
                    strokeCap = androidx.compose.ui.graphics.StrokeCap.Round
                )
            }

            var isSandboxExpanded by remember { mutableStateOf(false) }
            var selectedSwr by remember { mutableFloatStateOf(0.04f) }
            var expenseMultiplier by remember { mutableFloatStateOf(1.0f) }

            val effectiveExpense = if (isManualEnabled && manualAnnualExpense > 0) manualAnnualExpense else annualExpense
            val simulatedAnnualExpense = (effectiveExpense * expenseMultiplier).toLong()
            val simulatedTargetFire = if (selectedSwr > 0f) (simulatedAnnualExpense / selectedSwr).toLong() else 0L
            val simulatedYearsOfCover = if (simulatedAnnualExpense > 0) totalAssets.toDouble() / simulatedAnnualExpense else 0.0

            Spacer(modifier = Modifier.height(12.dp))

            Surface(
                onClick = { isSandboxExpanded = !isSandboxExpanded },
                shape = MaterialTheme.shapes.small,
                color = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.25f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            Icons.Default.Tune,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.tertiary,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            "FIRE Runway Sandbox",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.tertiary
                        )
                    }
                    Text(
                        if (isSandboxExpanded) "Hide ▲" else "Simulate ▼",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.tertiary,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            AnimatedVisibility(visible = isSandboxExpanded) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Safe Withdrawal Rate Tabs
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "SWR:",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        listOf(0.03f to "3.0%", 0.035f to "3.5%", 0.04f to "4.0% (Standard)").forEach { (swr, label) ->
                            val isSelected = selectedSwr == swr
                            FilterChip(
                                selected = isSelected,
                                onClick = { selectedSwr = swr },
                                label = { Text(label, fontSize = 11.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal) }
                            )
                        }
                    }

                    // Spend Multiplier Slider
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                "Spending: ${(expenseMultiplier * 100).toInt()}% (${String.format(Locale.US, "%.1f", simulatedYearsOfCover)} yrs)",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                if (isPrivacyModeEnabled) "••••••" else CurrencyFormatter.formatAmount(simulatedAnnualExpense / 12, currencyCode) + "/mo",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        Slider(
                            value = expenseMultiplier,
                            onValueChange = { expenseMultiplier = it },
                            valueRange = 0.5f..1.5f,
                            steps = 9
                        )
                    }

                    // Target Nest Egg & Milestone Badge
                    val milestoneBadge = when {
                        simulatedYearsOfCover >= (1.0 / selectedSwr) -> "👑 Full Financial Independence"
                        simulatedYearsOfCover >= 12.5 -> "🏔️ Lean FIRE Range"
                        simulatedYearsOfCover >= 6.0 -> "🧭 Half FI Milestone"
                        simulatedYearsOfCover >= 1.0 -> "⛵ Coast Cushion"
                        else -> "🛡️ Emergency Shield"
                    }

                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        shape = MaterialTheme.shapes.medium,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    "Simulated Nest Egg Goal",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                PrivacyText(
                                    amount = simulatedTargetFire,
                                    currencyCode = currencyCode,
                                    isVisible = !isPrivacyModeEnabled,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Black,
                                    color = MaterialTheme.colorScheme.tertiary
                                )
                            }
                            Surface(
                                color = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.15f),
                                shape = MaterialTheme.shapes.small
                            ) {
                                Text(
                                    milestoneBadge,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.tertiary
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

