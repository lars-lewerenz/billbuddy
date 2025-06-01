package com.billbuddy.shared.auth

import android.app.Activity
import android.content.Context
import android.content.Intent
import com.billbuddy.shared.model.User
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.tasks.Task
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

// Companion object to hold the GoogleSignInClient and a way to get the sign-in intent
// This is a bit unconventional for KMP expect/actual but helps manage Android specifics.
// A better approach might involve an interface passed from androidApp to shared.
object GoogleSignInHolder {
    private var _googleSignInClient: GoogleSignInClient? = null
    val googleSignInClient: GoogleSignInClient get() = _googleSignInClient ?: throw IllegalStateException("GoogleSignInClient not initialized. Call init first.")

    // Observable state for the sign-in result, to be handled by the Activity
    // private val _signInResultIntent = MutableStateFlow<Intent?>(null) // This flow isn't directly used by the proposed activity code
    // val signInIntentFlow: StateFlow<Intent?> = _signInResultIntent.asStateFlow() // So commenting it out

    fun init(context: Context, webClientId: String) {
        if (_googleSignInClient == null) {
            val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(webClientId) // Crucial for backend authentication
                .requestEmail()
                .requestProfile()
                .build()
            _googleSignInClient = GoogleSignIn.getClient(context.applicationContext, gso)
        }
    }

    fun getSignInIntent(): Intent {
        return googleSignInClient.signInIntent
    }

    // To be called from Activity's onActivityResult or new Activity Result API
    // Returns User on success, null on failure/cancellation
    suspend fun handleSignInResult(data: Intent?): AuthResult {
        return try {
            val task = GoogleSignIn.getSignedInAccountFromIntent(data)
            val account = task.getResult(ApiException::class.java)
            if (account != null) {
                val user = User(
                    id = account.id ?: uuid4(), // Fallback if id is null
                    email = account.email,
                    displayName = account.displayName,
                    photoUrl = account.photoUrl?.toString()
                    // idToken = account.idToken // Important: Handle this securely!
                )
                AuthResult.Success(user)
            } else {
                AuthResult.Error("GoogleSignInAccount is null")
            }
        } catch (e: ApiException) {
            AuthResult.Error("Google Sign-In failed: ${e.statusCode} - ${e.message}")
        } catch (e: Exception) {
            AuthResult.Error("Google Sign-In failed: ${e.message}")
        }
    }
}


actual class GoogleAuthService {
    // Context is now managed by GoogleSignInHolder for initialization
    // No need to pass it here if using the holder pattern for client

    // Call this from your Android Application's onCreate or a Koin module
    fun initialize(context: Context, webClientId: String = "YOUR_WEB_CLIENT_ID_PLACEHOLDER") {
         // IMPORTANT: Replace "YOUR_WEB_CLIENT_ID_PLACEHOLDER" with your actual Web Client ID from Google Cloud Console
        GoogleSignInHolder.init(context.applicationContext, webClientId)
    }

    actual suspend fun signIn(): AuthResult {
        // This function now signals the Activity to launch the intent.
        // The result is handled separately via handleSignInResult.
        // This KMP interface method might need to change to better suit Android's Intent flow.
        // For now, it just triggers the intent generation for the Activity to pick up.
        // The Activity will observe a state or call a method to get the intent.
        // GoogleSignInHolder.getSignInIntent() // The activity should call this directly.
        // This method can't directly return the AuthResult anymore with this pattern if it's just a trigger.
        // It's better if this method is NOT called directly by AuthenticationManager for starting the sign-in.
        // The Activity should initiate the Google Sign-In.
        // This KMP method is more for cases where KMP logic could somehow trigger it,
        // but for Android, the Activity is the entry point for UI ops.
        // So we return an error indicating it should be launched from UI.
        return AuthResult.Error("Sign-in must be initiated from the Android Activity.")
    }

    actual suspend fun signOut() {
        try {
            GoogleSignInHolder.googleSignInClient.signOut().await()
            // Also consider revoking access if you want a full sign out:
            // GoogleSignInHolder.googleSignInClient.revokeAccess().await()
        } catch (e: Exception) {
            // Handle error
            println("Error signing out: ${e.message}")
        }
    }

    actual fun getCurrentUser(): User? {
        // Check if GoogleSignInClient has been initialized to prevent crash
        // This might happen if getCurrentUser is called before initialization (e.g. in AuthenticationManager init)
        // A better approach might be to have an observable state for initialization.
        return try {
            val account: GoogleSignInAccount? = GoogleSignIn.getLastSignedInAccount(GoogleSignInHolder.googleSignInClient.applicationContext)
            account?.let {
                User(
                    id = it.id ?: uuid4(),
                    email = it.email,
                    displayName = it.displayName,
                    photoUrl = it.photoUrl?.toString()
                )
            }
        } catch (e: IllegalStateException) {
            // GoogleSignInClient not initialized yet
            println("getCurrentUser called before GoogleSignInClient initialized: ${e.message}")
            null
        }
    }
}
