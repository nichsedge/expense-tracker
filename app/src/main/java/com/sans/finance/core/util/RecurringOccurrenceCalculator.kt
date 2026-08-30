package com.sans.finance.core.util

import java.util.Calendar

data class RecurringOccurrence(
    val occurrenceIndex: Int,
    val date: Long
)

object RecurringOccurrenceCalculator {

    private const val SYNTHETIC_ID_OFFSET = 200_000_000L
    private const val SYNTHETIC_ID_MULTIPLIER = 10_000L
    private const val MAX_OCCURRENCES_CAP = 500

    /**
     * Calculates all occurrence timestamps for a recurring rule that fall within [since, until).
     */
    fun calculateOccurrences(
        startDate: Long,
        interval: String?,
        multiplier: Int = 1,
        endType: String? = "NEVER",
        endDate: Long? = null,
        totalOccurrences: Int? = null,
        status: String = "ACTIVE",
        since: Long = 0L,
        until: Long = Long.MAX_VALUE
    ): List<RecurringOccurrence> {
        if (status.equals("PAUSED", ignoreCase = true) || status.equals("CANCELLED", ignoreCase = true)) {
            return emptyList()
        }

        val effectiveMultiplier = multiplier.coerceAtLeast(1)
        val effectiveInterval = interval?.uppercase() ?: "MONTHLY"
        val effectiveEndType = endType?.uppercase() ?: "NEVER"

        val maxAllowedOccurrences = when (effectiveEndType) {
            "AFTER_COUNT" -> (totalOccurrences ?: 1).coerceAtLeast(0)
            else -> MAX_OCCURRENCES_CAP
        }

        if (maxAllowedOccurrences == 0) return emptyList()

        val startCal = CalendarUtils.getInstance().apply { timeInMillis = startDate }
        val startDay = startCal.get(Calendar.DAY_OF_MONTH)

        val occurrences = ArrayList<RecurringOccurrence>()

        for (index in 0 until maxAllowedOccurrences) {
            val occurrenceTime = calculateOccurrenceDate(
                startDate = startDate,
                startDay = startDay,
                interval = effectiveInterval,
                multiplier = effectiveMultiplier,
                index = index
            )

            // Check End Date condition
            if (effectiveEndType == "UNTIL_DATE" && endDate != null && occurrenceTime > endDate) {
                break
            }

            // If past the requested window end, stop iterating
            if (occurrenceTime >= until) {
                break
            }

            // Include if within the requested [since, until) window
            if (occurrenceTime >= since) {
                occurrences.add(RecurringOccurrence(occurrenceIndex = index, date = occurrenceTime))
            }
        }

        return occurrences
    }

    /**
     * Calculates the next due date on or strictly after the given reference time (typically now).
     */
    fun calculateNextDueDate(
        startDate: Long,
        interval: String?,
        multiplier: Int = 1,
        endType: String? = "NEVER",
        endDate: Long? = null,
        totalOccurrences: Int? = null,
        status: String = "ACTIVE",
        afterTime: Long = System.currentTimeMillis()
    ): Long? {
        if (status.equals("PAUSED", ignoreCase = true) || status.equals("CANCELLED", ignoreCase = true)) {
            return null
        }

        val effectiveMultiplier = multiplier.coerceAtLeast(1)
        val effectiveInterval = interval?.uppercase() ?: "MONTHLY"
        val effectiveEndType = endType?.uppercase() ?: "NEVER"

        val maxAllowedOccurrences = when (effectiveEndType) {
            "AFTER_COUNT" -> (totalOccurrences ?: 1).coerceAtLeast(0)
            else -> MAX_OCCURRENCES_CAP
        }

        if (maxAllowedOccurrences == 0) return null

        val startCal = CalendarUtils.getInstance().apply { timeInMillis = startDate }
        val startDay = startCal.get(Calendar.DAY_OF_MONTH)

        for (index in 0 until maxAllowedOccurrences) {
            val occurrenceTime = calculateOccurrenceDate(
                startDate = startDate,
                startDay = startDay,
                interval = effectiveInterval,
                multiplier = effectiveMultiplier,
                index = index
            )

            if (effectiveEndType == "UNTIL_DATE" && endDate != null && occurrenceTime > endDate) {
                return null
            }

            if (occurrenceTime >= afterTime) {
                return occurrenceTime
            }
        }

        return null
    }

    /**
     * Generates a deterministic synthetic negative ID for a projected recurring occurrence.
     */
    fun generateSyntheticId(parentRuleId: Long, occurrenceIndex: Int): Long {
        return -(SYNTHETIC_ID_OFFSET + (parentRuleId * SYNTHETIC_ID_MULTIPLIER) + occurrenceIndex)
    }

    /**
     * Checks whether an ID is a synthetic recurring instance ID.
     */
    fun isSyntheticRecurringId(id: Long): Boolean {
        return id <= -SYNTHETIC_ID_OFFSET
    }

    /**
     * Extracts the parent rule ID from a synthetic recurring instance ID.
     */
    fun extractParentRuleId(syntheticId: Long): Long {
        if (!isSyntheticRecurringId(syntheticId)) return syntheticId
        val absVal = -syntheticId - SYNTHETIC_ID_OFFSET
        return absVal / SYNTHETIC_ID_MULTIPLIER
    }

    /**
     * Extracts the occurrence index from a synthetic recurring instance ID.
     */
    fun extractOccurrenceIndex(syntheticId: Long): Int {
        if (!isSyntheticRecurringId(syntheticId)) return 0
        val absVal = -syntheticId - SYNTHETIC_ID_OFFSET
        return (absVal % SYNTHETIC_ID_MULTIPLIER).toInt()
    }

    private fun calculateOccurrenceDate(
        startDate: Long,
        startDay: Int,
        interval: String,
        multiplier: Int,
        index: Int
    ): Long {
        if (index == 0) return startDate

        val cal = CalendarUtils.getInstance()
        cal.timeInMillis = startDate

        when (interval) {
            "DAILY" -> {
                cal.add(Calendar.DAY_OF_YEAR, index * multiplier)
            }
            "WEEKLY" -> {
                cal.add(Calendar.WEEK_OF_YEAR, index * multiplier)
            }
            "MONTHLY" -> {
                cal.add(Calendar.MONTH, index * multiplier)
                val maxDay = cal.getActualMaximum(Calendar.DAY_OF_MONTH)
                cal.set(Calendar.DAY_OF_MONTH, startDay.coerceAtMost(maxDay))
            }
            "YEARLY" -> {
                cal.add(Calendar.YEAR, index * multiplier)
                val maxDay = cal.getActualMaximum(Calendar.DAY_OF_MONTH)
                cal.set(Calendar.DAY_OF_MONTH, startDay.coerceAtMost(maxDay))
            }
            else -> {
                cal.add(Calendar.MONTH, index * multiplier)
            }
        }
        return cal.timeInMillis
    }
}
