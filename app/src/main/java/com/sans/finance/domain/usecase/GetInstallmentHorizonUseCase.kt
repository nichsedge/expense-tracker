package com.sans.finance.domain.usecase

import com.sans.finance.data.local.dao.InstallmentDao
import com.sans.finance.data.util.LocaleManager
import com.sans.finance.domain.model.InstallmentHorizonMilestone
import com.sans.finance.domain.model.InstallmentHorizonRoadmap
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import java.util.Calendar
import javax.inject.Inject

class GetInstallmentHorizonUseCase @Inject constructor(
    private val installmentDao: InstallmentDao,
    private val localeManager: LocaleManager
) {
    operator fun invoke(): Flow<InstallmentHorizonRoadmap> {
        val baseCurrency = localeManager.getCurrency()

        return combine(
            installmentDao.getActiveInstallments(),
            installmentDao.getAllPendingItems()
        ) { activeList, pendingItems ->
            val pendingByInstallment = pendingItems.groupBy { it.installmentId }
            var currentMonthlyCommitment = 0L
            val totalOutstanding = pendingItems.sumOf { it.amount }
            val maxDueEpoch = pendingItems.maxOfOrNull { it.dueDate }

            activeList.forEach { instWithExp ->
                val instItems = pendingByInstallment[instWithExp.installment.id] ?: emptyList()
                if (instItems.isNotEmpty()) {
                    currentMonthlyCommitment += (instItems.firstOrNull()?.amount ?: 0L)
                }
            }

            val horizonMonths = listOf(1, 3, 6, 12, 24)
            val milestones = horizonMonths.map { monthsAhead ->
                val targetCal = Calendar.getInstance().apply {
                    add(Calendar.MONTH, monthsAhead)
                    set(Calendar.DAY_OF_MONTH, 1)
                    set(Calendar.HOUR_OF_DAY, 0)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                }
                val targetEpochMs = targetCal.timeInMillis

                var futureMonthlyCommitment = 0L
                var activeCount = 0

                activeList.forEach { instWithExp ->
                    val instItems = pendingByInstallment[instWithExp.installment.id] ?: emptyList()
                    val hasPendingAfter = instItems.any { it.dueDate >= targetEpochMs }
                    if (hasPendingAfter) {
                        futureMonthlyCommitment += (instItems.firstOrNull()?.amount ?: 0L)
                        activeCount++
                    }
                }

                val freedCashFlow = (currentMonthlyCommitment - futureMonthlyCommitment).coerceAtLeast(0L)

                InstallmentHorizonMilestone(
                    monthsAhead = monthsAhead,
                    targetDateEpochMs = targetEpochMs,
                    monthlyPaymentDue = futureMonthlyCommitment,
                    monthlyCashFlowFreed = freedCashFlow,
                    remainingActivePlansCount = activeCount
                )
            }

            InstallmentHorizonRoadmap(
                currentMonthlyCommitment = currentMonthlyCommitment,
                totalOutstandingBalance = totalOutstanding,
                milestones = milestones,
                fullyPaidDateEpochMs = maxDueEpoch,
                currencyCode = baseCurrency
            )
        }
    }
}
