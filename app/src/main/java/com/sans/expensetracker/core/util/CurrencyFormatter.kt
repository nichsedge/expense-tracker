package com.sans.expensetracker.core.util

import java.text.NumberFormat
import java.util.Locale
import kotlin.math.ceil

object CurrencyFormatter {
    private val locale = Locale("id", "ID")

    /**
     * Formats the amount in cents (Long) into a display string.
     * Rounds up to the nearest whole number and removes thousands separators.
     */
    private val formatter = object : ThreadLocal<NumberFormat>() {
        override fun initialValue(): NumberFormat {
            return NumberFormat.getCurrencyInstance(locale).apply {
                isGroupingUsed = true
                maximumFractionDigits = 0
            }
        }
    }

    fun formatAmount(amountInCents: Long): String {
        val amount = ceil(amountInCents / 100.0).toLong()

        // This will include the currency symbol and thousands separator but NO decimal part.
        return formatter.get()?.format(amount) ?: ""
    }

    /**
     * Just the rounded up number without any symbols or separators
     */
    fun formatNumberOnly(amountInCents: Long): String {
        return ceil(amountInCents / 100.0).toLong().toString()
    }
}
