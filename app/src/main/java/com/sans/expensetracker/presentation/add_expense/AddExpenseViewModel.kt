package com.sans.expensetracker.presentation.add_expense

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sans.expensetracker.domain.model.Expense
import com.sans.expensetracker.domain.usecase.AddExpenseUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import androidx.lifecycle.SavedStateHandle
import androidx.navigation.toRoute
import com.sans.expensetracker.domain.usecase.GetExpenseByIdUseCase
import com.sans.expensetracker.domain.usecase.UpdateExpenseUseCase
import com.sans.expensetracker.presentation.navigation.Screen
import javax.inject.Inject

@HiltViewModel
class AddExpenseViewModel @Inject constructor(
    private val addExpenseUseCase: AddExpenseUseCase,
    private val updateExpenseUseCase: UpdateExpenseUseCase,
    private val getExpenseByIdUseCase: GetExpenseByIdUseCase,
    private val getCategoriesUseCase: com.sans.expensetracker.domain.usecase.GetCategoriesUseCase,
    private val createInstallmentPlanUseCase: com.sans.expensetracker.domain.usecase.CreateInstallmentPlanUseCase,
    private val installmentRepository: com.sans.expensetracker.domain.repository.InstallmentRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val editExpenseId: Long? = try {
        savedStateHandle.toRoute<Screen.EditExpense>().expenseId
    } catch (e: Exception) {
        null
    }

    val isEditMode get() = editExpenseId != null

    init {
        editExpenseId?.let { id ->
            viewModelScope.launch {
                getExpenseByIdUseCase(id)?.let { expense ->
                    amount = (expense.amount.toDouble() / 100.0).toString()
                    itemName = expense.itemName
                    merchant = expense.merchant ?: ""
                    categoryId = expense.categoryId
                    isInstallment = expense.isInstallment
                    selectedDate = expense.date
                    
                    if (expense.isInstallment) {
                        installmentRepository.getInstallmentByExpenseId(id)?.let { installment ->
                            durationMonths = installment.durationMonths.toString()
                        }
                    }
                }
            }
        }
    }

    val categories = getCategoriesUseCase().stateIn(
        scope = viewModelScope,
        started = kotlinx.coroutines.flow.SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    var amount by mutableStateOf("")
    var itemName by mutableStateOf("")
    var merchant by mutableStateOf("")
    var categoryId by mutableLongStateOf(0L)
    var isInstallment by mutableStateOf(false)
    var durationMonths by mutableStateOf("")
    var selectedDate by mutableLongStateOf(System.currentTimeMillis())

    fun onSaveClick(onSuccess: () -> Unit) {
        val amountInCents = amount.toSafeLongCents() ?: 0L
        
        viewModelScope.launch {
            val expense = Expense(
                id = editExpenseId ?: 0,
                date = selectedDate,
                itemName = itemName.ifBlank { "Uncategorized Item" },
                amount = amountInCents,
                categoryId = categoryId,
                paymentMethod = "Cash",
                isInstallment = isInstallment,
                merchant = merchant.ifBlank { null },
                quantity = 1
            )
            
            if (editExpenseId == null) {
                val expenseId = addExpenseUseCase(expense)
                if (isInstallment && durationMonths.isNotBlank()) {
                    val duration = durationMonths.toIntOrNull() ?: 0
                    createInstallmentPlanUseCase(expenseId, amountInCents, duration, selectedDate)
                }
            } else {
                updateExpenseUseCase(expense, durationMonths.toIntOrNull())
            }
            onSuccess()
        }
    }

    private fun String.toSafeLongCents(): Long? {
        return try {
            val cleanStr = this.replace(",", ".")
            val pieces = cleanStr.split(".")
            val major = pieces[0].toLongOrNull() ?: 0L
            val minor = if (pieces.size > 1) {
                pieces[1].take(2).padEnd(2, '0').toLongOrNull() ?: 0L
            } else 0L
            (major * 100) + minor
        } catch (e: Exception) {
            null
        }
    }
}
