package com.billbuddy.android

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.lifecycleScope
import com.billbuddy.android.ui.login.LoginScreen
import com.billbuddy.android.ui.login.LoginViewModel // Import the new ViewModel
import com.billbuddy.android.ui.theme.BillBuddyTheme
import com.billbuddy.shared.auth.AuthResult
import com.billbuddy.shared.auth.AuthenticationManager
import com.billbuddy.shared.auth.GoogleAuthService
import com.billbuddy.shared.auth.GoogleSignInHolder
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject
import org.koin.androidx.viewmodel.ext.android.viewModel // For Koin ViewModel injection

class MainActivity : ComponentActivity() {

    private val googleAuthService: GoogleAuthService by inject()
    private val authenticationManager: AuthenticationManager by inject()
    private val loginViewModel: LoginViewModel by viewModel() // Inject LoginViewModel

    private lateinit var signInResultLauncher: ActivityResultLauncher<Intent>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Initialize GoogleAuthService (ideally in Application class or Koin module)
        // Ensure this uses the actual Web Client ID.
        googleAuthService.initialize(this, "YOUR_WEB_CLIENT_ID_PLACEHOLDER") // Ensure this is called

        signInResultLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                lifecycleScope.launch {
                    val authResult = GoogleSignInHolder.handleSignInResult(result.data)
                    handleAuthResult(authResult) // Changed to suspend
                }
            } else {
                Toast.makeText(this, "Google Sign-In cancelled or failed (code: ${result.resultCode})", Toast.LENGTH_LONG).show()
                handleAuthResult(AuthResult.Cancelled) // Pass Cancelled state
            }
        }

        setContent {
            BillBuddyTheme {
                val user by loginViewModel.currentUser.collectAsState()

                // Simple navigation placeholder: If user is signed in, show a toast.
                // In a real app, this would navigate to the main content (e.g., GroupListScreen).
                LaunchedEffect(user) {
                    if (user != null) {
                        Toast.makeText(this@MainActivity, "User signed in: ${user?.displayName}. Navigating to groups (placeholder)...", Toast.LENGTH_SHORT).show()
                        // onNavigateToGroups() // Call actual navigation logic here
                    }
                }

                LoginScreen(
                    loginViewModel = loginViewModel,
                    onSignInClick = {
                        val signInIntent = GoogleSignInHolder.getSignInIntent()
                        signInResultLauncher.launch(signInIntent)
                    },
                    onNavigateToGroups = {
                        // This callback would be used by a NavHost typically.
                        // For now, it's a placeholder.
                        Toast.makeText(this@MainActivity, "Navigate to Groups Placeholder", Toast.LENGTH_SHORT).show()
                    }
                )
            }
        }
    }

    private suspend fun handleAuthResult(authResult: AuthResult) { // Changed to suspend
        when (authResult) {
            is AuthResult.Success -> {
                // The AuthenticationManager should ideally have a method to directly process this.
                // E.g., authenticationManager.onSignInSuccess(authResult.user)
                // For now, we make AuthenticationManager re-trigger its own logic.
                // This specific call might need refinement as AuthenticationManager.signInWithGoogle()
                // calls GoogleAuthService.signIn() which is now just a stub in androidMain.
                // The state update should primarily come from the GoogleSignInHolder.handleSignInResult
                // and then propagate to AuthenticationManager.
                // A better way:
                authenticationManager.processGoogleSignInSuccess(authResult.user)
                // Toast.makeText(this, "Signed in: ${authResult.user.displayName}", Toast.LENGTH_LONG).show() // Handled by LaunchedEffect
            }
            is AuthResult.Error -> {
                Toast.makeText(this, "Sign-in error: ${authResult.message}", Toast.LENGTH_LONG).show()
                authenticationManager.clearUserOnError() // Method to clear user on error
            }
            AuthResult.Cancelled -> {
                Toast.makeText(this, "Sign-in cancelled by user.", Toast.LENGTH_SHORT).show()
                authenticationManager.clearUserOnError() // Method to clear user on error/cancel
            }
        }
    }
}
