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
import com.sans.finance.data.local.dao.ExpenseDao
import com.sans.finance.data.util.LocaleManager
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.Calendar

class QuickAddWidgetProvider : AppWidgetProvider() {

    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface WidgetEntryPoint {
        fun expenseDao(): ExpenseDao
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
                    WidgetEntryPoint::class.java
                )
                val expenseDao = entryPoint.expenseDao()
                val localeManager = entryPoint.localeManager()

                // Calculate today's time range
                val cal = Calendar.getInstance().apply {
                    set(Calendar.HOUR_OF_DAY, 0)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }
                val startOfDay = cal.timeInMillis
                cal.add(Calendar.DAY_OF_YEAR, 1)
                val endOfDay = cal.timeInMillis

                val summary = expenseDao.getTodaySpentSummary(startOfDay, endOfDay)
                val totalSpentCents = summary.totalAmount ?: 0L
                val currency = localeManager.getCurrency()
                val formattedTotal = CurrencyFormatter.formatAmount(totalSpentCents, currency)

                val countText = if (summary.count == 0) {
                    context.getString(R.string.widget_no_transactions_today)
                } else {
                    context.getString(R.string.widget_transactions_today_count, summary.count)
                }

                for (widgetId in appWidgetIds) {
                    val views = RemoteViews(context.packageName, R.layout.widget_quick_add).apply {
                        setTextViewText(R.id.widget_today_amount, formattedTotal)
                        setTextViewText(R.id.widget_today_count, countText)

                        // 1. Add Transaction PendingIntent
                        val addIntent = Intent(context, MainActivity::class.java).apply {
                            action = MainActivity.ACTION_ADD_TRANSACTION
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                        }
                        val addPendingIntent = PendingIntent.getActivity(
                            context,
                            1001,
                            addIntent,
                            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                        )
                        setOnClickPendingIntent(R.id.widget_btn_add, addPendingIntent)

                        // 2. Transactions History PendingIntent
                        val historyIntent = Intent(context, MainActivity::class.java).apply {
                            action = MainActivity.ACTION_VIEW_TRANSACTIONS
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                        }
                        val historyPendingIntent = PendingIntent.getActivity(
                            context,
                            1002,
                            historyIntent,
                            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                        )
                        setOnClickPendingIntent(R.id.widget_btn_history, historyPendingIntent)

                        // 3. Header and Body PendingIntent (Opens Main)
                        val mainIntent = Intent(context, MainActivity::class.java).apply {
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                        }
                        val mainPendingIntent = PendingIntent.getActivity(
                            context,
                            1000,
                            mainIntent,
                            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                        )
                        setOnClickPendingIntent(R.id.widget_header, mainPendingIntent)
                        setOnClickPendingIntent(R.id.widget_spending_section, mainPendingIntent)
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
                com.sans.finance.presentation.widget.glance.QuickAddGlanceReceiver.updateAll(context)
                val appWidgetManager = AppWidgetManager.getInstance(context)
                val componentName = ComponentName(context, QuickAddWidgetProvider::class.java)
                val widgetIds = appWidgetManager.getAppWidgetIds(componentName)
                if (widgetIds.isNotEmpty()) {
                    val intent = Intent(context, QuickAddWidgetProvider::class.java).apply {
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
