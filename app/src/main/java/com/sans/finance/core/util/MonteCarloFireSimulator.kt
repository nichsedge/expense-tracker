package com.sans.finance.core.util

import java.util.Random
import kotlin.math.exp
import kotlin.math.ln
import kotlin.math.pow
import kotlin.math.sqrt

data class MonteCarloResult(
    val p10Projections: List<Long>,  // Bear / Pessimistic case (10th percentile)
    val p50Projections: List<Long>,  // Median / Base case (50th percentile)
    val p90Projections: List<Long>,  // Bull / Optimistic case (90th percentile)
    val successRate: Float,          // % of simulations that reach or maintain target
    val iterations: Int = 1000
)

object MonteCarloFireSimulator {

    /**
     * Runs stochastic Monte Carlo simulations using geometric Brownian motion for asset returns.
     *
     * @param initialWealth Initial net worth in cents
     * @param annualSavings Net annual contribution (savings) in cents
     * @param meanReturn Expected annual arithmetic return (e.g. 0.07 for 7%)
     * @param volatility Annual standard deviation / volatility (e.g. 0.15 for 15%)
     * @param inflation Expected annual inflation rate (e.g. 0.025 for 2.5%)
     * @param years Number of projection years
     * @param fireTarget Target FIRE number in cents
     * @param iterations Number of simulation runs (default 1,000)
     */
    fun simulate(
        initialWealth: Long,
        annualSavings: Long,
        meanReturn: Double = 0.07,
        volatility: Double = 0.15,
        inflation: Double = 0.025,
        years: Int = 20,
        fireTarget: Long = 0L,
        iterations: Int = 1000,
        seed: Long = 42L
    ): MonteCarloResult {
        if (years <= 0 || initialWealth < 0) {
            val initialList = if (initialWealth >= 0) listOf(initialWealth) else emptyList()
            val rate = if (fireTarget > 0 && initialWealth >= fireTarget) 1f else 0f
            return MonteCarloResult(initialList, initialList, initialList, rate, iterations)
        }

        val random = Random(seed)
        val realMean = (1.0 + meanReturn) / (1.0 + inflation) - 1.0
        val drift = realMean - 0.5 * volatility.pow(2.0)

        // Matrix of trajectories: [iterations][years + 1]
        val simulationMatrix = Array(iterations) { DoubleArray(years + 1) }
        var successCount = 0

        for (i in 0 until iterations) {
            simulationMatrix[i][0] = initialWealth.toDouble()
            var currentWealth = initialWealth.toDouble()
            var reachedTarget = initialWealth >= fireTarget && fireTarget > 0

            for (y in 1..years) {
                // Standard Normal Z using Box-Muller transform
                val u1 = random.nextDouble().coerceAtLeast(1e-10)
                val u2 = random.nextDouble()
                val z = sqrt(-2.0 * ln(u1)) * kotlin.math.cos(2.0 * Math.PI * u2)

                // Annual growth factor
                val annualReturnFactor = exp(drift + volatility * z)
                currentWealth = (currentWealth + annualSavings) * annualReturnFactor
                if (currentWealth < 0) currentWealth = 0.0

                simulationMatrix[i][y] = currentWealth
                if (fireTarget > 0 && currentWealth >= fireTarget) {
                    reachedTarget = true
                }
            }

            if (reachedTarget || (fireTarget <= 0 && currentWealth > initialWealth)) {
                successCount++
            }
        }

        // Calculate percentiles per year
        val p10List = mutableListOf<Long>()
        val p50List = mutableListOf<Long>()
        val p90List = mutableListOf<Long>()

        for (y in 0..years) {
            val yearValues = DoubleArray(iterations) { i -> simulationMatrix[i][y] }
            yearValues.sort()

            val p10Idx = (iterations * 0.10).toInt().coerceIn(0, iterations - 1)
            val p50Idx = (iterations * 0.50).toInt().coerceIn(0, iterations - 1)
            val p90Idx = (iterations * 0.90).toInt().coerceIn(0, iterations - 1)

            p10List.add(yearValues[p10Idx].toLong().coerceAtLeast(0L))
            p50List.add(yearValues[p50Idx].toLong().coerceAtLeast(0L))
            p90List.add(yearValues[p90Idx].toLong().coerceAtLeast(0L))
        }

        val successRate = (successCount.toFloat() / iterations.toFloat()).coerceIn(0f, 1f)

        return MonteCarloResult(
            p10Projections = p10List,
            p50Projections = p50List,
            p90Projections = p90List,
            successRate = successRate,
            iterations = iterations
        )
    }
}
