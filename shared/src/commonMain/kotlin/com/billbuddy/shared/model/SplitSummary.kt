package com.billbuddy.shared.model

import kotlinx.serialization.Serializable

@Serializable
data class SplitSummary(
    val balances: List<SplitBalance>,
    val recommendedTransactions: List<UserDebt>
)
