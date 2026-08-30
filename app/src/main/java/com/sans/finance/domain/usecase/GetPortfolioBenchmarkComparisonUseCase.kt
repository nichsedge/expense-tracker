package com.sans.finance.domain.usecase

import com.sans.finance.data.local.dao.SnapshotTotal
import com.sans.finance.domain.model.BenchmarkPoint
import com.sans.finance.domain.model.BenchmarkType
import com.sans.finance.domain.model.PortfolioBenchmarkComparison
import javax.inject.Inject
import kotlin.math.pow

class GetPortfolioBenchmarkComparisonUseCase @Inject constructor() {

    operator fun invoke(
        history: List<SnapshotTotal>,
        benchmarkType: BenchmarkType = BenchmarkType.SP500
    ): PortfolioBenchmarkComparison {
        if (history.isEmpty()) {
            return PortfolioBenchmarkComparison(
                benchmarkType = benchmarkType,
                portfolioTotalReturnPct = 0.0,
                benchmarkTotalReturnPct = 0.0,
                alphaPct = 0.0,
                isOutperforming = true,
                durationDays = 0,
                trajectory = emptyList()
            )
        }

        val sortedHistory = history.sortedBy { it.snapshot_date }
        val firstPoint = sortedHistory.first()
        val lastPoint = sortedHistory.last()
        val firstVal = firstPoint.totalIdr.coerceAtLeast(1.0)
        val lastVal = lastPoint.totalIdr.coerceAtLeast(0.0)

        val durationDays = ((lastPoint.snapshot_date - firstPoint.snapshot_date) / (1000.0 * 60 * 60 * 24)).toInt().coerceAtLeast(0)

        val trajectory = sortedHistory.map { point ->
            val daysElapsed = ((point.snapshot_date - firstPoint.snapshot_date) / (1000.0 * 60 * 60 * 24)).coerceAtLeast(0.0)
            val yearsElapsed = daysElapsed / 365.25
            val benchmarkIndex = 100.0 * (1.0 + benchmarkType.annualCagr).pow(yearsElapsed)
            val portfolioIndex = if (firstVal > 0) (point.totalIdr / firstVal) * 100.0 else 100.0

            BenchmarkPoint(
                dateEpochMs = point.snapshot_date,
                portfolioIndex = portfolioIndex,
                benchmarkIndex = benchmarkIndex,
                portfolioValueInBase = point.totalIdr
            )
        }

        val portfolioTotalReturnPct = if (firstVal > 0) {
            ((lastVal - firstVal) / firstVal) * 100.0
        } else 0.0

        val lastBenchmarkIndex = trajectory.lastOrNull()?.benchmarkIndex ?: 100.0
        val benchmarkTotalReturnPct = lastBenchmarkIndex - 100.0
        val alphaPct = portfolioTotalReturnPct - benchmarkTotalReturnPct

        return PortfolioBenchmarkComparison(
            benchmarkType = benchmarkType,
            portfolioTotalReturnPct = portfolioTotalReturnPct,
            benchmarkTotalReturnPct = benchmarkTotalReturnPct,
            alphaPct = alphaPct,
            isOutperforming = alphaPct >= 0.0,
            durationDays = durationDays,
            trajectory = trajectory
        )
    }
}
