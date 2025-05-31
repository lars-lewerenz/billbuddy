package com.billbuddy.shared.model

import kotlinx.serialization.Serializable

// Using a typealias for Double to represent money, consider a dedicated Money class for precision in a real app
typealias Money = Double

@Serializable
data class Expense(
    val id: String, // Unique ID for the expense
    val groupId: String, // ID of the group this expense belongs to
    val description: String,
    val amount: Money,
    val paidBy: String, // User ID of the person who paid
    val participants: List<String>, // List of User IDs who participated in this expense
    val date: Long // Timestamp of when the expense occurred or was added
)
