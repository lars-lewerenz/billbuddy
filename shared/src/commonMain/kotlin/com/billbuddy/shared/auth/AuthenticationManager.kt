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

    suspend fun signOut() {
        googleAuthService.signOut()
        _currentUser.value = null
        // Clear any local session data / database entries if needed
    }
}
