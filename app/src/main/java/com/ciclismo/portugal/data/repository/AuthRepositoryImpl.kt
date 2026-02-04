package com.ciclismo.portugal.data.repository

import com.ciclismo.portugal.data.remote.firebase.AuthService
import com.ciclismo.portugal.domain.model.User
import com.ciclismo.portugal.domain.repository.AuthRepository
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthRepositoryImpl @Inject constructor(
    private val authService: AuthService
) : AuthRepository {

    override val currentUser: Flow<User?>
        get() = authService.currentUser

    override val isAuthenticated: Boolean
        get() = authService.isAuthenticated

    override suspend fun signInWithGoogle(account: GoogleSignInAccount): Result<User> {
        return authService.signInWithGoogle(account)
    }

    override suspend fun signInWithEmail(email: String, password: String): Result<User> {
        return authService.signInWithEmail(email, password)
    }

    override suspend fun signUpWithEmail(email: String, password: String, displayName: String): Result<User> {
        return authService.signUpWithEmail(email, password, displayName)
    }

    override suspend fun sendPasswordResetEmail(email: String): Result<Unit> {
        return authService.sendPasswordResetEmail(email)
    }

    override suspend fun signOut() {
        authService.signOut()
    }

    override fun getCurrentUser(): User? {
        return authService.getCurrentUser()
    }
}
