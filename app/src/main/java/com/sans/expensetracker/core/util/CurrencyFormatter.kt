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
    fun formatAmount(amountInCents: Long): String {
        val amount = ceil(amountInCents / 100.0).toLong()
        val formatter = NumberFormat.getCurrencyInstance(locale)
        formatter.isGroupingUsed = true
        formatter.maximumFractionDigits = 0
        
        // This will include the currency symbol and thousands separator but NO decimal part.
        return formatter.format(amount)
    }

    /**
     * Just the rounded up number without any symbols or separators
     */
    fun formatNumberOnly(amountInCents: Long): String {
        return ceil(amountInCents / 100.0).toLong().toString()
    }
}
