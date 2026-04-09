package com.sans.expensetracker.domain.usecase

import com.sans.expensetracker.domain.model.Installment
import com.sans.expensetracker.domain.repository.InstallmentRepository
import javax.inject.Inject

class CreateInstallmentPlanUseCase @Inject constructor(
    private val installmentRepository: InstallmentRepository
) {
    suspend operator fun invoke(
        expenseId: Long,
        totalAmount: Long,
        durationMonths: Int,
        startDate: Long
    ) {
        if (durationMonths <= 0) return

        val monthlyPayment = totalAmount / durationMonths

        val installment = Installment(
            expenseId = expenseId,
            totalAmount = totalAmount,
            monthlyPayment = monthlyPayment,
            durationMonths = durationMonths,
            remainingBalance = totalAmount,
            nextDueDate = startDate,
            status = "Active"
        )

        val installmentId = installmentRepository.createInstallment(installment)

        installmentRepository.createInstallmentItems(
            installmentId,
            durationMonths,
            monthlyPayment,
            startDate
        )
    }
}
