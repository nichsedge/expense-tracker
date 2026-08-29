package com.sans.finance.data.repository

import androidx.room.withTransaction
import com.sans.finance.domain.model.AccountSyncDryRunResult
import com.sans.finance.domain.model.Category
import com.sans.finance.domain.model.CategorySpent
import com.sans.finance.domain.model.DaySpent
import com.sans.finance.domain.model.Expense
import com.sans.finance.domain.model.ReSyncMode
import com.sans.finance.domain.model.Tag
import com.sans.finance.domain.repository.ExpenseRepository
import com.sans.finance.presentation.widget.FinancialSummaryWidgetProvider
import com.sans.finance.presentation.widget.QuickAddWidgetProvider
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

class ExpenseRepositoryImpl(
    private val db: com.sans.finance.data.local.AppDatabase,
    private val dao: com.sans.finance.data.local.dao.ExpenseDao,
    private val tagDao: com.sans.finance.data.local.dao.TagDao,
    private val categoryDao: com.sans.finance.data.local.dao.CategoryDao,
    private val installmentDao: com.sans.finance.data.local.dao.InstallmentDao,
    private val accountDao: com.sans.finance.data.local.dao.AccountDao,
    private val context: android.content.Context? = null
) : ExpenseRepository {

    private val widgetScope = kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.Default + kotlinx.coroutines.SupervisorJob())
    private var widgetDebounceJob: kotlinx.coroutines.Job? = null

    private fun notifyWidgets() {
        context?.let { ctx ->
            widgetDebounceJob?.cancel()
            widgetDebounceJob = widgetScope.launch {
                kotlinx.coroutines.delay(200)
                QuickAddWidgetProvider.updateAllWidgets(ctx)
                FinancialSummaryWidgetProvider.updateAllWidgets(ctx)
                com.sans.finance.presentation.util.DynamicShortcutManager.updateShortcuts(ctx)
            }
        }
    }

    init {
        widgetScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            dao.deleteSyntheticDuplicateExpenses(INSTALLMENT_PAYMENT_ID_OFFSET)
        }
    }

    companion object {
        private const val INSTALLMENT_PAYMENT_ID_OFFSET = Expense.SYNTHETIC_INSTALLMENT_OFFSET
    }

    override fun getAllExpenses(): Flow<List<Expense>> {
        return combine(
            dao.getAllExpenses(),
            installmentDao.getInstallmentPaymentsBetween(0, Long.MAX_VALUE),
            installmentDao.getAllInstallmentItems()
        ) { expenseEntities, installmentRows, items ->
            val itemsByInstallment = items.groupBy { it.installmentId }
            val expenses = expenseEntities.map { it.toDomain(itemsByInstallment) }
            val installmentPayments = installmentRows.map { it.toDomain() }
            (expenses + installmentPayments).sortedByDescending { it.date }
        }
    }

    override fun getExpensesBetween(since: Long, until: Long): Flow<List<Expense>> {
        return combine(
            dao.getExpensesBetween(since, until),
            installmentDao.getInstallmentPaymentsBetween(since, until),
            installmentDao.getAllInstallmentItems()
        ) { expenseEntities, installmentRows, items ->
            val itemsByInstallment = items.groupBy { it.installmentId }
            val expenses = expenseEntities.map { it.toDomain(itemsByInstallment) }
            val installmentPayments = installmentRows.map { it.toDomain() }
            (expenses + installmentPayments).sortedByDescending { it.date }
        }
    }

    override fun getRecurringExpenses(): Flow<List<Expense>> {
        return combine(
            dao.getRecurringExpenses(),
            installmentDao.getAllInstallmentItems()
        ) { entities, items ->
            val itemsByInstallment = items.groupBy { it.installmentId }
            entities.map { it.toDomain(itemsByInstallment) }
        }
    }

    override fun getFilteredExpenses(
        query: String?,
        categoryIds: List<Long>,
        accountIds: List<Long>,
        since: Long,
        until: Long,
        minAmount: Long?,
        maxAmount: Long?,
        tags: List<String>,
        types: List<String>
    ): Flow<List<Expense>> {
        val searchQuery = if (query.isNullOrBlank()) null else query

        val expensesFlow = dao.getFilteredExpenses(
            searchQuery,
            categoryIds,
            categoryIds.size,
            accountIds,
            accountIds.size,
            since,
            until,
            minAmount,
            maxAmount,
            tags,
            tags.size,
            types,
            types.size
        )

        val installmentsFlow = installmentDao.getFilteredInstallmentPayments(
            since,
            until,
            searchQuery,
            categoryIds,
            categoryIds.size,
            accountIds,
            accountIds.size,
            minAmount,
            maxAmount,
            tags,
            tags.size,
            types,
            types.size
        )

        return combine(
            expensesFlow,
            installmentsFlow,
            installmentDao.getAllInstallmentItems()
        ) { expenseEntities, installmentRows, items ->
            val itemsByInstallment = items.groupBy { it.installmentId }
            val expenses = expenseEntities.map { it.toDomain(itemsByInstallment) }
            val installmentPayments = installmentRows.map { it.toDomain() }
            (expenses + installmentPayments).sortedByDescending { it.date }
        }
    }

    override suspend fun getExpenseById(id: Long): Expense? {
        return if (id >= INSTALLMENT_PAYMENT_ID_OFFSET) {
            val installmentItemId = id - INSTALLMENT_PAYMENT_ID_OFFSET
            installmentDao.getInstallmentItemById(installmentItemId)?.let { item ->
                val installment = installmentDao.getInstallmentById(item.installmentId)
                val parentExpense = installment?.let { dao.getExpenseById(it.expenseId) }

                Expense(
                    id = id,
                    date = item.dueDate,
                    title = parentExpense?.expense?.title ?: "Installment",
                    amount = item.amount,
                    categoryId = parentExpense?.expense?.categoryId ?: 1L,
                    isInstallmentPayment = true,
                    installmentMonth = item.monthNumber,
                    installmentTotalMonths = installment?.durationMonths ?: 0,
                    status = item.status,
                    details = parentExpense?.expense?.details,
                    accountId = parentExpense?.expense?.accountId ?: 1L,
                    currency = parentExpense?.expense?.currency ?: "USD",
                    tags = parentExpense?.tags?.map { it.name } ?: emptyList(),
                    categoryName = parentExpense?.category?.name,
                    categoryIcon = parentExpense?.category?.icon
                )
            }
        } else {
            dao.getExpenseById(id)?.let {
                val items = installmentDao.getItemsByInstallmentIdForId(it.installment?.id ?: -1)
                it.toDomain(items.groupBy { it.installmentId })
            }
        }
    }

    override suspend fun getTitleSuggestions(query: String): List<String> {
        return dao.getTitleSuggestions(query)
    }

    override suspend fun getTopFrequentTitles(limit: Int): List<String> {
        return dao.getTopFrequentTitles(limit)
    }

    override suspend fun getTopFrequentTitlesByDay(dayOfWeek: Int, limit: Int): List<String> {
        return dao.getTopFrequentTitlesByDay(dayOfWeek.toString(), limit)
    }

    override suspend fun getDetailsSuggestions(query: String): List<String> {
        return dao.getDetailsSuggestions(query)
    }

    override suspend fun getPredictionForTitle(title: String): Expense? {
        return dao.getLastExpenseByTitle(title)?.let {
            val items = installmentDao.getItemsByInstallmentIdForId(it.installment?.id ?: -1)
            it.toDomain(items.groupBy { it.installmentId })
        }
    }

    override suspend fun findPotentialDuplicate(
        title: String,
        amount: Long,
        date: Long,
        accountId: Long
    ): Expense? {
        val window = 5 * 60 * 1000 // 5 minutes
        return dao.findDuplicateExpense(
            title = title,
            amount = amount,
            startTime = date - window,
            endTime = date + window,
            accountId = accountId
        )?.let {
            val items = installmentDao.getItemsByInstallmentIdForId(it.installment?.id ?: -1)
            it.toDomain(items.groupBy { it.installmentId })
        }
    }

    override suspend fun insertExpense(expense: Expense): Long {
        val expenseToInsert = if (expense.id >= INSTALLMENT_PAYMENT_ID_OFFSET) {
            expense.copy(id = 0)
        } else {
            expense
        }
        val expenseId = dao.insertExpense(expenseToInsert.toEntity())
        notifyWidgets()
        return expenseId
    }

    override suspend fun updateExpense(expense: Expense) {
        if (expense.id >= INSTALLMENT_PAYMENT_ID_OFFSET) {
            val itemId = expense.id - INSTALLMENT_PAYMENT_ID_OFFSET
            val oldItem = installmentDao.getInstallmentItemById(itemId)
            if (oldItem != null) {
                installmentDao.insertInstallmentItem(
                    oldItem.copy(
                        amount = expense.amount,
                        dueDate = expense.date,
                        status = expense.status
                    )
                )
                // Update parent installment status
                val installment = installmentDao.getInstallmentById(oldItem.installmentId)
                if (installment != null) {
                    installmentDao.updateInstallment(
                        installment.copy(
                            status = if (installmentDao.getPendingItemsCount(installment.id) == 0) "Completed" else "Active"
                        )
                    )
                }
            }
        } else {
            dao.updateExpense(expense.toEntity())
        }
        notifyWidgets()
    }

    override suspend fun deleteExpense(expense: Expense) {
        if (expense.id >= INSTALLMENT_PAYMENT_ID_OFFSET) {
            val itemId = expense.id - INSTALLMENT_PAYMENT_ID_OFFSET
            installmentDao.getInstallmentItemById(itemId)?.let { item ->
                installmentDao.updateInstallmentItemStatus(itemId, "Pending")
                val installment = installmentDao.getInstallmentById(item.installmentId)
                if (installment != null) {
                    installmentDao.updateInstallment(
                        installment.copy(status = "Active")
                    )
                }
            }
        } else {
            dao.deleteExpense(expense.toEntity())
        }
        notifyWidgets()
    }

    override fun getTotalSpentSince(since: Long): Flow<Long?> {
        return dao.getTotalSpentSince(since)
    }

    override fun getTotalSpentBetween(since: Long, until: Long): Flow<Long?> {
        return dao.getTotalSpentBetween(since, until)
    }

    override fun getAllTimeSpent(): Flow<Long?> {
        return dao.getAllTimeSpent()
    }

    override fun getOldestExpenseDate(): Flow<Long?> {
        return dao.getOldestExpenseDate()
    }

    override suspend fun getReSyncBalancesDryRun(): List<AccountSyncDryRunResult> {
        val accounts = accountDao.getAllAccounts().first()
        val balances = mutableMapOf<Long, Long>()
        accounts.forEach { balances[it.id] = 0L }

        val expenses = dao.getAllExpenseEntities()
        expenses.forEach { exp ->
            if (exp.type == "TRANSFER") {
                balances[exp.accountId] = (balances[exp.accountId] ?: 0L) - exp.amount
                val toId = exp.toAccountId
                if (toId != null) {
                    balances[toId] = (balances[toId] ?: 0L) + exp.amount
                }
            } else if (exp.type == "INCOME") {
                balances[exp.accountId] = (balances[exp.accountId] ?: 0L) + exp.amount
            } else {
                balances[exp.accountId] = (balances[exp.accountId] ?: 0L) - exp.amount
            }
        }

        // Add installment payments
        val installmentItems =
            installmentDao.getInstallmentPaymentsBetween(0, Long.MAX_VALUE).first()
        installmentItems.forEach { item ->
            if (item.status == "Paid") {
                balances[item.accountId] = (balances[item.accountId] ?: 0L) - item.amount
            }
        }

        return accounts.map { account ->
            val calculated = balances[account.id] ?: 0L
            AccountSyncDryRunResult(
                accountId = account.id,
                accountName = account.name,
                currentBalance = account.balance,
                calculatedBalance = calculated,
                currency = account.currency
            )
        }
    }

    override suspend fun reSyncAccountBalances(mode: ReSyncMode, adjustmentDate: Long) {
        db.withTransaction {
            val accounts = accountDao.getAllAccounts().first()
            val dryRunResults = getReSyncBalancesDryRun()

            if (mode == ReSyncMode.TRANSACTIONS_AS_TRUTH) {
                accounts.forEach { account ->
                    val calc = dryRunResults.firstOrNull { it.accountId == account.id }?.calculatedBalance ?: 0L
                    if (account.balance != calc) {
                        accountDao.updateAccount(
                            account.copy(
                                balance = calc,
                                updatedAt = System.currentTimeMillis()
                            )
                        )
                    }
                }
            } else {
                val categories = categoryDao.getAllCategoriesSync()
                dryRunResults.forEach { result ->
                    if (result.isDifferenceExist) {
                        val delta = result.delta
                        val existingAdjustment = dao.getAllExpenseEntities().firstOrNull { exp ->
                            exp.accountId == result.accountId &&
                            exp.date == adjustmentDate &&
                            exp.title == "Balance Adjustment"
                        }

                        if (existingAdjustment != null) {
                            val existingEffect = if (existingAdjustment.type == "INCOME") existingAdjustment.amount else -existingAdjustment.amount
                            val newEffect = existingEffect - delta
                            if (newEffect == 0L) {
                                dao.deleteExpense(existingAdjustment)
                            } else {
                                val newType = if (newEffect > 0L) "INCOME" else "EXPENSE"
                                val newAmount = kotlin.math.abs(newEffect)
                                val targetCategory = categories.firstOrNull {
                                    it.type == newType && (it.name.contains("Misc", ignoreCase = true) || it.name.contains("Other", ignoreCase = true))
                                } ?: categories.firstOrNull { it.type == newType }

                                dao.updateExpense(
                                    existingAdjustment.copy(
                                        amount = newAmount,
                                        type = newType,
                                        categoryId = targetCategory?.id ?: 1L,
                                        updatedAt = System.currentTimeMillis()
                                    )
                                )
                            }
                        } else {
                            val isIncome = delta < 0L
                            val type = if (isIncome) "INCOME" else "EXPENSE"
                            val amount = kotlin.math.abs(delta)
                            val targetCategory = categories.firstOrNull {
                                it.type == type && (it.name.contains("Misc", ignoreCase = true) || it.name.contains("Other", ignoreCase = true))
                            } ?: categories.firstOrNull { it.type == type }

                            val expenseEntity = com.sans.finance.data.local.entity.ExpenseEntity(
                                date = adjustmentDate,
                                title = "Balance Adjustment",
                                details = "System generated to match actual account balance during re-sync.",
                                amount = amount,
                                categoryId = targetCategory?.id ?: 1L,
                                accountId = result.accountId,
                                type = type,
                                currency = result.currency,
                                status = "Paid",
                                isRecurring = false,
                                isInstallment = false
                            )
                            val newId = dao.insertExpense(expenseEntity)
                            // Note: Tag sync should be handled by TagRepository if needed,
                            // but for simplicity here we might keep a direct DAO call if it's internal re-sync
                            dao.insertExpenseTagCrossRefs(listOf(
                                com.sans.finance.data.local.entity.ExpenseTagCrossRef(newId, 1L) // Assuming tag 1 is Adjustment
                            ))
                        }
                    }
                }
            }
        }
    }

    override fun getSpendingByCategoryBetween(
        since: Long,
        until: Long
    ): Flow<List<CategorySpent>> {
        return dao.getSpendingByCategoryBetween(since, until)
    }

    override fun getBreakdownByCategoryBetween(
        since: Long,
        until: Long,
        type: String
    ): Flow<List<CategorySpent>> {
        return dao.getBreakdownByCategoryBetween(since, until, type)
    }

    override fun getTotalAmountByTypeBetween(
        since: Long,
        until: Long,
        type: String
    ): Flow<Long?> {
        return dao.getTotalAmountByTypeBetween(since, until, type)
    }

    override fun getDailySpendingBetween(
        since: Long,
        until: Long
    ): Flow<List<DaySpent>> {
        return dao.getDailySpendingBetween(since, until)
    }

    override fun getDailyBreakdownByCategoryBetween(
        since: Long,
        until: Long,
        categoryId: Long,
        type: String
    ): Flow<List<DaySpent>> {
        return dao.getDailyBreakdownByCategoryBetween(since, until, categoryId, type)
    }

    override fun getMonthlyBreakdownByCategory(
        categoryId: Long,
        type: String
    ): Flow<List<DaySpent>> {
        return dao.getMonthlyBreakdownByCategory(categoryId, type)
    }

    private fun com.sans.finance.data.local.entity.ExpenseWithTags.toDomain(
        itemsByInstallment: Map<Long, List<com.sans.finance.data.local.entity.InstallmentItemEntity>>? = null
    ): Expense {
        val items = installment?.id?.let { itemsByInstallment?.get(it) }
        val totalPaid = items?.filter { it.status == "Paid" }?.sumOf { it.amount } ?: 0L
        val remainingBalance = items?.filter { it.status == "Pending" }?.sumOf { it.amount } ?: 0L
        val totalAmount = items?.sumOf { it.amount } ?: 0L
        val inst = installment
        val monthlyPayment =
            if (inst != null && inst.durationMonths > 0) totalAmount / inst.durationMonths else 0L

        return Expense(
            id = expense.id,
            date = expense.date,
            title = expense.title,
            amount = expense.amount,
            categoryId = expense.categoryId,
            isRecurring = expense.isRecurring,
            isInstallment = expense.isInstallment,
            recurrenceInterval = expense.recurrenceInterval,
            nextDueDate = expense.nextDueDate,
            accountId = expense.accountId,
            toAccountId = expense.toAccountId,
            type = expense.type,
            details = expense.details,
            tags = tags.map { it.name },
            currency = expense.currency,
            totalPaid = totalPaid,
            remainingBalance = remainingBalance,
            monthlyPayment = monthlyPayment,
            categoryName = category?.name,
            categoryIcon = category?.icon
        )
    }

    private fun Expense.toEntity(): com.sans.finance.data.local.entity.ExpenseEntity {
        return com.sans.finance.data.local.entity.ExpenseEntity(
            id = id,
            date = date,
            title = title,
            amount = amount,
            categoryId = categoryId,
            isRecurring = isRecurring,
            isInstallment = isInstallment,
            recurrenceInterval = recurrenceInterval,
            nextDueDate = nextDueDate,
            accountId = accountId,
            toAccountId = toAccountId,
            type = type,
            details = details,
            currency = currency,
            status = "completed"
        )
    }

    private fun com.sans.finance.data.local.entity.InstallmentPaymentRow.toDomain(): Expense {
        return Expense(
            id = this.id + INSTALLMENT_PAYMENT_ID_OFFSET,
            amount = this.amount,
            date = this.date,
            title = this.title,
            type = "EXPENSE",
            categoryId = this.categoryId,
            isInstallmentPayment = true,
            installmentMonth = this.monthNumber,
            installmentTotalMonths = this.totalMonths,
            status = this.status,
            details = this.details,
            accountId = this.accountId,
            currency = this.currency,
            tags = this.tagsList?.split(",")?.map { it.trim() }?.filter { it.isNotEmpty() }
                ?: emptyList(),
            categoryName = this.categoryName,
            categoryIcon = this.categoryIcon
        )
    }
}
