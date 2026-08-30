package com.sans.finance.domain.usecase

import com.sans.finance.data.local.dao.SnapshotTotal
import com.sans.finance.domain.model.BenchmarkType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class GetPortfolioBenchmarkComparisonUseCaseTest {

    private val useCase = GetPortfolioBenchmarkComparisonUseCase()

    @Test
    fun testEmptyHistoryReturnsDefaults() {
        val result = useCase(emptyList(), BenchmarkType.SP500)
        assertEquals(0.0, result.alphaPct, 0.001)
        assertEquals(0, result.durationDays)
        assertTrue(result.trajectory.isEmpty())
    }

    @Test
    fun testOneYearOutperformingBenchmark() {
        val now = 1700000000000L
        val oneYearLater = now + (365L * 24 * 60 * 60 * 1000)

        // Portfolio grew 20% in 1 year (100M -> 120M)
        val history = listOf(
            SnapshotTotal(snapshot_date = now, totalIdr = 100_000_000.0, totalUsd = 6500.0),
            SnapshotTotal(snapshot_date = oneYearLater, totalIdr = 120_000_000.0, totalUsd = 7800.0)
        )

        val result = useCase(history, BenchmarkType.SP500)

        // S&P 500 is 10.5% CAGR, portfolio grew 20% -> Alpha ~ 9.5%
        assertEquals(20.0, result.portfolioTotalReturnPct, 0.5)
        assertEquals(10.5, result.benchmarkTotalReturnPct, 0.5)
        assertEquals(9.5, result.alphaPct, 0.5)
        assertTrue(result.isOutperforming)
        assertEquals(2, result.trajectory.size)
        assertEquals(100.0, result.trajectory.first().portfolioIndex, 0.01)
        assertEquals(100.0, result.trajectory.first().benchmarkIndex, 0.01)
        assertEquals(120.0, result.trajectory.last().portfolioIndex, 0.5)
    }

    @Test
    fun testUnderperformingBenchmark() {
        val now = 1700000000000L
        val oneYearLater = now + (365L * 24 * 60 * 60 * 1000)

        // Portfolio grew only 5% in 1 year
        val history = listOf(
            SnapshotTotal(snapshot_date = now, totalIdr = 100_000_000.0, totalUsd = 6500.0),
            SnapshotTotal(snapshot_date = oneYearLater, totalIdr = 105_000_000.0, totalUsd = 6800.0)
        )

        val result = useCase(history, BenchmarkType.SP500)

        assertTrue(result.alphaPct < 0.0)
        assertFalse(result.isOutperforming)
    }
}
