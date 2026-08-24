package com.sans.finance.presentation.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import com.sans.finance.MainActivity
import com.sans.finance.R
import com.sans.finance.core.util.CurrencyFormatter
import com.sans.finance.core.util.DateFormatterUtils
import com.sans.finance.data.local.dao.AccountDao
import com.sans.finance.data.local.dao.AccountTypeDao
import com.sans.finance.data.local.dao.BudgetDao
import com.sans.finance.data.local.dao.CurrencyDao
import com.sans.finance.data.local.dao.ExpenseDao
import com.sans.finance.data.local.dao.PortfolioDao
import com.sans.finance.data.util.LocaleManager
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.util.Calendar
import java.util.Date

class FinancialSummaryWidgetProvider : AppWidgetProvider() {

    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface SummaryEntryPoint {
        fun expenseDao(): ExpenseDao
        fun budgetDao(): BudgetDao
        fun accountDao(): AccountDao
        fun accountTypeDao(): AccountTypeDao
        fun portfolioDao(): PortfolioDao
        fun currencyDao(): CurrencyDao
        fun localeManager(): LocaleManager
    }

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        if (appWidgetIds.isEmpty()) return

        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val entryPoint = EntryPointAccessors.fromApplication(
                    context.applicationContext,
                    SummaryEntryPoint::class.java
                )
                val expenseDao = entryPoint.expenseDao()
                val budgetDao = entryPoint.budgetDao()
                val accountDao = entryPoint.accountDao()
                val accountTypeDao = entryPoint.accountTypeDao()
                val portfolioDao = entryPoint.portfolioDao()
                val currencyDao = entryPoint.currencyDao()
                val localeManager = entryPoint.localeManager()

                val baseCurrency = localeManager.getCurrency()

                // --- Net Worth Calculation ---
                val accounts = accountDao.getAllAccounts().first()
                val types = accountTypeDao.getAllAccountTypes().first()
                val rates = currencyDao.getAllRates().first()
                val ratesMap = rates.associate { it.code to it.rateToIdr }
                val baseRate = if (baseCurrency == "IDR") 1.0 else (ratesMap[baseCurrency] ?: 1.0)

                fun convertToBase(amount: Long, from: String): Long {
                    if (from == baseCurrency) return amount
                    val amountInIdr = if (from == "IDR") amount.toDouble() else amount * (ratesMap[from] ?: 1.0)
                    return (amountInIdr / baseRate).toLong()
                }

                val liabilityTypeNames = types.filter { it.isLiability }.map { it.name }.toSet()
                var cashTotal = 0L
                var liabilitiesTotal = 0L
                accounts.forEach { account ->
                    val converted = convertToBase(account.balance, account.currency)
                    if (account.type in liabilityTypeNames) {
                        liabilitiesTotal += converted
                    } else {
                        cashTotal += converted
                    }
                }

                val latestHeader = portfolioDao.getLatestSnapshotHeader().first()
                val portfolioTotal = if (latestHeader != null) {
                    val portfolioIdr = (latestHeader.totalValueIdr * 100).toLong()
                    convertToBase(portfolioIdr, "IDR")
                } else {
                    0L
                }
                val netWorthTotal = cashTotal - liabilitiesTotal + portfolioTotal
                val formattedNetWorth = CurrencyFormatter.formatAmount(netWorthTotal, baseCurrency)

                // --- Budget Calculation ---
                val calendar = Calendar.getInstance().apply {
                    set(Calendar.DAY_OF_MONTH, 1)
                    set(Calendar.HOUR_OF_DAY, 0)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }
                val startOfMonth = calendar.timeInMillis
                calendar.add(Calendar.MONTH, 1)
                val endOfMonth = calendar.timeInMillis

                val budgets = budgetDao.getAllBudgets().first()
                val totalBudgetCents = budgets.sumOf { it.amount }
                val totalSpentCents = expenseDao.getTotalSpentBetween(startOfMonth, endOfMonth).first() ?: 0L

                val budgetPct = if (totalBudgetCents > 0) {
                    ((totalSpentCents.toDouble() / totalBudgetCents) * 100).toInt()
                } else {
                    0
                }

                val budgetText = if (budgets.isEmpty()) {
                    context.getString(R.string.widget_no_budget_set)
                } else {
                    val formattedSpent = CurrencyFormatter.formatAmountCompact(totalSpentCents, baseCurrency)
                    val formattedBudget = CurrencyFormatter.formatAmountCompact(totalBudgetCents, baseCurrency)
                    "$formattedSpent / $formattedBudget"
                }

                val budgetPctText = if (budgets.isNotEmpty()) "$budgetPct%" else ""

                // --- Today's Spending ---
                val todayCal = Calendar.getInstance().apply {
                    set(Calendar.HOUR_OF_DAY, 0)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }
                val startOfDay = todayCal.timeInMillis
                todayCal.add(Calendar.DAY_OF_YEAR, 1)
                val endOfDay = todayCal.timeInMillis

                val todaySummary = expenseDao.getTodaySpentSummary(startOfDay, endOfDay)
                val todaySpent = todaySummary.totalAmount ?: 0L

                val todayText = if (todaySummary.count == 0) {
                    context.getString(R.string.widget_no_transactions_today)
                } else {
                    val formattedToday = CurrencyFormatter.formatAmountCompact(todaySpent, baseCurrency)
                    context.getString(R.string.widget_summary_today_spent, formattedToday, todaySummary.count)
                }

                val dateStr = DateFormatterUtils.getDayMonthFormatter().format(Date())

                // --- Build RemoteViews ---
                for (widgetId in appWidgetIds) {
                    val views = RemoteViews(context.packageName, R.layout.widget_financial_summary).apply {
                        setTextViewText(R.id.widget_summary_net_worth, formattedNetWorth)
                        setTextViewText(R.id.widget_summary_budget_text, budgetText)
                        setTextViewText(R.id.widget_summary_budget_pct, budgetPctText)
                        setProgressBar(R.id.widget_summary_progress, 100, budgetPct.coerceIn(0, 100), false)
                        setTextViewText(R.id.widget_summary_today, todayText)
                        setTextViewText(R.id.widget_summary_date, dateStr)

                        // 1. Add Transaction PendingIntent
                        val addIntent = Intent(context, MainActivity::class.java).apply {
                            action = MainActivity.ACTION_ADD_TRANSACTION
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                        }
                        val addPendingIntent = PendingIntent.getActivity(
                            context,
                            3001,
                            addIntent,
                            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                        )
                        setOnClickPendingIntent(R.id.widget_summary_btn_add, addPendingIntent)

                        // 2. View Dashboard PendingIntent
                        val dashboardIntent = Intent(context, MainActivity::class.java).apply {
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                        }
                        val dashboardPendingIntent = PendingIntent.getActivity(
                            context,
                            3002,
                            dashboardIntent,
                            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                        )
                        setOnClickPendingIntent(R.id.widget_summary_btn_view, dashboardPendingIntent)
                        setOnClickPendingIntent(R.id.widget_summary_header, dashboardPendingIntent)
                        setOnClickPendingIntent(R.id.widget_summary_body, dashboardPendingIntent)
                    }

                    appWidgetManager.updateAppWidget(widgetId, views)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                pendingResult.finish()
            }
        }
    }

    companion object {
        fun updateAllWidgets(context: Context) {
            try {
                val appWidgetManager = AppWidgetManager.getInstance(context)
                val componentName = ComponentName(context, FinancialSummaryWidgetProvider::class.java)
                val widgetIds = appWidgetManager.getAppWidgetIds(componentName)
                if (widgetIds.isNotEmpty()) {
                    val intent = Intent(context, FinancialSummaryWidgetProvider::class.java).apply {
                        action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
                        putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, widgetIds)
                    }
                    context.sendBroadcast(intent)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}
