package com.sans.finance.domain.usecase

import com.sans.finance.domain.model.AssetClassHealth
import kotlinx.serialization.Serializable
import javax.inject.Inject
import kotlin.math.max

@Serializable
data class CashInjectionAllocation(
    val assetClass: String,
    val currentAmount: Double,
    val targetPercentage: Double,
    val allocatedDeposit: Double,
    val projectedAmount: Double,
    val projectedPercentage: Double
)

@Serializable
data class CashInjectionRebalanceResult(
    val totalDeposit: Double,
    val allocations: List<CashInjectionAllocation>,
    val trackingErrorBefore: Double,
    val trackingErrorAfter: Double
)

class GetCashInjectionRebalanceUseCase @Inject constructor() {

    operator fun invoke(
        healthList: List<AssetClassHealth>,
        depositAmount: Double
    ): CashInjectionRebalanceResult {
        if (healthList.isEmpty() || depositAmount <= 0) {
            return CashInjectionRebalanceResult(
                totalDeposit = 0.0,
                allocations = emptyList(),
                trackingErrorBefore = 0.0,
                trackingErrorAfter = 0.0
            )
        }

        val currentTotal = healthList.sumOf { it.currentAmount }
        val newTotal = currentTotal + depositAmount

        // 1. Calculate deficit for each asset class
        val deficits = healthList.associateWith { health ->
            val targetAmount = newTotal * (health.targetPercentage / 100.0)
            max(0.0, targetAmount - health.currentAmount)
        }

        val totalDeficit = deficits.values.sum()
        val allocationMap = mutableMapOf<String, Double>()

        if (totalDeficit > 0) {
            if (totalDeficit >= depositAmount) {
                // Scale deficits to fit depositAmount
                deficits.forEach { (health, deficit) ->
                    val allocated = (deficit / totalDeficit) * depositAmount
                    allocationMap[health.assetClass] = allocated
                }
            } else {
                // Fulfill all deficits first, then allocate remainder by target percentage
                val remainder = depositAmount - totalDeficit
                deficits.forEach { (health, deficit) ->
                    val extra = remainder * (health.targetPercentage / 100.0)
                    allocationMap[health.assetClass] = deficit + extra
                }
            }
        } else {
            // Allocate directly according to target weights
            healthList.forEach { health ->
                allocationMap[health.assetClass] = depositAmount * (health.targetPercentage / 100.0)
            }
        }

        // Build result allocations
        val allocations = healthList.map { health ->
            val allocated = allocationMap[health.assetClass] ?: 0.0
            val projectedAmount = health.currentAmount + allocated
            val projectedPercentage = if (newTotal > 0) (projectedAmount / newTotal) * 100.0 else 0.0

            CashInjectionAllocation(
                assetClass = health.assetClass,
                currentAmount = health.currentAmount,
                targetPercentage = health.targetPercentage,
                allocatedDeposit = allocated,
                projectedAmount = projectedAmount,
                projectedPercentage = projectedPercentage
            )
        }

        // Tracking error calculation (Sum of squared differences from target percentage)
        val trackingErrorBefore = healthList.sumOf {
            kotlin.math.abs(it.currentPercentage - it.targetPercentage)
        }
        val trackingErrorAfter = allocations.sumOf {
            kotlin.math.abs(it.projectedPercentage - it.targetPercentage)
        }

        return CashInjectionRebalanceResult(
            totalDeposit = depositAmount,
            allocations = allocations.sortedByDescending { it.allocatedDeposit },
            trackingErrorBefore = trackingErrorBefore,
            trackingErrorAfter = trackingErrorAfter
        )
    }
}
