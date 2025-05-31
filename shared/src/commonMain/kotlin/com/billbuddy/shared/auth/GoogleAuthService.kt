package com.billbuddy.shared.auth

import com.billbuddy.shared.model.User

// Result sealed class for auth operations
sealed class AuthResult {
    data class Success(val user: User) : AuthResult()
    data class Error(val message: String) : AuthResult()
    object Cancelled : AuthResult() // User cancelled the operation
}

expect class GoogleAuthService {
    // Initiates the Google Sign-In process.
    // The actual UI interaction will be handled by the platform-specific implementation.
    suspend fun signIn(): AuthResult

    // Initiates the sign-out process.
    suspend fun signOut()

    // Gets the currently signed-in user, if any.
    // This might check a local cache or a silent sign-in.
    fun getCurrentUser(): User?
}
