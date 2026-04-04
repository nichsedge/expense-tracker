package com.sans.expensetracker.presentation.expense_list

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sans.expensetracker.domain.model.Expense
import com.sans.expensetracker.domain.usecase.GetExpensesUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Locale

data class ExpenseListState(
    val expenses: List<Expense> = emptyList(),
    val groupedExpenses: Map<String, List<Expense>> = emptyMap(),
    val thisMonthSpent: Long = 0L,
    val totalFilteredAmount: Long = 0L,
    val startDate: Long = 0L,
    val endDate: Long = Long.MAX_VALUE,
    val isLoading: Boolean = true,
    val error: String? = null,
    val syncMessage: String? = null
)

@HiltViewModel
class ExpenseListViewModel @Inject constructor(
    private val getExpensesUseCase: GetExpensesUseCase,
    private val repository: com.sans.expensetracker.domain.repository.ExpenseRepository,
    private val installmentRepository: com.sans.expensetracker.domain.repository.InstallmentRepository,
    private val db: com.sans.expensetracker.data.local.AppDatabase
) : ViewModel() {

    private val _state = MutableStateFlow(ExpenseListState())
    val state: StateFlow<ExpenseListState> = _state.asStateFlow()

    private val dateFormat = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())

    init {
        // Default to this month
        val calendar = java.util.Calendar.getInstance()
        calendar.set(java.util.Calendar.DAY_OF_MONTH, 1)
        calendar.set(java.util.Calendar.HOUR_OF_DAY, 0)
        calendar.set(java.util.Calendar.MINUTE, 0)
        calendar.set(java.util.Calendar.SECOND, 0)
        calendar.set(java.util.Calendar.MILLISECOND, 0)
        
        _state.update { it.copy(startDate = calendar.timeInMillis) }
        
        loadExpenses()
        loadHistoricalStats()
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    private fun loadExpenses() {
        _state
            .map { it.startDate to it.endDate }
            .distinctUntilChanged()
            .flatMapLatest { (start, end) ->
                repository.getExpensesBetween(start, end)
            }
            .onEach { expenses ->
                val grouped = groupExpensesByDate(expenses)
                _state.update { it.copy(
                    expenses = expenses, 
                    groupedExpenses = grouped,
                    totalFilteredAmount = expenses.sumOf { it.amount },
                    isLoading = false
                ) }
            }
            .launchIn(viewModelScope)
    }

    private fun groupExpensesByDate(expenses: List<Expense>): Map<String, List<Expense>> {
        val dailyTotalFormat = java.text.NumberFormat.getCurrencyInstance(java.util.Locale("id", "ID"))
        return expenses.groupBy { expense ->
            dateFormat.format(java.util.Date(expense.date))
        }.mapKeys { (date, items) ->
            val total = items.sumOf { it.amount }
            val totalStr = dailyTotalFormat.format(total / 100.0).replace(",00", "")
            "$date • Total: $totalStr"
        }
    }

    fun updateDateRange(startDate: Long, endDate: Long = Long.MAX_VALUE) {
        _state.update { it.copy(startDate = startDate, endDate = endDate, isLoading = true) }
    }

    private fun loadHistoricalStats() {
        // This Month
        val calendar = java.util.Calendar.getInstance()
        calendar.set(java.util.Calendar.DAY_OF_MONTH, 1)
        calendar.set(java.util.Calendar.HOUR_OF_DAY, 0)
        
        val monthExpensesFlow = repository.getTotalSpentSince(calendar.timeInMillis)
        val monthInstallmentsFlow = installmentRepository.getTotalPaidAmountSince(calendar.timeInMillis)

        monthExpensesFlow.combine(monthInstallmentsFlow) { exp, inst ->
            (exp ?: 0L) + (inst ?: 0L)
        }.onEach { total ->
            _state.update { it.copy(thisMonthSpent = total) }
        }.launchIn(viewModelScope)


    }

    fun deleteExpense(expense: Expense) {
        viewModelScope.launch {
            repository.deleteExpense(expense)
        }
    }

    fun exportFullBackup(context: android.content.Context) {
        _state.update { it.copy(isLoading = true) }
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            try {
                // Ensure all WAL data is flushed to the main SQLite file
                db.checkpoint()
                
                val dbName = "expense_tracker_db"
                val dbFile = context.getDatabasePath(dbName)
                
                if (!dbFile.exists()) {
                    _state.update { it.copy(isLoading = false, error = "Database not found") }
                    return@launch
                }

                val snapshotName = "expense_tracker_db_snapshot.sqlite"
                val resolver = context.contentResolver
                val relativePath = "${android.os.Environment.DIRECTORY_DOWNLOADS}/"
                
                // Delete any existing snapshot with the same name (regardless of path) for idempotency
                val selection = "${android.provider.MediaStore.MediaColumns.DISPLAY_NAME} = ?"
                val selectionArgs = arrayOf(snapshotName)
                resolver.delete(android.provider.MediaStore.Downloads.EXTERNAL_CONTENT_URI, selection, selectionArgs)

                val contentValues = android.content.ContentValues().apply {
                    put(android.provider.MediaStore.MediaColumns.DISPLAY_NAME, snapshotName)
                    put(android.provider.MediaStore.MediaColumns.MIME_TYPE, "application/x-sqlite3")
                    put(android.provider.MediaStore.MediaColumns.RELATIVE_PATH, relativePath)
                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                        put(android.provider.MediaStore.MediaColumns.IS_PENDING, 1)
                    }
                }
                
                val uri = resolver.insert(android.provider.MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)
                uri?.let {
                    resolver.openOutputStream(it, "wt")?.use { outputStream -> // Use "wt" for truncate/overwrite if URI reused
                        FileInputStream(dbFile).use { inputStream ->
                            inputStream.copyTo(outputStream)
                        }
                    }
                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                        val done = android.content.ContentValues().apply {
                            put(android.provider.MediaStore.MediaColumns.IS_PENDING, 0)
                        }
                        resolver.update(it, done, null, null)
                    }
                    _state.update { it.copy(isLoading = false, syncMessage = "Snapshot Saved: $snapshotName") }
                } ?: run {
                    _state.update { it.copy(isLoading = false, error = "Failed to create snapshot") }
                }
            } catch (e: Exception) {
                _state.update { it.copy(isLoading = false, error = e.message) }
            }
        }
    }

    fun clearSyncMessage() {
        _state.update { it.copy(syncMessage = null) }
    }
}
