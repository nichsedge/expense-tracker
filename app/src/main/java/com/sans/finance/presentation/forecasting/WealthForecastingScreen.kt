package com.sans.finance.presentation.forecasting

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sans.finance.core.util.CurrencyFormatter
import com.sans.finance.presentation.components.AppTopBar
import com.sans.finance.presentation.components.GlassCard

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WealthForecastingScreen(
    onBack: () -> Unit,
    viewModel: WealthForecastingViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            AppTopBar(
                title = "Wealth Trajectory",
                onBack = onBack
            )
        }
    ) { paddingValues ->
        Column(
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
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Summary Card
            GlassCard(
                modifier = Modifier.fillMaxWidth(),
                containerColor = MaterialTheme.colorScheme.primary,
                alpha = 0.15f
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        "Future Wealth Projection",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        CurrencyFormatter.formatAmount(
                            state.projections.lastOrNull()?.value ?: 0L,
                            state.currentCurrency
                        ),
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    if (state.extraMonthlyContribution > 0) {
                        Text(
                            "What-if: " + CurrencyFormatter.formatAmount(
                                state.whatIfProjections.lastOrNull()?.value ?: 0L,
                                state.currentCurrency
                            ),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.tertiary
                        )
                    }
                    Text(
                        "Estimated in ${state.projectionYears} years",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                }
            }

            // Controls
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
                    Text(
                        "EXPECTED ANNUAL ROI",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.secondary
                    )
                    Text(
                        "${(state.expectedRoi * 100).toInt()}% per year",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Black
                    )
                    Slider(
                        value = state.expectedRoi,
                        onValueChange = { viewModel.updateRoi(it) },
                        valueRange = 0f..0.20f,
                        steps = 19
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        "EXTRA MONTHLY SAVINGS (WHAT-IF)",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.secondary
                    )
                    Text(
                        CurrencyFormatter.formatAmount(state.extraMonthlyContribution, state.currentCurrency),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.tertiary,
                        fontWeight = FontWeight.Black
                    )
                    Slider(
                        value = state.extraMonthlyContribution.toFloat(),
                        onValueChange = { viewModel.updateExtraContribution(it.toLong()) },
                        valueRange = 0f..100_000_000f, // Max 1M USD roughly in cents is 100k USD. 100M IDR is reasonable.
                        // Actually let's just use reasonable steps
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        InfoItem(
                            "Monthly Savings",
                            CurrencyFormatter.formatAmount(
                                state.monthlySavings,
                                state.currentCurrency
                            )
                        )
                        InfoItem(
                            "Current Wealth",
                            CurrencyFormatter.formatAmount(
                                state.currentNetWorth,
                                state.currentCurrency
                            )
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        "PROJECTION HORIZON",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.secondary
                    )
                    Text(
                        "${state.projectionYears} years",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Black
                    )
                    Slider(
                        value = state.projectionYears.toFloat(),
                        onValueChange = { viewModel.updateProjectionYears(it.toInt()) },
                        valueRange = 5f..50f,
                        steps = 8
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        "EMERGENCY FUND TARGET",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.secondary
                    )
                    Text(
                        "${state.emergencyFundMonths} months of expenses",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Black
                    )
                    Slider(
                        value = state.emergencyFundMonths.toFloat(),
                        onValueChange = { viewModel.updateEmergencyFundMonths(it.toInt()) },
                        valueRange = 1f..24f,
                        steps = 22
                    )
                }
            }

            // Chart
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.extraLarge,
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        "Growth Trajectory",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    TrajectoryChart(
                        projections = state.projections,
                        whatIfProjections = if (state.extraMonthlyContribution > 0) state.whatIfProjections else null,
                        currencyCode = state.currentCurrency
                    )
                }
            }

            // FIRE Index
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.extraLarge,
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.4f)),
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.tertiary.copy(alpha = 0.2f))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.Info,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.tertiary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            "FIRE INDEX",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 1.sp
                        )
                    }
                    Spacer(Modifier.height(16.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Column {
                            Text("Target (25x Expenses)", style = MaterialTheme.typography.labelSmall)
                            Text(
                                CurrencyFormatter.formatAmountCompact(state.fireNumber, state.currentCurrency),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Black
                            )
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text("Years to Freedom", style = MaterialTheme.typography.labelSmall)
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    state.yearsToFire?.let { "$it Years" } ?: "∞",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Black,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                if (state.whatIfYearsToFire != null && state.whatIfYearsToFire != state.yearsToFire) {
                                    Spacer(Modifier.width(8.dp))
                                    Icon(Icons.Default.Info, null, modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.tertiary)
                                    Text(
                                        "${state.whatIfYearsToFire}y",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Black,
                                        color = MaterialTheme.colorScheme.tertiary
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Emergency Fund
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.extraLarge,
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        "Emergency Fund (${state.emergencyFundMonths} Months)",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Black
                    )
                    Spacer(Modifier.height(12.dp))
                    val progress = if (state.emergencyFundTarget > 0) (state.currentEmergencyFund.toFloat() / state.emergencyFundTarget).coerceIn(0f, 1f) else 0f
                    androidx.compose.material3.LinearProgressIndicator(
                        progress = { progress },
                        modifier = Modifier.fillMaxWidth().height(8.dp).clip(MaterialTheme.shapes.small),
                        color = if (progress >= 1f) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.primary,
                        trackColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                    Spacer(Modifier.height(8.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("${(progress * 100).toInt()}% Covered", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = if (progress >= 1f) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.primary)
                        Text(
                            "${CurrencyFormatter.formatAmountCompact(state.currentEmergencyFund, state.currentCurrency)} / ${CurrencyFormatter.formatAmountCompact(state.emergencyFundTarget, state.currentCurrency)}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Educational Note
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.2f),
                shape = MaterialTheme.shapes.large,
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.secondary.copy(alpha = 0.1f))
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Info,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        "This projection assumes constant monthly savings and a fixed annual ROI. Real-world returns will vary.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                        lineHeight = 16.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
fun InfoItem(label: String, value: String) {
    Column {
        Text(
            label.uppercase(),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.5.sp
        )
        Text(
            value,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Black,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
fun TrajectoryChart(
    projections: List<ProjectionPoint>,
    whatIfProjections: List<ProjectionPoint>? = null,
    currencyCode: String
) {
    if (projections.isEmpty()) return

    val textMeasurer = rememberTextMeasurer()
    val labelStyle = TextStyle(
        fontSize = 10.sp,
        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
    )
    val primaryColor = MaterialTheme.colorScheme.primary
    val whatIfColor = MaterialTheme.colorScheme.tertiary
    val gridColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f)
    var selectedIndex by remember { mutableStateOf<Int?>(null) }
    val tooltipStyle = MaterialTheme.typography.labelMedium.copy(
        color = MaterialTheme.colorScheme.onPrimary,
        fontWeight = FontWeight.Bold
    )
    val tooltipYearStyle = MaterialTheme.typography.labelSmall.copy(
        color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f)
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(220.dp)
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(projections.size) {
                    detectDragGestures(
                        onDragStart = { offset ->
                            val chartLeft = 80f
                            val chartRight = size.width - 20f
                            val chartWidth = chartRight - chartLeft
                            val index = ((offset.x - chartLeft) / chartWidth * (projections.size - 1)).toInt()
                                .coerceIn(0, projections.size - 1)
                            selectedIndex = index
                        },
                        onDrag = { change, _ ->
                            val chartLeft = 80f
                            val chartRight = size.width - 20f
                            val chartWidth = chartRight - chartLeft
                            val index = ((change.position.x - chartLeft) / chartWidth * (projections.size - 1)).toInt()
                                .coerceIn(0, projections.size - 1)
                            selectedIndex = index
                        },
                        onDragEnd = { selectedIndex = null },
                        onDragCancel = { selectedIndex = null }
                    )
                }
                .pointerInput(projections.size) {
                    detectTapGestures(
                        onPress = { offset ->
                            val chartLeft = 80f
                            val chartRight = size.width - 20f
                            val chartWidth = chartRight - chartLeft
                            val index = ((offset.x - chartLeft) / chartWidth * (projections.size - 1)).toInt()
                                .coerceIn(0, projections.size - 1)
                            selectedIndex = index
                            tryAwaitRelease()
                            selectedIndex = null
                        }
                    )
                }
        ) {
            val padding = 60f
            val chartTop = 20f
            val chartBottom = size.height - padding
            val chartLeft = 80f
            val chartRight = size.width - 20f
            val chartHeight = chartBottom - chartTop
            val chartWidth = chartRight - chartLeft

            val maxValNormal = projections.maxOf { it.value }
            val maxValWhatIf = whatIfProjections?.maxOf { it.value } ?: 0L
            val maxVal = maxOf(maxValNormal, maxValWhatIf).coerceAtLeast(1L)
            val range = maxVal.toFloat()

            // Draw Y-axis
            val yLines = 5
            for (i in 0 until yLines) {
                val fraction = i.toFloat() / (yLines - 1)
                val y = chartBottom - (fraction * chartHeight)
                val value = (fraction * range).toLong()
                drawLine(
                    color = gridColor,
                    start = Offset(chartLeft, y),
                    end = Offset(chartRight, y),
                    strokeWidth = 1f
                )
                val label = CurrencyFormatter.formatAmountCompact(value, currencyCode)
                val layout = textMeasurer.measure(label, style = labelStyle)
                drawText(textLayoutResult = layout, color = labelStyle.color, topLeft = Offset(chartLeft - layout.size.width - 8f, y - layout.size.height / 2f))
            }

            // Draw X-axis
            val xSteps = listOf(0, 5, 10, 15, 20, 25).filter { it <= projections.last().year }
            xSteps.forEach { year ->
                val xFraction = year.toFloat() / projections.last().year.toFloat()
                val x = chartLeft + (xFraction * chartWidth)
                val layout = textMeasurer.measure("${year}y", style = labelStyle)
                drawText(textLayoutResult = layout, color = labelStyle.color, topLeft = Offset(x - layout.size.width / 2f, chartBottom + 8f))
            }

            // Helper to build path
            fun buildPath(data: List<ProjectionPoint>): Path {
                val path = Path()
                data.forEachIndexed { index, point ->
                    val x = chartLeft + (point.year.toFloat() / data.last().year.toFloat() * chartWidth)
                    val y = chartBottom - (point.value.toFloat() / range * chartHeight)
                    if (index == 0) path.moveTo(x, y) else path.lineTo(x, y)
                }
                return path
            }

            // Normal path
            drawPath(buildPath(projections), primaryColor, style = Stroke(width = 6f))

            // What-if path
            whatIfProjections?.let {
                drawPath(buildPath(it), whatIfColor, style = Stroke(width = 4f, pathEffect = PathEffect.dashPathEffect(floatArrayOf(15f, 10f), 0f)))
            }

            // Selected Tooltip
            selectedIndex?.let { idx ->
                val p = projections[idx]
                val x = chartLeft + (p.year.toFloat() / projections.last().year.toFloat() * chartWidth)
                val y = chartBottom - (p.value.toFloat() / range * chartHeight)

                drawLine(
                    color = primaryColor.copy(alpha = 0.5f),
                    start = Offset(x, chartTop),
                    end = Offset(x, chartBottom),
                    strokeWidth = 2.dp.toPx(),
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)
                )
                drawCircle(primaryColor, 6.dp.toPx(), Offset(x, y))

                val pWhatIf = whatIfProjections?.getOrNull(idx)
                val yWhatIf = pWhatIf?.let { chartBottom - (it.value.toFloat() / range * chartHeight) }
                if (yWhatIf != null) {
                    drawCircle(whatIfColor, 6.dp.toPx(), Offset(x, yWhatIf))
                }

                // Tooltip box
                val yearStr = "Year ${p.year}"
                val valStr = CurrencyFormatter.formatAmountCompact(p.value, currencyCode)
                val whatIfStr = pWhatIf?.let { "What-if: " + CurrencyFormatter.formatAmountCompact(it.value, currencyCode) }

                val layouts = listOfNotNull(
                    textMeasurer.measure(yearStr, tooltipYearStyle),
                    textMeasurer.measure(valStr, tooltipStyle),
                    whatIfStr?.let { textMeasurer.measure(it, tooltipStyle.copy(color = whatIfColor)) }
                )

                val tooltipWidth = layouts.maxOf { it.size.width } + 24.dp.toPx()
                val tooltipHeight = layouts.sumOf { it.size.height } + 16.dp.toPx()
                val tx = (x - tooltipWidth / 2f).coerceIn(chartLeft + 4f, chartRight - tooltipWidth - 4f)
                val ty = (minOf(y, yWhatIf ?: y) - tooltipHeight - 16.dp.toPx()).coerceAtLeast(chartTop + 4f)

                drawRoundRect(primaryColor.copy(alpha = 0.9f), Offset(tx, ty), Size(tooltipWidth, tooltipHeight), CornerRadius(8.dp.toPx()))
                var currentY = ty + 8.dp.toPx()
                layouts.forEach {
                    drawText(textLayoutResult = it, color = Color.White, topLeft = Offset(tx + 12.dp.toPx(), currentY))
                    currentY += it.size.height
                }
            }
        }
    }
}
