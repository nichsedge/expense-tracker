package com.sans.finance.presentation.widget.glance

import android.content.Context
import android.content.Intent
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.LinearProgressIndicator
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import com.sans.finance.MainActivity
import com.sans.finance.R
import com.sans.finance.core.util.CurrencyFormatter
import com.sans.finance.domain.usecase.GetDashboardSummaryUseCase
import com.sans.finance.domain.usecase.GetWealthMetricsUseCase
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.flow.first

class FinancialSummaryGlanceWidget : GlanceAppWidget() {

    override val sizeMode: SizeMode = SizeMode.Exact

    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface SummaryEntryPoint {
        fun getDashboardSummaryUseCase(): GetDashboardSummaryUseCase
        fun getWealthMetricsUseCase(): GetWealthMetricsUseCase
    }

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val entryPoint = EntryPointAccessors.fromApplication(
            context.applicationContext,
            SummaryEntryPoint::class.java
        )

        var formattedNetWorth = "--"
        var budgetText = context.getString(R.string.widget_no_budget_set)
        var budgetProgress = 0f
        var statusText = context.getString(R.string.widget_no_transactions_today)

        try {
            val summary = entryPoint.getDashboardSummaryUseCase()().first()
            val metrics = entryPoint.getWealthMetricsUseCase()().first()
            val baseCurrency = summary.currentCurrency

            formattedNetWorth = CurrencyFormatter.formatAmount(summary.netWorth, baseCurrency)

            if (summary.globalBudget > 0L) {
                budgetProgress = (summary.globalSpent.toFloat() / summary.globalBudget.toFloat()).coerceIn(0f, 1f)
                val formattedSpent = CurrencyFormatter.formatAmountCompact(summary.globalSpent, baseCurrency)
                val formattedBudget = CurrencyFormatter.formatAmountCompact(summary.globalBudget, baseCurrency)
                val pct = (budgetProgress * 100).toInt()
                budgetText = "$formattedSpent / $formattedBudget ($pct%)"
            }

            statusText = when {
                summary.spendingVelocity > 1.1f -> "⚠️ Pacing: High velocity"
                metrics.runwayMonths > 0 -> "🛡️ ${String.format(java.util.Locale.US, "%.1f", metrics.runwayMonths)} mo runway"
                else -> context.getString(R.string.widget_no_transactions_today)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        val addIntent = Intent(context, MainActivity::class.java).apply {
            action = MainActivity.ACTION_ADD_TRANSACTION
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }

        val mainIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }

        provideContent {
            GlanceTheme {
                Box(
                    modifier = GlanceModifier
                        .fillMaxSize()
                        .background(GlanceTheme.colors.widgetBackground)
                        .cornerRadius(20.dp)
                        .padding(14.dp)
                        .clickable(actionStartActivity(mainIntent))
                ) {
                    Column(
                        modifier = GlanceModifier.fillMaxSize(),
                        verticalAlignment = Alignment.Top,
                        horizontalAlignment = Alignment.Start
                    ) {
                        // Header
                        Row(
                            modifier = GlanceModifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "NET WORTH",
                                style = TextStyle(
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = GlanceTheme.colors.secondary
                                ),
                                modifier = GlanceModifier.defaultWeight()
                            )
                            Box(
                                modifier = GlanceModifier
                                    .background(GlanceTheme.colors.primary)
                                    .cornerRadius(12.dp)
                                    .padding(horizontal = 10.dp, vertical = 4.dp)
                                    .clickable(actionStartActivity(addIntent))
                            ) {
                                Text(
                                    text = "+ Add",
                                    style = TextStyle(
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = GlanceTheme.colors.onPrimary
                                    )
                                )
                            }
                        }

                        // Net worth value
                        Text(
                            text = formattedNetWorth,
                            style = TextStyle(
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                                color = GlanceTheme.colors.onSurface
                            ),
                            modifier = GlanceModifier.padding(top = 2.dp, bottom = 6.dp)
                        )

                        // Budget bar
                        Text(
                            text = "Budget: $budgetText",
                            style = TextStyle(
                                fontSize = 11.sp,
                                color = GlanceTheme.colors.onSurfaceVariant
                            )
                        )
                        Spacer(modifier = GlanceModifier.height(4.dp))
                        LinearProgressIndicator(
                            progress = budgetProgress,
                            modifier = GlanceModifier
                                .fillMaxWidth()
                                .height(6.dp)
                                .cornerRadius(3.dp),
                            color = GlanceTheme.colors.primary,
                            backgroundColor = GlanceTheme.colors.surfaceVariant
                        )

                        Spacer(modifier = GlanceModifier.defaultWeight())

                        // Footer / Status
                        Text(
                            text = statusText,
                            style = TextStyle(
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium,
                                color = GlanceTheme.colors.onSurfaceVariant
                            )
                        )
                    }
                }
            }
        }
    }
}
