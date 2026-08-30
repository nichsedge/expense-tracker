package com.sans.finance.domain.usecase

import com.sans.finance.domain.model.AssetClassHealth
import com.sans.finance.domain.model.HealthStatus
import com.sans.finance.domain.model.RiskLevel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class GetCashInjectionRebalanceUseCaseTest {

    private val useCase = GetCashInjectionRebalanceUseCase()

    @Test
    fun testCashInjectionAllocatesToUnderweightAssets() {
        val healthList = listOf(
            AssetClassHealth(
                assetClass = "Equity",
                currentAmount = 70_000.0,
                currentPercentage = 70.0,
                targetPercentage = 50.0,
                diffPercentage = 20.0,
                status = HealthStatus.OVERWEIGHT,
                riskLevel = RiskLevel.HIGH
            ),
            AssetClassHealth(
                assetClass = "Bonds",
                currentAmount = 30_000.0,
                currentPercentage = 30.0,
                targetPercentage = 50.0,
                diffPercentage = -20.0,
                status = HealthStatus.UNDERWEIGHT,
                riskLevel = RiskLevel.LOW
            )
        )

        // Inject 40,000 fresh cash. Total becomes 140,000.
        // Target: Equity 70,000, Bonds 70,000.
        // Current: Equity 70,000, Bonds 30,000 -> Bonds needs 40,000!
        val result = useCase(healthList, 40_000.0)

        assertEquals(40_000.0, result.totalDeposit, 0.01)
        val bondAlloc = result.allocations.find { it.assetClass == "Bonds" }
        val equityAlloc = result.allocations.find { it.assetClass == "Equity" }

        assertEquals(40_000.0, bondAlloc?.allocatedDeposit ?: 0.0, 0.01)
        assertEquals(0.0, equityAlloc?.allocatedDeposit ?: 0.0, 0.01)
        assertEquals(50.0, bondAlloc?.projectedPercentage ?: 0.0, 0.01)
        assertEquals(50.0, equityAlloc?.projectedPercentage ?: 0.0, 0.01)

        // Tracking error after rebalancing should be near 0
        assertTrue(result.trackingErrorAfter < result.trackingErrorBefore)
    }

    @Test
    fun testCashInjectionWithZeroDeposit() {
        val result = useCase(emptyList(), 0.0)
        assertEquals(0.0, result.totalDeposit, 0.01)
        assertTrue(result.allocations.isEmpty())
    }
}
