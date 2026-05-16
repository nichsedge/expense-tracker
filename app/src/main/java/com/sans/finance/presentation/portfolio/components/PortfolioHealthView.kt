package com.sans.finance.presentation.portfolio.components

import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sans.finance.domain.model.AssetClassHealth
import com.sans.finance.domain.model.HealthStatus
import com.sans.finance.domain.model.RiskLevel

@Composable
fun PortfolioHealthView(
    healthList: List<AssetClassHealth>,
    rebalanceSuggestions: List<com.sans.finance.domain.model.RebalanceAction>,
    isPrivacyModeEnabled: Boolean,
    currentCurrency: String,
    modifier: Modifier = Modifier,
    onTargetClick: (AssetClassHealth) -> Unit = {}
) {
    if (healthList.isEmpty()) {
        Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("No health data available", style = MaterialTheme.typography.bodyLarge)
        }
        return
    }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        DiversificationSummary(healthList)

        if (rebalanceSuggestions.isNotEmpty()) {
            RebalanceSuggestionsSection(
                suggestions = rebalanceSuggestions,
                isPrivacyModeEnabled = isPrivacyModeEnabled,
                currentCurrency = currentCurrency
            )
        }

        healthList.forEach { health ->
            AssetHealthCard(health, isPrivacyModeEnabled, onClick = { onTargetClick(health) })
        }
    }
}

@Composable
fun DiversificationSummary(healthList: List<AssetClassHealth>) {
    val healthyCount = healthList.count { it.status == HealthStatus.HEALTHY }
    val totalCount = healthList.size
    val healthScore = if (totalCount > 0) (healthyCount.toFloat() / totalCount.toFloat()) else 0f

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.extraLarge,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
        )
    ) {
        Row(
            modifier = Modifier.padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(CircleShape)
                    .background(
                        when {
                            healthScore >= 0.8f -> Color(0xFF4CAF50)
                            healthScore >= 0.5f -> Color(0xFFFFC107)
                            else -> Color(0xFFF44336)
                        }
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "${(healthScore * 100).toInt()}",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
            Spacer(Modifier.width(20.dp))
            Column {
                Text(
                    "Diversification Score",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = when {
                        healthScore >= 0.8f -> "Your portfolio is well-balanced."
                        healthScore >= 0.5f -> "Some assets need rebalancing."
                        else -> "Your portfolio is highly imbalanced."
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun AssetHealthCard(health: AssetClassHealth, isPrivacyModeEnabled: Boolean, onClick: () -> Unit) {
    val statusColor = when (health.status) {
        HealthStatus.HEALTHY -> Color(0xFF4CAF50)
        HealthStatus.OVERWEIGHT -> Color(0xFFF44336)
        HealthStatus.UNDERWEIGHT -> Color(0xFFFFC107)
    }

    val statusIcon = when (health.status) {
        HealthStatus.HEALTHY -> Icons.Default.CheckCircle
        HealthStatus.OVERWEIGHT -> Icons.Default.Warning
        HealthStatus.UNDERWEIGHT -> Icons.Default.ArrowDownward
    }

    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = statusIcon,
                        contentDescription = null,
                        tint = statusColor,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = health.assetClass,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }

                RiskBadge(health.riskLevel)
            }

            Spacer(Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        "Current",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        "${String.format("%.1f", health.currentPercentage)}%",
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Bold
                    )
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        "Target",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        "${String.format("%.1f", health.targetPercentage)}%",
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Bold
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        "Status",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = health.status.name,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Bold,
                        color = statusColor
                    )
                }
            }

            Spacer(Modifier.height(12.dp))

            LinearProgressIndicator(
                progress = { (health.currentPercentage / 100f).toFloat() },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(CircleShape),
                color = statusColor,
                trackColor = MaterialTheme.colorScheme.surfaceVariant
            )

            if (health.status != HealthStatus.HEALTHY) {
                Spacer(Modifier.height(12.dp))
                val action = if (health.status == HealthStatus.OVERWEIGHT) "Reduce" else "Increase"
                val diff = Math.abs(health.diffPercentage)
                Text(
                    text = "$action this asset class by ≈${
                        String.format(
                            "%.1f",
                            diff
                        )
                    }% to reach target.",
                    style = MaterialTheme.typography.bodySmall,
                    color = statusColor.copy(alpha = 0.8f),
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

@Composable
fun RebalanceSuggestionsSection(
    suggestions: List<com.sans.finance.domain.model.RebalanceAction>,
    isPrivacyModeEnabled: Boolean,
    currentCurrency: String
) {
    Column {
        Text(
            "SUGGESTED REBALANCING ACTIONS",
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Black,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(bottom = 8.dp),
            letterSpacing = 1.sp
        )
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.extraLarge,
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            border = androidx.compose.foundation.BorderStroke(
                1.dp,
                MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
            )
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                suggestions.forEachIndexed { index, suggestion ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                suggestion.assetClass,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                val actionColor = if (suggestion.action == com.sans.finance.domain.model.RebalanceType.BUY)
                                    Color(0xFF4CAF50) else Color(0xFFF44336)
                                Icon(
                                    imageVector = if (suggestion.action == com.sans.finance.domain.model.RebalanceType.BUY)
                                        Icons.Default.Add else Icons.Default.Remove,
                                    contentDescription = null,
                                    tint = actionColor,
                                    modifier = Modifier.size(12.dp)
                                )
                                Spacer(Modifier.width(4.dp))
                                Text(
                                    suggestion.action.name,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = actionColor,
                                    fontWeight = FontWeight.Black
                                )
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    "Adjust by ${String.format("%.1f%%", suggestion.percentageToAdjust)}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        com.sans.finance.presentation.components.PrivacyText(
                            amount = (suggestion.amount * 100).toLong(),
                            currencyCode = currentCurrency,
                            isVisible = !isPrivacyModeEnabled,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Black,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    if (index < suggestions.size - 1) {
                        androidx.compose.material3.HorizontalDivider(
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun RiskBadge(riskLevel: RiskLevel) {
    val (color, label) = when (riskLevel) {
        RiskLevel.LOW -> Color(0xFF4CAF50) to "Low Risk"
        RiskLevel.MEDIUM -> Color(0xFFFF9800) to "Medium Risk"
        RiskLevel.HIGH -> Color(0xFFF44336) to "High Risk"
        RiskLevel.VERY_HIGH -> Color(0xFF9C27B0) to "Very High Risk"
    }

    Surface(
        color = color.copy(alpha = 0.1f),
        shape = CircleShape,
        modifier = Modifier.padding(start = 8.dp)
    ) {
        Text(
            text = label,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
            style = MaterialTheme.typography.labelSmall,
            color = color,
            fontWeight = FontWeight.Bold
        )
    }
}
