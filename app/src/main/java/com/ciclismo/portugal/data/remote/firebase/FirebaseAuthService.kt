package com.ciclismo.portugal.data.remote.firebase

import android.util.Log
import com.ciclismo.portugal.domain.model.User
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseAuthWeakPasswordException
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.userProfileChangeRequest
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FirebaseAuthService @Inject constructor(
    private val firebaseAuth: FirebaseAuth,
    private val firestore: FirebaseFirestore
) : AuthService {

    companion object {
        private const val TAG = "FirebaseAuthService"
        private const val ADMINS_COLLECTION = "admins"
    }

    // Cache admin status to avoid repeated Firestore calls
    private var cachedAdminUids: Set<String> = emptySet()
    private var lastAdminFetch: Long = 0
    private val ADMIN_CACHE_DURATION = 5 * 60 * 1000L // 5 minutes

    override val currentUser: Flow<User?> = callbackFlow {
        val listener = FirebaseAuth.AuthStateListener { auth ->
            trySend(auth.currentUser?.toUser())
        }
        firebaseAuth.addAuthStateListener(listener)
        awaitClose { firebaseAuth.removeAuthStateListener(listener) }
    }

    override val isAuthenticated: Boolean
        get() = firebaseAuth.currentUser != null

    override suspend fun signInWithGoogle(account: GoogleSignInAccount): Result<User> {
        return try {
            val credential = GoogleAuthProvider.getCredential(account.idToken, null)
            val authResult = firebaseAuth.signInWithCredential(credential).await()
            val user = authResult.user?.toUser()
                ?: return Result.failure(Exception("Falha ao obter dados do utilizador"))
            Result.success(user)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun signInWithEmail(email: String, password: String): Result<User> {
        return try {
            val authResult = firebaseAuth.signInWithEmailAndPassword(email, password).await()
            val user = authResult.user?.toUser()
                ?: return Result.failure(Exception("Falha ao obter dados do utilizador"))
            Result.success(user)
        } catch (e: FirebaseAuthInvalidUserException) {
            Result.failure(Exception("Utilizador não encontrado"))
        } catch (e: FirebaseAuthInvalidCredentialsException) {
            Result.failure(Exception("Email ou password incorretos"))
        } catch (e: Exception) {
            Result.failure(Exception("Erro ao fazer login: ${e.message}"))
        }
    }

    override suspend fun signUpWithEmail(email: String, password: String, displayName: String): Result<User> {
        return try {
            val authResult = firebaseAuth.createUserWithEmailAndPassword(email, password).await()
            val firebaseUser = authResult.user
                ?: return Result.failure(Exception("Falha ao criar conta"))

            // Atualizar nome do utilizador
            val profileUpdates = userProfileChangeRequest {
                this.displayName = displayName
            }
            firebaseUser.updateProfile(profileUpdates).await()

            Result.success(firebaseUser.toUser().copy(displayName = displayName))
        } catch (e: FirebaseAuthWeakPasswordException) {
            Result.failure(Exception("Password demasiado fraca. Use pelo menos 6 caracteres"))
        } catch (e: FirebaseAuthInvalidCredentialsException) {
            Result.failure(Exception("Email inválido"))
        } catch (e: FirebaseAuthUserCollisionException) {
            Result.failure(Exception("Este email já está registado"))
        } catch (e: Exception) {
            Result.failure(Exception("Erro ao criar conta: ${e.message}"))
        }
    }

    override suspend fun sendPasswordResetEmail(email: String): Result<Unit> {
        return try {
            firebaseAuth.sendPasswordResetEmail(email).await()
            Result.success(Unit)
        } catch (e: FirebaseAuthInvalidUserException) {
            Result.failure(Exception("Email não encontrado"))
        } catch (e: Exception) {
            Result.failure(Exception("Erro ao enviar email: ${e.message}"))
        }
    }

    override suspend fun signOut() {
        firebaseAuth.signOut()
    }

    override fun getCurrentUser(): User? {
        val fbUser = firebaseAuth.currentUser
        Log.d(TAG, "getCurrentUser: fbUser=${fbUser?.uid}, email=${fbUser?.email}")
        val user = fbUser?.toUser()
        Log.d(TAG, "getCurrentUser: converted user=${user?.id}, email=${user?.email}")
        return user
    }

    /**
     * Fetch admin UIDs from Firestore.
     * Admins are stored in the 'admins' collection with document ID = user UID.
     *
     * Structure:
     * admins/
     *   {uid}/
     *     email: "admin@example.com"
     *     addedBy: "original-admin-uid"
     *     addedAt: timestamp
     */
    private suspend fun fetchAdminUids(): Set<String> {
        val now = System.currentTimeMillis()
        if (cachedAdminUids.isNotEmpty() && (now - lastAdminFetch) < ADMIN_CACHE_DURATION) {
            return cachedAdminUids
        }

        return try {
            val snapshot = firestore.collection(ADMINS_COLLECTION).get().await()
            val uids = snapshot.documents.mapNotNull { it.id }.toSet()
            cachedAdminUids = uids
            lastAdminFetch = now
            Log.d(TAG, "Fetched ${uids.size} admin UIDs from Firestore")
            uids
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching admins from Firestore: ${e.message}")
            cachedAdminUids // Return cached if fetch fails
        }
    }

    /**
     * Check if a user UID is an admin (synchronous, uses cache).
     * Falls back to hardcoded email list if Firestore check fails.
     */
    private fun isAdminSync(uid: String, email: String): Boolean {
        // First check cache
        if (cachedAdminUids.contains(uid)) {
            return true
        }
        // Fallback to hardcoded email list
        return User.isAdminEmail(email)
    }

    /**
     * Add a user as admin (call from Admin screen).
     */
    suspend fun addAdmin(uid: String, email: String): Result<Unit> {
        return try {
            val currentUser = firebaseAuth.currentUser
                ?: return Result.failure(Exception("Nao autenticado"))

            firestore.collection(ADMINS_COLLECTION).document(uid).set(
                mapOf(
                    "email" to email,
                    "addedBy" to currentUser.uid,
                    "addedAt" to System.currentTimeMillis()
                )
            ).await()

            // Update cache
            cachedAdminUids = cachedAdminUids + uid
            Log.d(TAG, "Added admin: $email ($uid)")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error adding admin: ${e.message}")
            Result.failure(e)
        }
    }

    /**
     * Remove a user from admins.
     */
    suspend fun removeAdmin(uid: String): Result<Unit> {
        return try {
            firestore.collection(ADMINS_COLLECTION).document(uid).delete().await()
            cachedAdminUids = cachedAdminUids - uid
            Log.d(TAG, "Removed admin: $uid")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error removing admin: ${e.message}")
            Result.failure(e)
        }
    }

    /**
     * Get all admins from Firestore.
     */
    suspend fun getAllAdmins(): List<Map<String, Any>> {
        return try {
            val snapshot = firestore.collection(ADMINS_COLLECTION).get().await()
            snapshot.documents.mapNotNull { doc ->
                val data = doc.data?.toMutableMap() ?: mutableMapOf()
                data["uid"] = doc.id
                data
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching all admins: ${e.message}")
            emptyList()
        }
    }

    /**
     * Refresh admin cache (call when entering Admin screen).
     */
    suspend fun refreshAdminCache() {
        lastAdminFetch = 0 // Force refresh
        fetchAdminUids()
    }

    private fun com.google.firebase.auth.FirebaseUser.toUser(): User {
        val userEmail = email ?: ""
        return User(
            id = uid,
            email = userEmail,
            displayName = displayName ?: "",
            photoUrl = photoUrl?.toString(),
            isPremium = false,
            isAdmin = isAdminSync(uid, userEmail),
            createdAt = metadata?.creationTimestamp ?: System.currentTimeMillis()
        )
    }
}
