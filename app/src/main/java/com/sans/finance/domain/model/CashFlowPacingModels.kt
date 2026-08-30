package com.sans.finance.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class DailySafeToSpend(
    val dailyAllowance: Long,          // Safe amount to spend per day for remainder of cycle
    val remainingDaysInCycle: Int,      // Days left in current month / billing cycle
    val remainingBudget: Long,          // Available unspent budget
    val committedUpcomingBills: Long,   // Pending installments/bills due before cycle end
    val netDiscretionaryRemaining: Long,// (Remaining Budget - Committed Upcoming Bills)
    val pacingPcnt: Float,              // Current spending pace % (1.0 = on track, >1.0 = fast)
    val currencyCode: String
)

@Serializable
data class InstallmentHorizonMilestone(
    val monthsAhead: Int,               // e.g. 1, 3, 6, 12, 24
    val targetDateEpochMs: Long,
    val monthlyPaymentDue: Long,        // Active monthly obligation at this future date
    val monthlyCashFlowFreed: Long,     // Monthly savings liberated compared to today
    val remainingActivePlansCount: Int
)

@Serializable
data class InstallmentHorizonRoadmap(
    val currentMonthlyCommitment: Long,
    val totalOutstandingBalance: Long,
    val milestones: List<InstallmentHorizonMilestone>,
    val fullyPaidDateEpochMs: Long?,
    val currencyCode: String
)
