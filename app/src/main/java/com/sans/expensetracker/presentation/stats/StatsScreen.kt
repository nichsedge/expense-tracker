package com.sans.expensetracker.presentation.stats

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Insights
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.sans.expensetracker.presentation.components.CategoryIcon
import com.patrykandpatrick.vico.compose.cartesian.CartesianChartHost
import com.patrykandpatrick.vico.compose.cartesian.axis.HorizontalAxis
import com.patrykandpatrick.vico.compose.cartesian.axis.VerticalAxis
import com.patrykandpatrick.vico.compose.cartesian.data.CartesianChartModel
import com.patrykandpatrick.vico.compose.cartesian.data.CartesianValueFormatter
import com.patrykandpatrick.vico.compose.cartesian.data.LineCartesianLayerModel
import com.patrykandpatrick.vico.compose.cartesian.layer.rememberLineCartesianLayer
import com.patrykandpatrick.vico.compose.cartesian.rememberCartesianChart
import com.patrykandpatrick.vico.compose.common.ProvideVicoTheme
import com.patrykandpatrick.vico.compose.m3.common.rememberM3VicoTheme
import com.sans.expensetracker.R
import com.sans.expensetracker.core.util.CurrencyFormatter
import java.util.*
import kotlin.math.roundToLong

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatsScreen(
    onBack: () -> Unit,
    viewModel: StatsViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(stringResource(R.string.statistics), fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        if (state.isLoading) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                // Monthly Overview Header
                HeaderPart(
                    title = stringResource(R.string.this_month),
                    amount = state.thisMonthSpent,
                    lastMonthAmount = state.lastMonthSpent
                )

                // Spending Trend Chart
                SpendingTrendChart(state.dailySpending)

                // Categories Breakdown
                CategoryBreakdown(state.spendingByCategory)

                // Comparison Cards
                // Comparison Cards
                SectionTitle(stringResource(R.string.yearly_summary), icon = Icons.Default.CalendarMonth)
                
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    StatsSimpleCard(
                        modifier = Modifier.weight(1.0f),
                        title = stringResource(R.string.this_year),
                        amount = state.thisYearSpent,
                        color = MaterialTheme.colorScheme.primaryContainer
                    )
                    StatsSimpleCard(
                        modifier = Modifier.weight(1.0f),
                        title = stringResource(R.string.last_year),
                        amount = state.lastYearSpent,
                        color = MaterialTheme.colorScheme.secondaryContainer
                    )
                }
                
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

@Composable
fun HeaderPart(
    title: String,
    amount: Long,
    lastMonthAmount: Long
) {
    val diff = amount - lastMonthAmount
    val percent = if (lastMonthAmount > 0) (diff.toDouble() / lastMonthAmount * 100).toInt() else 0
    
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
        Text(title, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.secondary)
        Text(
            com.sans.expensetracker.core.util.CurrencyFormatter.formatAmount(amount),
            style = MaterialTheme.typography.displaySmall,
            fontWeight = FontWeight.Black,
            color = MaterialTheme.colorScheme.primary
        )
        
        if (lastMonthAmount > 0) {
            Surface(
                color = if (diff > 0) MaterialTheme.colorScheme.errorContainer else Color(0xFFC8E6C9),
                shape = CircleShape
            ) {
                Text(
                    text = "${if (diff > 0) "+" else ""}$percent% from last month",
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                    style = MaterialTheme.typography.labelSmall,
                    color = if (diff > 0) MaterialTheme.colorScheme.error else Color(0xFF2E7D32),
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
fun SpendingTrendChart(dailySpending: List<com.sans.expensetracker.data.local.entity.DaySpent>) {
    SectionTitle(stringResource(R.string.spending_trend), icon = Icons.Default.Insights)
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
    ) {
        Box(modifier = Modifier.padding(16.dp).fillMaxWidth().height(200.dp)) {
            if (dailySpending.isEmpty()) {
                Text("No data for this month", modifier = Modifier.align(Alignment.Center))
            } else {
                val sortedSpending = remember(dailySpending) { dailySpending.sortedBy { it.day } }
                val model = remember(sortedSpending) {
                    CartesianChartModel(
                        LineCartesianLayerModel.build {
                            series(
                                sortedSpending.map { it.day.toDouble() },
                                sortedSpending.map { it.amount / 100.0 }
                            )
                        }
                    )
                }
                val dateLabelFormatter = remember(sortedSpending) {
                    val dateFormat = java.text.SimpleDateFormat("d MMM", Locale.getDefault())
                    CartesianValueFormatter { _, value, _ ->
                        dateFormat.format(Date(value.roundToLong()))
                    }
                }
                val currencyLabelFormatter = remember {
                    CartesianValueFormatter { _, value, _ ->
                        CurrencyFormatter.formatAmount((value * 100).roundToLong())
                    }
                }
                
                ProvideVicoTheme(rememberM3VicoTheme()) {
                    CartesianChartHost(
                        chart = rememberCartesianChart(
                            rememberLineCartesianLayer(),
                            startAxis = VerticalAxis.rememberStart(
                                valueFormatter = currencyLabelFormatter
                            ),
                            bottomAxis = HorizontalAxis.rememberBottom(
                                valueFormatter = dateLabelFormatter
                            ),
                        ),
                        model = model,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        }
    }
}

@Composable
fun CategoryBreakdown(
    categories: List<com.sans.expensetracker.data.local.entity.CategorySpent>
) {
    SectionTitle(stringResource(R.string.by_category), icon = Icons.Default.PieChart)
    
    val totalInCategories = categories.sumOf { it.totalAmount }
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(8.dp)) {
            categories.sortedByDescending { it.totalAmount }.forEach { category ->
                val percent = if (totalInCategories > 0) category.totalAmount.toFloat() / totalInCategories else 0f
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        CategoryIcon(category.categoryIcon, fontSize = 24.sp)
                    }
                    
                    Spacer(modifier = Modifier.width(16.dp))
                    
                    Column(modifier = Modifier.weight(1f)) {
                        Text(category.categoryName, fontWeight = FontWeight.Bold)
                        LinearProgressIndicator(
                            progress = percent,
                            modifier = Modifier.fillMaxWidth().height(4.dp).clip(CircleShape),
                            color = MaterialTheme.colorScheme.primary,
                            trackColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    }
                    
                    Spacer(modifier = Modifier.width(16.dp))
                    
                    Text(
                        com.sans.expensetracker.core.util.CurrencyFormatter.formatAmount(category.totalAmount),
                        fontWeight = FontWeight.ExtraBold
                    )
                }
            }
        }
    }
}

@Composable
fun StatsSimpleCard(
    modifier: Modifier = Modifier,
    title: String,
    amount: Long,
    color: Color
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = color),
        shape = RoundedCornerShape(24.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(title, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(
                com.sans.expensetracker.core.util.CurrencyFormatter.formatAmount(amount),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.ExtraBold
            )
        }
    }
}

@Composable
fun SectionTitle(title: String, icon: ImageVector? = null) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(bottom = 12.dp)
    ) {
        if (icon != null) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
        }
        Text(
            title,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Black
        )
    }
}
