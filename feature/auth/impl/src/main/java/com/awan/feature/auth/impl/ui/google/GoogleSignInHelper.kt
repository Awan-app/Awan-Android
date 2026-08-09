package com.awan.feature.auth.impl.ui.google

import android.content.Context
import android.content.Intent
import android.util.Log
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GoogleSignInHelper @Inject constructor() {

    fun getGoogleSignInIntent(context: Context): Result<Intent> {
        return runCatching {
            val webClientId = getWebClientId(context)
                ?: throw IllegalStateException("Google Web Client ID (default_web_client_id) not found in resources.")

            val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(webClientId)
                .requestEmail()
                .build()

            val googleSignInClient = GoogleSignIn.getClient(context, gso)
            googleSignInClient.signOut()
            googleSignInClient.signInIntent
        }
    }

    suspend fun getFirebaseIdToken(googleIdToken: String): String {
        return try {
            val firebaseCredential = GoogleAuthProvider.getCredential(googleIdToken, null)
            val authResult = Firebase.auth.signInWithCredential(firebaseCredential).await()
            val user = authResult.user
                ?: throw IllegalStateException("Firebase user is null after Google sign-in.")

            if (Log.isLoggable(TAG, Log.DEBUG)) {
                Log.d(TAG, "Successfully signed into Firebase Auth (uid=${user.uid})")
            }

            val tokenResult = user.getIdToken(false).await()
            tokenResult.token ?: throw IllegalStateException("Firebase ID token is null.")
        } catch (e: Exception) {
            Log.e(TAG, "Error exchanging Google ID token for Firebase ID token", e)
            throw e
        }
    }

    private fun getWebClientId(context: Context): String? {
        val resId = context.resources.getIdentifier(
            "default_web_client_id",
            "string",
            context.packageName,
        )
        if (resId != 0) {
            val webClientId = context.getString(resId)
            Log.d(TAG, "Resolved default_web_client_id: $webClientId for package ${context.packageName}")
            return webClientId
        }
        Log.e(TAG, "Failed to resolve string resource 'default_web_client_id' for package ${context.packageName}")
        return null
    }

    private companion object {
        private const val TAG = "GoogleSignInHelper"
    }
}
