package com.billbuddy.shared.model

import kotlinx.serialization.Serializable

@Serializable
data class User(
    val id: String, // Usually from Google (e.g., sub)
    val email: String?,
    val displayName: String?,
    val photoUrl: String? = null // Optional
)
