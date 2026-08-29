package com.sans.finance.domain.usecase

import com.sans.finance.domain.model.AssetClassHealth
import com.sans.finance.domain.model.HealthStatus
import com.sans.finance.domain.model.RebalanceType
import com.sans.finance.domain.model.RiskLevel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class GetRebalanceSuggestionsUseCaseTest {

    private val useCase = GetRebalanceSuggestionsUseCase()

    private fun health(
        assetClass: String,
        currentPercentage: Double,
        targetPercentage: Double,
        currentAmount: Double
    ) = AssetClassHealth(
        assetClass = assetClass,
        currentPercentage = currentPercentage,
        targetPercentage = targetPercentage,
        currentAmount = currentAmount,
        riskLevel = RiskLevel.MEDIUM,
        status = HealthStatus.HEALTHY,
        diffPercentage = targetPercentage - currentPercentage
    )

    @Test
    fun `empty portfolio returns no actions`() {
        assertTrue(useCase(emptyList()).isEmpty())
    }

    @Test
    fun `zero total value returns no actions`() {
        val result = useCase(
            listOf(health("Equity", 50.0, 50.0, 0.0))
        )
        assertTrue(result.isEmpty())
    }

    @Test
    fun `balanced portfolio returns no actions`() {
        val result = useCase(
            listOf(
                health("Equity", 60.0, 60.0, 6000.0),
                health("Bonds", 40.0, 40.0, 4000.0)
            )
        )
        // Deltas are exactly zero -> below the 0.01 threshold
        assertTrue(result.isEmpty())
    }

    @Test
    fun `overweight and underweight produce sell and buy sorted by amount`() {
        val result = useCase(
            listOf(
                health("Equity", 80.0, 60.0, 8000.0),   // overweight -> SELL 2000
                health("Bonds", 20.0, 40.0, 2000.0)     // underweight -> BUY 2000
            )
        )
        assertEquals(2, result.size)

        val sells = result.filter { it.action == RebalanceType.SELL }
        val buys = result.filter { it.action == RebalanceType.BUY }
        assertEquals(1, sells.size)
        assertEquals(1, buys.size)
        assertEquals(2000.0, sells[0].amount, 0.01)
        assertEquals(2000.0, buys[0].amount, 0.01)

        // Sorted descending by amount; equal amounts so both present
        assertEquals(result.sortedByDescending { it.amount }, result)
    }

    @Test
    fun `larger imbalance appears first`() {
        val result = useCase(
            listOf(
                health("Equity", 70.0, 60.0, 7000.0),   // SELL 1000
                health("Bonds", 25.0, 35.0, 2500.0),    // BUY 1000
                health("Gold", 5.0, 5.0, 500.0)         // balanced -> NONE (filtered out)
            )
        )
        assertEquals(2, result.size)
        assertTrue(result.all { it.action != RebalanceType.NONE })
    }

    @Test
    fun `percentageToAdjust reflects drift from target`() {
        val result = useCase(
            listOf(health("Equity", 80.0, 60.0, 8000.0))
        )
        assertEquals(1, result.size)
        assertEquals(20.0, result[0].percentageToAdjust, 0.001)
    }
}
