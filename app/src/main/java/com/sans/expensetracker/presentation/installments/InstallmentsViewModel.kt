package com.sans.expensetracker.presentation.installments

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sans.expensetracker.domain.model.Installment
import com.sans.expensetracker.domain.repository.InstallmentRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class InstallmentsState(
    val activeInstallments: List<Installment> = emptyList(),
    val totalMonthlyDue: Long = 0L,
    val totalRemainingBalance: Long = 0L
)

@HiltViewModel
class InstallmentsViewModel @Inject constructor(
    private val installmentRepository: InstallmentRepository
) : ViewModel() {

    private val _state = MutableStateFlow(InstallmentsState())
    val state: StateFlow<InstallmentsState> = _state

    init {
        viewModelScope.launch {
            installmentRepository.getActiveInstallments().collect { installments ->
                _state.value = InstallmentsState(
                    activeInstallments = installments,
                    totalMonthlyDue = installments.sumOf { it.monthlyPayment },
                    totalRemainingBalance = installments.sumOf { it.remainingBalance }
                )
            }
        }
    }

    fun onToggleStatus(itemId: Long, currentStatus: String) {
        viewModelScope.launch {
            val nextStatus = if (currentStatus == "Paid") "Pending" else "Paid"
            installmentRepository.updateInstallmentItemStatus(itemId, nextStatus)
        }
    }

    fun getItemsForInstallment(installmentId: Long) = 
        installmentRepository.getInstallmentItems(installmentId)
}
