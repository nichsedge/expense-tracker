package com.sans.finance.presentation.installments

import com.sans.finance.domain.model.Installment
import com.sans.finance.domain.model.InstallmentItem

data class InstallmentDetailContext(
    val installment: Installment,
    val items: List<InstallmentItem>,
    val currencyCode: String = "USD"
)
