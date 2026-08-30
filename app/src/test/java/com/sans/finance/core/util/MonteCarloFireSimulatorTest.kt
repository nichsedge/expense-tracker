package com.sans.finance.core.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MonteCarloFireSimulatorTest {

    @Test
    fun testSimulationProducesValidTrajectories() {
        val result = MonteCarloFireSimulator.simulate(
            initialWealth = 100_000_000L,
            annualSavings = 60_000_000L,
            meanReturn = 0.08,
            volatility = 0.15,
            inflation = 0.025,
            years = 20,
            fireTarget = 1_500_000_000L,
            iterations = 500
        )

        assertEquals(21, result.p10Projections.size)
        assertEquals(21, result.p50Projections.size)
        assertEquals(21, result.p90Projections.size)

        // Initial wealth should match year 0
        assertEquals(100_000_000L, result.p10Projections[0])
        assertEquals(100_000_000L, result.p50Projections[0])
        assertEquals(100_000_000L, result.p90Projections[0])

        // Final year percentiles should follow monotonic order: P10 <= P50 <= P90
        val lastYear = 20
        assertTrue(result.p10Projections[lastYear] <= result.p50Projections[lastYear])
        assertTrue(result.p50Projections[lastYear] <= result.p90Projections[lastYear])

        // Success rate should be within [0.0, 1.0]
        assertTrue(result.successRate in 0.0f..1.0f)
    }

    @Test
    fun testSimulationWithZeroYears() {
        val result = MonteCarloFireSimulator.simulate(
            initialWealth = 500_000_000L,
            annualSavings = 0L,
            meanReturn = 0.07,
            volatility = 0.15,
            inflation = 0.02,
            years = 0,
            fireTarget = 1_000_000_000L,
            iterations = 100
        )

        assertEquals(1, result.p50Projections.size)
        assertEquals(500_000_000L, result.p50Projections[0])
        assertEquals(0f, result.successRate)
    }
}
