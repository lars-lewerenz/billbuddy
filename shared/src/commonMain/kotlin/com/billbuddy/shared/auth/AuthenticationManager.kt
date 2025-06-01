package com.billbuddy.shared.auth

import com.billbuddy.shared.model.User
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class AuthenticationManager(private val googleAuthService: GoogleAuthService) {
    private val _currentUser = MutableStateFlow<User?>(null)
    val currentUser: StateFlow<User?> = _currentUser.asStateFlow()

    init {
        // Initialize with current user if already signed in
        _currentUser.value = googleAuthService.getCurrentUser()
    }

    suspend fun signInWithGoogle(): AuthResult {
        val result = googleAuthService.signIn()
        if (result is AuthResult.Success) {
            _currentUser.value = result.user
            // Here you might want to save the user to your SQLDelight database
            // For example: database.userQueries.insertUser(result.user.id, result.user.email, result.user.displayName)
        }
        return result
    }

    // Called by platform-specific code after a successful sign-in from Google (e.g. Activity result)
    fun processGoogleSignInSuccess(user: User) {
        _currentUser.value = user
        // Potentially save user to DB here as well
        // For example: database.userQueries.insertUser(user.id, user.email, user.displayName)
        println("AuthenticationManager: User signed in - ${user.displayName}")
    }

    // Called by platform-specific code if sign-in fails or is cancelled
    fun clearUserOnError() {
        _currentUser.value = null
        // No need to call googleAuthService.signOut() here as there might not be a session,
        // or if there was a session but sign-in renewal failed, signOut might be called separately if appropriate.
        println("AuthenticationManager: User cleared due to error or cancellation.")
    }

    suspend fun signOut() {
        googleAuthService.signOut()
        _currentUser.value = null
        // Clear any local session data / database entries if needed
         println("AuthenticationManager: User signed out.")
    }
}
