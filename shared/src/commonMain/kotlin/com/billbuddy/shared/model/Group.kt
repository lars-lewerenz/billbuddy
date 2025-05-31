package com.billbuddy.shared.model

import kotlinx.serialization.Serializable

@Serializable
data class Group(
    val id: String, // Unique ID for the group
    val name: String,
    val members: List<User>, // List of users in the group
    val createdBy: String, // User ID of the creator
    val inviteCode: String? = null // Simple invite code or link identifier
)
