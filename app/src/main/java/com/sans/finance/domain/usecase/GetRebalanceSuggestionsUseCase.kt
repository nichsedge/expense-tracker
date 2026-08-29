package com.sans.finance.domain.usecase

import com.sans.finance.domain.model.AssetClassHealth
import com.sans.finance.domain.model.RebalanceAction
import com.sans.finance.domain.model.RebalanceType
import kotlin.math.abs

class GetRebalanceSuggestionsUseCase {
    operator fun invoke(healthList: List<AssetClassHealth>): List<RebalanceAction> {
        val totalValue = healthList.sumOf { it.currentAmount }
        if (totalValue <= 0) return emptyList()

        return healthList.map { health ->
            val targetAmount = totalValue * (health.targetPercentage / 100.0)
            val delta = targetAmount - health.currentAmount
            
            val action = when {
                delta > 0.01 -> RebalanceType.BUY
                delta < -0.01 -> RebalanceType.SELL
                else -> RebalanceType.NONE
            }

            RebalanceAction(
                assetClass = health.assetClass,
                action = action,
                amount = abs(delta),
                percentageToAdjust = abs(health.targetPercentage - health.currentPercentage)
            )
        }.filter { it.action != RebalanceType.NONE }
        .sortedByDescending { it.amount }
    }
}
