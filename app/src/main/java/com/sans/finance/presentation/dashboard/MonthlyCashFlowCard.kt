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
fun MonthlyCashFlowCard(
    income: Long,
    expense: Long,
    cashFlow: Long,
    savingsRate: Float,
    currencyCode: String,
    isPrivacyModeEnabled: Boolean
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.extraLarge,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                "Monthly Summary",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Income box
                FlowBox(
                    label = "Income",
                    amount = income,
                    currencyCode = currencyCode,
                    color = MaterialTheme.colorScheme.tertiary,
                    icon = Icons.Default.ArrowUpward,
                    isPrivacyModeEnabled = isPrivacyModeEnabled,
                    modifier = Modifier.weight(1f)
                )
                // Expense box
                FlowBox(
                    label = "Expense",
                    amount = expense,
                    currencyCode = currencyCode,
                    color = MaterialTheme.colorScheme.error,
                    icon = Icons.Default.ArrowDownward,
                    isPrivacyModeEnabled = isPrivacyModeEnabled,
                    modifier = Modifier.weight(1f)
                )
            }

            // Cash Flow and Savings Rate Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                        MaterialTheme.shapes.large
                    )
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        "Cash Flow",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.Bold
                    )
                    PrivacyText(
                        amount = cashFlow,
                        currencyCode = currencyCode,
                        isVisible = !isPrivacyModeEnabled,
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Black,
                        color = if (cashFlow >= 0) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.error
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        "Savings Rate",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.Bold
                    )
                }

                com.sans.finance.presentation.components.CircularGauge(
                    progress = savingsRate,
                    size = 80.dp,
                    strokeWidth = 10.dp,
                    color = if (savingsRate >= 0.2f) MaterialTheme.colorScheme.tertiary
                    else if (savingsRate >= 0f) Color(0xFFFFC107)
                    else MaterialTheme.colorScheme.error,
                    isPrivacyModeEnabled = isPrivacyModeEnabled
                )
            }
        }
    }
}

@Composable
fun FlowBox(
    label: String,
    amount: Long,
    currencyCode: String,
    color: Color,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    isPrivacyModeEnabled: Boolean,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .background(color.copy(alpha = 0.08f), MaterialTheme.shapes.large)
            .border(1.dp, color.copy(alpha = 0.1f), MaterialTheme.shapes.large)
            .padding(12.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                icon,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(14.dp)
            )
            Spacer(Modifier.width(4.dp))
            Text(
                label,
                style = MaterialTheme.typography.labelSmall,
                color = color,
                fontWeight = FontWeight.Bold
            )
        }
        PrivacyText(
            amount = amount,
            currencyCode = currencyCode,
            isVisible = !isPrivacyModeEnabled,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Black,
            color = color
        )
    }
}

