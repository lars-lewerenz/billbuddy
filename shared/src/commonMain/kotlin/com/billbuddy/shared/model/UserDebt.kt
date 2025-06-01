package com.billbuddy.shared.model

import kotlinx.serialization.Serializable

@Serializable
data class UserDebt(
    val fromUser: User, // The user who owes money
    val toUser: User,   // The user who should receive money
    val amount: Money    // The amount to be transferred
)
