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
fun NetWorthCard(
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
        alpha = 0.15f
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
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Black,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                BreakdownItem(
                    "Assets",
                    assets,
                    MaterialTheme.colorScheme.tertiary,
                    currencyCode,
                    isPrivacyModeEnabled
                )
                VerticalDivider(
                    modifier = Modifier.height(32.dp),
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                )
                BreakdownItem(
                    "Liabilities",
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
fun BreakdownItem(
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
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Black,
            color = color
        )
    }
}

