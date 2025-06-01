package com.billbuddy.android.ui.login

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.tooling.preview.Preview
import com.billbuddy.android.ui.theme.BillBuddyTheme
import com.billbuddy.shared.auth.AuthenticationManager
import com.billbuddy.shared.model.User
import org.koin.androidx.compose.koinViewModel // For Koin ViewModel

// A simple LoginViewModel (can be enhanced later)
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.StateFlow
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject


class LoginViewModel : ViewModel(), KoinComponent {
    private val authenticationManager: AuthenticationManager by inject()
    val currentUser: StateFlow<User?> = authenticationManager.currentUser
}


@Composable
fun LoginScreen(
    loginViewModel: LoginViewModel = koinViewModel(), // Get ViewModel via Koin
    onSignInClick: () -> Unit,
    onNavigateToGroups: () -> Unit // Callback for navigation (placeholder for now)
) {
    val user by loginViewModel.currentUser.collectAsState()

    // If user is already signed in, MainActivity's LaunchedEffect will handle navigation.
    // This screen will just reflect the current state.

    BillBuddyTheme {
        Surface(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Bill Buddy",
                    style = MaterialTheme.typography.headlineLarge,
                    modifier = Modifier.padding(bottom = 48.dp)
                )

                if (user == null) {
                    Button(
                        onClick = onSignInClick,
                        modifier = Modifier.fillMaxWidth(0.8f)
                    ) {
                        Text("Sign In with Google")
                    }
                } else {
                    Text("Welcome, ${user?.displayName ?: "User"}!")
                    // Button to manually navigate (for testing, can be removed)
                    // Button(onClick = onNavigateToGroups) { Text("Go to Groups (Placeholder)") }
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun LoginScreenPreviewNotSignedIn() {
    BillBuddyTheme {
        // For preview, you'd typically pass parameters or use a fake ViewModel
        // that doesn't rely on actual Koin injection for the preview to build.
        // Creating a dummy LoginViewModel for preview purposes:
        val dummyAuthManager = AuthenticationManager(com.billbuddy.shared.auth.GoogleAuthService()) // Assuming default constructor for actual
        class PreviewLoginViewModel(authManager: AuthenticationManager) : LoginViewModel() {
            // Override or provide fake data if needed, though base class might be enough if it handles null authManager for preview
        }

        LoginScreen(
            // loginViewModel = PreviewLoginViewModel(dummyAuthManager), // This approach for preview might be complex
            onSignInClick = {},
            onNavigateToGroups = {}
        )
    }
}
