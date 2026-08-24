package com.sans.finance.data.repository

import android.content.Context
import com.sans.finance.data.local.dao.BudgetDao
import com.sans.finance.data.local.entity.BudgetEntity
import com.sans.finance.domain.repository.BudgetRepository
import com.sans.finance.presentation.widget.FinancialSummaryWidgetProvider
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class BudgetRepositoryImpl @Inject constructor(
    private val budgetDao: BudgetDao,
    private val context: Context? = null
) : BudgetRepository {
    override fun getAllBudgets(): Flow<List<BudgetEntity>> = budgetDao.getAllBudgets()
    override suspend fun insertBudget(budget: BudgetEntity): Long {
        val id = budgetDao.insertBudget(budget)
        context?.let { FinancialSummaryWidgetProvider.updateAllWidgets(it) }
        return id
    }
    override suspend fun updateBudget(budget: BudgetEntity) {
        budgetDao.updateBudget(budget)
        context?.let { FinancialSummaryWidgetProvider.updateAllWidgets(it) }
    }
    override suspend fun deleteBudget(budget: BudgetEntity) {
        budgetDao.deleteBudget(budget)
        context?.let { FinancialSummaryWidgetProvider.updateAllWidgets(it) }
    }
}
