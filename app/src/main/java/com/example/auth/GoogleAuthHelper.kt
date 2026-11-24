package com.example.auth

import android.content.Context
import android.util.Log
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import com.example.R
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.auth
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

object GoogleAuthHelper {
    private const val TAG = "GoogleAuthHelper"

    private fun resolveClientId(context: Context): String? {
        val clientId = context.getString(R.string.default_web_client_id).trim()
        return clientId.takeIf { it.isNotEmpty() && !it.startsWith("REPLACE") }
    }

    /**
     * Silent auto sign-in at app startup for returning authorized Google accounts.
     */
    suspend fun attemptAutoSignIn(
        activityContext: Context,
        auth: FirebaseAuth = Firebase.auth
    ): FirebaseUser? {
        if (auth.currentUser != null) return auth.currentUser
        val clientId = resolveClientId(activityContext) ?: return null

        return try {
            val credentialManager = CredentialManager.create(activityContext)
            val googleIdOption = GetGoogleIdOption.Builder()
                .setFilterByAuthorizedAccounts(true)
                .setServerClientId(clientId)
                .setAutoSelectEnabled(true)
                .build()
            val request = GetCredentialRequest.Builder()
                .addCredentialOption(googleIdOption)
                .build()

            val result = credentialManager.getCredential(activityContext, request)
            val idToken = GoogleIdTokenCredential.createFrom(result.credential.data).idToken
            val authResult = auth.signInWithCredential(
                GoogleAuthProvider.getCredential(idToken, null)
            ).await()
            authResult.user
        } catch (e: CancellationException) {
            throw e
        } catch (e: GetCredentialCancellationException) {
            Log.d(TAG, "Auto sign-in cancelled by user")
            null
        } catch (e: GetCredentialException) {
            Log.d(TAG, "No authorized Google credential available for silent sign-in: ${e.message}")
            null
        } catch (e: Exception) {
            Log.w(TAG, "Silent auto sign-in failed", e)
            null
        }
    }

    /**
     * Interactive Google Sign-In using GetSignInWithGoogleOption.
     */
    suspend fun signInWithGoogle(
        activityContext: Context,
        auth: FirebaseAuth = Firebase.auth
    ): Result<FirebaseUser> {
        val clientId = resolveClientId(activityContext)
            ?: return Result.failure(IllegalStateException("Google Sign-In client ID is missing"))

        return try {
            val credentialManager = CredentialManager.create(activityContext)
            val signInOption = GetSignInWithGoogleOption.Builder(clientId).build()
            val request = GetCredentialRequest.Builder()
                .addCredentialOption(signInOption)
                .build()

            val result = credentialManager.getCredential(activityContext, request)
            val googleCredential = GoogleIdTokenCredential.createFrom(result.credential.data)
            val firebaseCredential = GoogleAuthProvider.getCredential(googleCredential.idToken, null)
            val authResult = auth.signInWithCredential(firebaseCredential).await()
            val user = authResult.user
                ?: return Result.failure(IllegalStateException("Firebase sign-in returned null user"))
            Result.success(user)
        } catch (e: CancellationException) {
            throw e
        } catch (e: GetCredentialCancellationException) {
            Log.w(TAG, "User cancelled Google Sign-In", e)
            Result.failure(e)
        } catch (e: Exception) {
            Log.e(TAG, "Google Sign-In failed", e)
            Result.failure(e)
        }
    }

    /**
     * Safely clears Credential Manager state and signs out of FirebaseAuth inside NonCancellable
     * after callers have detached Firestore listeners.
     */
    suspend fun signOut(
        context: Context,
        auth: FirebaseAuth = Firebase.auth
    ) = withContext(NonCancellable) {
        try {
            CredentialManager.create(context).clearCredentialState(ClearCredentialStateRequest())
        } catch (e: Exception) {
            Log.w(TAG, "Unable to clear Credential Manager state", e)
        }
        try {
            auth.signOut()
        } catch (e: Exception) {
            Log.w(TAG, "Firebase sign-out failed", e)
        }
    }
}
