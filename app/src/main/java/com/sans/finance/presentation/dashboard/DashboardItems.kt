package com.sans.finance.presentation.dashboard

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.sans.finance.core.util.CurrencyFormatter
import com.sans.finance.presentation.components.AppTopBar
import com.sans.finance.presentation.components.CategoryIcon
import com.sans.finance.presentation.components.GlassCard
import com.sans.finance.presentation.components.PrivacyText
import java.util.Locale
import androidx.lifecycle.compose.collectAsStateWithLifecycle


@Composable
fun DashboardGoalItem(goal: DashboardGoal, currencyCode: String, isPrivacyModeEnabled: Boolean) {
    val progress = goal.progress
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
        shape = MaterialTheme.shapes.large
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    goal.name,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold
                )
                if (!isPrivacyModeEnabled) {
                    Text(
                        "${(progress * 100).toInt()}%",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Black
                    )
                } else {
                    Text(
                        "••%",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Black
                    )
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(CircleShape),
                strokeCap = androidx.compose.ui.graphics.StrokeCap.Round,
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.surfaceVariant
            )
        }
    }
}

@Composable
fun DashboardBillItem(
    bill: com.sans.finance.domain.model.Expense,
    currencyCode: String,
    isPrivacyModeEnabled: Boolean
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.1f)
        ),
        shape = MaterialTheme.shapes.large,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.1f))
    ) {
        Row(
            modifier = Modifier
                .padding(10.dp)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .background(
                        MaterialTheme.colorScheme.error.copy(alpha = 0.1f),
                        MaterialTheme.shapes.medium
                    ),
                contentAlignment = Alignment.Center
            ) {
                CategoryIcon(
                    icon = bill.categoryIcon ?: "📄",
                    fontSize = 14.sp
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = bill.title.ifBlank { bill.categoryName ?: "Upcoming Bill" },
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1
                )
                Text(
                    text = if (bill.isInstallmentPayment) "Installment ${bill.installmentMonth}/${bill.installmentTotalMonths}"
                    else "Recurring Payment",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            PrivacyText(
                amount = bill.amount,
                currencyCode = bill.currency,
                isVisible = !isPrivacyModeEnabled,
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Black,
                color = MaterialTheme.colorScheme.error
            )
        }
    }
}

@Composable
fun RecentTransactionItem(
    transaction: com.sans.finance.domain.model.Expense,
    currencyCode: String,
    isPrivacyModeEnabled: Boolean,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
        shape = MaterialTheme.shapes.large
    ) {
        Row(
            modifier = Modifier
                .padding(8.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            val isIncome = transaction.type == "INCOME"
            val statusColor =
                if (isIncome) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.onSurface

            Box(
                modifier = Modifier
                    .size(28.dp)
                    .background(
                        (if (isIncome) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.surfaceVariant).copy(
                            alpha = 0.1f
                        ),
                        MaterialTheme.shapes.medium
                    ),
                contentAlignment = Alignment.Center
            ) {
                CategoryIcon(
                    icon = transaction.categoryIcon ?: (if (isIncome) "💰" else "💸"),
                    fontSize = 14.sp
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = transaction.title.ifBlank { transaction.categoryName ?: "Transaction" },
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1
                )
                val subtitle = if (transaction.title.isNotBlank()) {
                    transaction.details ?: transaction.categoryName ?: ""
                } else {
                    transaction.details ?: ""
                }

                if (subtitle.isNotBlank()) {
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1
                    )
                }
            }

            Column(horizontalAlignment = Alignment.End) {
                PrivacyText(
                    amount = transaction.amount,
                    currencyCode = transaction.currency,
                    isVisible = !isPrivacyModeEnabled,
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Black,
                    color = statusColor
                )
                Text(
                    text = com.sans.finance.core.util.DateFormatterUtils.getStandardFormatter()
                        .format(java.util.Date(transaction.date)),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

