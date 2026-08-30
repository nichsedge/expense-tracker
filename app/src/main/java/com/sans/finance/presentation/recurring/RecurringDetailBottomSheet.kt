package com.sans.finance.presentation.recurring

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SheetState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sans.finance.R
import com.sans.finance.core.util.CurrencyFormatter
import com.sans.finance.domain.model.Expense

private const val DAYS_IN_MONTH = 30L
private const val WEEKS_IN_MONTH = 4L
private const val MONTHS_IN_YEAR = 12L
private const val COMPOUND_10Y_FACTOR = 173.0848

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecurringDetailBottomSheet(
    context: RecurringExpenseContext,
    onDismiss: () -> Unit,
    onEditExpense: (expenseId: Long) -> Unit,
    onDeleteExpense: (expense: Expense) -> Unit,
    onTogglePause: ((expense: Expense) -> Unit)? = null,
    sheetState: SheetState = rememberModalBottomSheetState()
) {
    var showDeleteDialog by remember { mutableStateOf(false) }
    val expense = context.expense
    val targetExpenseId = expense.parentRecurringId ?: expense.id

    val mult = expense.recurrenceIntervalMultiplier.coerceAtLeast(1)
    val monthlyAmount = when (expense.recurrenceInterval) {
        "DAILY" -> (expense.amount * DAYS_IN_MONTH) / mult
        "WEEKLY" -> (expense.amount * WEEKS_IN_MONTH) / mult
        "MONTHLY" -> expense.amount / mult
        "YEARLY" -> expense.amount / (MONTHS_IN_YEAR * mult)
        else -> expense.amount
    }
    val annualAmount = monthlyAmount * MONTHS_IN_YEAR
    val opportunityCost10Y = (monthlyAmount * COMPOUND_10Y_FACTOR).toLong()

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            RecurringDetailHeader(
                context = context,
                onDeleteClick = { showDeleteDialog = true }
            )

            RecurringCadenceCard(expense = expense)

            Text(
                text = "COST PROJECTION",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                letterSpacing = 1.sp
            )

            RecurringCostProjectionCard(
                monthlyAmount = monthlyAmount,
                annualAmount = annualAmount,
                opportunityCost10Y = opportunityCost10Y,
                currencyCode = context.currencyCode
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp, bottom = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (onTogglePause != null) {
                    val isPaused = expense.recurrenceStatus.equals("PAUSED", ignoreCase = true)
                    OutlinedButton(
                        onClick = {
                            onTogglePause(expense)
                            onDismiss()
                        },
                        modifier = Modifier.weight(1f),
                        shape = MaterialTheme.shapes.small
                    ) {
                        Text(if (isPaused) "Resume" else "Pause")
                    }
                }
                OutlinedButton(
                    onClick = {
                        onDismiss()
                        onEditExpense(targetExpenseId)
                    },
                    modifier = Modifier.weight(1f),
                    shape = MaterialTheme.shapes.small
                ) {
                    Icon(
                        Icons.Default.Edit,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Edit Definition")
                }
            }
        }
    }

    if (showDeleteDialog) {
        RecurringDeleteDialog(
            onDismiss = { showDeleteDialog = false },
            onConfirmDelete = {
                showDeleteDialog = false
                onDeleteExpense(expense)
                onDismiss()
            }
        )
    }
}

@Composable
private fun RecurringDetailHeader(
    context: RecurringExpenseContext,
    onDeleteClick: () -> Unit
) {
    val expense = context.expense
    val category = context.categoryName ?: "Uncategorized"
    val account = context.accountName ?: "Unknown Account"

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = expense.title.ifBlank { "Recurring Subscription" },
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "$category • $account",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        IconButton(onClick = onDeleteClick) {
            Icon(
                Icons.Default.Delete,
                contentDescription = "Delete Recurring",
                tint = MaterialTheme.colorScheme.error
            )
        }
    }
}

@Composable
private fun RecurringCadenceCard(expense: Expense) {
    val mult = expense.recurrenceIntervalMultiplier.coerceAtLeast(1)
    val unit = when (expense.recurrenceInterval) {
        "DAILY" -> if (mult > 1) "Days" else "Day"
        "WEEKLY" -> if (mult > 1) "Weeks" else "Week"
        "MONTHLY" -> if (mult > 1) "Months" else "Month"
        "YEARLY" -> if (mult > 1) "Years" else "Year"
        else -> "Month"
    }
    val cadenceText = if (mult == 1) {
        "Every ${expense.recurrenceInterval?.lowercase()?.replaceFirstChar { it.uppercase() } ?: "Month"}"
    } else {
        "Every $mult $unit"
    }

    val endText = when (expense.recurrenceEndType) {
        "UNTIL_DATE" -> {
            if (expense.recurrenceEndDate != null) {
                "Ends on " + com.sans.finance.core.util.DateFormatterUtils.formatStandardDate(expense.recurrenceEndDate)
            } else "Ends on set date"
        }
        "AFTER_COUNT" -> {
            val total = expense.recurrenceTotalOccurrences ?: 1
            "Ends after $total cycles"
        }
        else -> "Ongoing (Never ends)"
    }

    val isPaused = expense.recurrenceStatus.equals("PAUSED", ignoreCase = true)

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
        ),
        shape = MaterialTheme.shapes.medium
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.Repeat,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "Cadence",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                        )
                        Text(
                            text = cadenceText,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }

                Text(
                    text = CurrencyFormatter.formatAmount(expense.amount, expense.currency),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Condition: $endText",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                )
                if (isPaused) {
                    Text(
                        text = "PAUSED",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
}

@Composable
private fun RecurringCostProjectionCard(
    monthlyAmount: Long,
    annualAmount: Long,
    opportunityCost10Y: Long,
    currencyCode: String
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
        ),
        shape = MaterialTheme.shapes.medium
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Monthly Cost",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = CurrencyFormatter.formatAmount(monthlyAmount, currencyCode),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Annual Cost",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = CurrencyFormatter.formatAmount(annualAmount, currencyCode),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "10-Year Opportunity Cost",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "If invested at 7% compound ROI",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.outline
                    )
                }
                Text(
                    text = CurrencyFormatter.formatAmount(opportunityCost10Y, currencyCode),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.secondary
                )
            }
        }
    }
}

@Composable
private fun RecurringDeleteDialog(
    onDismiss: () -> Unit,
    onConfirmDelete: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Delete Recurring Expense") },
        text = {
            Text(
                "Are you sure you want to delete this recurring expense definition? " +
                    "Future transactions will no longer be tracked."
            )
        },
        confirmButton = {
            TextButton(
                onClick = onConfirmDelete,
                colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
            ) {
                Text(stringResource(R.string.delete))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        }
    )
}
