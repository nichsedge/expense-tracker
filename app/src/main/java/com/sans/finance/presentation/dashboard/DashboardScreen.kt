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


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    onTransactionClick: (Long) -> Unit,
    onRecurringExpensesClick: () -> Unit,
    onInstallmentsClick: () -> Unit,
    onWealthForecastingClick: () -> Unit,
    viewModel: DashboardViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            AppTopBar(
                title = "Dashboard"
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
            contentPadding = PaddingValues(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item {
                NetWorthCard(
                    netWorth = state.netWorth,
                    assets = state.totalAssets,
                    liabilities = state.totalLiabilities,
                    currencyCode = state.currentCurrency,
                    isPrivacyModeEnabled = state.isPrivacyModeEnabled,
                    onTogglePrivacyMode = viewModel::togglePrivacyMode
                )
            }

            item {
                FinancialFreedomCard(
                    yearsOfCover = state.financialFreedomYears,
                    freedomScore = state.financialFreedomScore,
                    totalAssets = state.totalAssets,
                    annualExpense = state.annualExpense,
                    currencyCode = state.currentCurrency,
                    isPrivacyModeEnabled = state.isPrivacyModeEnabled,
                    isManualEnabled = state.isFireManualEnabled,
                    manualAnnualExpense = state.manualFireAnnualExpense,
                    onManualToggle = { viewModel.setFireManualEnabled(it) },
                    onManualAmountChange = { viewModel.setManualFireAnnualExpense(it) }
                )
            }

            item {
                MonthlyCashFlowCard(
                    income = state.monthlyIncome,
                    expense = state.monthlyExpense,
                    cashFlow = state.monthlyCashFlow,
                    savingsRate = state.monthlySavingsRate,
                    currencyCode = state.currentCurrency,
                    isPrivacyModeEnabled = state.isPrivacyModeEnabled
                )
            }

            if (state.globalBudget > 0L) {
                item {
                    GlobalBudgetCard(
                        budget = state.globalBudget,
                        spent = state.globalSpent,
                        daysLeft = state.daysLeftInMonth,
                        currencyCode = state.currentCurrency,
                        isPrivacyModeEnabled = state.isPrivacyModeEnabled
                    )
                }
            }

            item {
                ForecastCard(
                    projectedBalance = state.projectedBalance30Days,
                    trendData = state.last30DaysTrend,
                    currencyCode = state.currentCurrency,
                    isPrivacyModeEnabled = state.isPrivacyModeEnabled,
                    onClick = onWealthForecastingClick
                )
            }

            if (state.wealthDistribution.isNotEmpty()) {
                item {
                    WealthDistributionCard(
                        distribution = state.wealthDistribution,
                        selectedTab = state.wealthDistributionTab,
                        onTabSelected = viewModel::setWealthDistributionTab,
                        currencyCode = state.currentCurrency,
                        isPrivacyModeEnabled = state.isPrivacyModeEnabled
                    )
                }
            }

            if (state.aiSuggestions.isNotEmpty()) {
                item {
                    AiAdvisorCard(suggestions = state.aiSuggestions)
                }
            }

            if (state.goals.isNotEmpty()) {
                item {
                    SectionHeader("GOAL PROGRESS")
                    Spacer(modifier = Modifier.height(4.dp))
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        state.goals.forEach { goal ->
                            DashboardGoalItem(
                                goal,
                                state.currentCurrency,
                                state.isPrivacyModeEnabled
                            )
                        }
                    }
                }
            }

            if (state.upcomingBills.isNotEmpty()) {
                item {
                    var showBillsMenu by remember { mutableStateOf(false) }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        SectionHeader("UPCOMING BILLS")
                        Box {
                            Text(
                                "See All",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier
                                    .clip(CircleShape)
                                    .clickable { showBillsMenu = true }
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                            DropdownMenu(
                                expanded = showBillsMenu,
                                onDismissRequest = { showBillsMenu = false }
                            ) {
                                DropdownMenuItem(
                                    text = { Text("Recurring Payments") },
                                    onClick = {
                                        showBillsMenu = false
                                        onRecurringExpensesClick()
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Installments") },
                                    onClick = {
                                        showBillsMenu = false
                                        onInstallmentsClick()
                                    }
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        state.upcomingBills.forEach { bill ->
                            DashboardBillItem(
                                bill,
                                state.currentCurrency,
                                state.isPrivacyModeEnabled
                            )
                        }
                    }
                }
            }
            if (state.recentTransactions.isNotEmpty()) {
                item {
                    SectionHeader("RECENT TRANSACTIONS")
                    Spacer(modifier = Modifier.height(6.dp))
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        state.recentTransactions.forEach { transaction ->
                            RecentTransactionItem(
                                transaction = transaction,
                                currencyCode = state.currentCurrency,
                                isPrivacyModeEnabled = state.isPrivacyModeEnabled,
                                onClick = { onTransactionClick(transaction.id) }
                            )
                        }
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(32.dp)) }
        }
    }
}
