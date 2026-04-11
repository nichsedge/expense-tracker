package com.sans.expensetracker.presentation.scan_invoice

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sans.expensetracker.domain.model.Expense
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SuggestedTransaction(
    val id: String = java.util.UUID.randomUUID().toString(),
    val title: String,
    val amount: Long,
    val category: String,
    val isAccepted: Boolean = true
)

data class ScanInvoiceState(
    val modelUri: Uri? = null,
    val imageUri: Uri? = null,
    val isProcessing: Boolean = false,
    val suggestedTransactions: List<SuggestedTransaction> = emptyList()
)

sealed class ScanInvoiceEvent {
    data class ModelSelected(val uri: Uri) : ScanInvoiceEvent()
    data class ImageSelected(val uri: Uri) : ScanInvoiceEvent()
    data class ToggleTransactionAcceptance(val id: String) : ScanInvoiceEvent()
    object SaveAcceptedTransactions : ScanInvoiceEvent()
}

@HiltViewModel
class ScanInvoiceViewModel @Inject constructor(
    private val expenseRepository: com.sans.expensetracker.domain.repository.ExpenseRepository
) : ViewModel() {

    private val _state = MutableStateFlow(ScanInvoiceState())
    val state: StateFlow<ScanInvoiceState> = _state.asStateFlow()

    fun onEvent(event: ScanInvoiceEvent) {
        when (event) {
            is ScanInvoiceEvent.ModelSelected -> {
                _state.update { it.copy(modelUri = event.uri) }
            }
            is ScanInvoiceEvent.ImageSelected -> {
                _state.update { it.copy(imageUri = event.uri, isProcessing = true) }
                processMockInference()
            }
            is ScanInvoiceEvent.ToggleTransactionAcceptance -> {
                _state.update { currentState ->
                    currentState.copy(
                        suggestedTransactions = currentState.suggestedTransactions.map {
                            if (it.id == event.id) it.copy(isAccepted = !it.isAccepted) else it
                        }
                    )
                }
            }
            is ScanInvoiceEvent.SaveAcceptedTransactions -> {
                viewModelScope.launch {
                    val accepted = _state.value.suggestedTransactions.filter { it.isAccepted }

                    // Fetch categories to try and match the string or assign default
                    val categories = expenseRepository.getAllCategories().firstOrNull() ?: emptyList()
                    val defaultCategoryId = categories.firstOrNull()?.id ?: 1L

                    accepted.forEach { tx ->
                        // Basic fuzzy match or fallback
                        val matchedCategory = categories.find { it.name.equals(tx.category, ignoreCase = true) }
                        val catId = matchedCategory?.id ?: defaultCategoryId

                        expenseRepository.insertExpense(
                            Expense(
                                date = System.currentTimeMillis(),
                                itemName = tx.title,
                                amount = tx.amount,
                                categoryId = catId,
                                merchant = "Scanned Invoice",
                                tags = listOf("AI Scanned")
                            )
                        )
                    }
                }
            }
        }
    }

    private fun processMockInference() {
        viewModelScope.launch {
            // Mock a 3 second delay for "running inference"
            delay(3000)

            val mockSuggestions = listOf(
                SuggestedTransaction(title = "Coffee", amount = 450, category = "Food"),
                SuggestedTransaction(title = "Bagel", amount = 300, category = "Food")
            )

            _state.update {
                it.copy(
                    isProcessing = false,
                    suggestedTransactions = mockSuggestions
                )
            }
        }
    }
}
