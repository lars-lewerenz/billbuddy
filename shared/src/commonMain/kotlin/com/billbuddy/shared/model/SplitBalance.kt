package com.billbuddy.shared.model

import kotlinx.serialization.Serializable

@Serializable
data class SplitBalance(
    val user: User,
    val netBalance: Money // Positive if owed, negative if owes
)
