package com.ciclismo.portugal.data.remote.firebase

import com.ciclismo.portugal.domain.model.User
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import kotlinx.coroutines.flow.Flow

interface AuthService {
    val currentUser: Flow<User?>
    val isAuthenticated: Boolean

    suspend fun signInWithGoogle(account: GoogleSignInAccount): Result<User>
    suspend fun signInWithEmail(email: String, password: String): Result<User>
    suspend fun signUpWithEmail(email: String, password: String, displayName: String): Result<User>
    suspend fun sendPasswordResetEmail(email: String): Result<Unit>
    suspend fun signOut()
    fun getCurrentUser(): User?
}
