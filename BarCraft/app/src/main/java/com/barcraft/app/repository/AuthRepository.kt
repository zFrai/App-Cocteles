package com.barcraft.app.repository

import com.barcraft.app.data.remote.Collections
import com.barcraft.app.data.remote.uidFlow
import com.barcraft.app.model.Role
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseAuthWeakPasswordException
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.FirebaseNetworkException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.tasks.await

/** Cuentas de usuario con Firebase Authentication (email + contraseña). */
class AuthRepository(
    private val auth: FirebaseAuth,
    private val db: FirebaseFirestore
) {
    val currentUid: String? get() = auth.currentUser?.uid

    fun observeUid(): Flow<String?> = auth.uidFlow()

    suspend fun login(email: String, password: String): Result<Unit> = runCatching {
        auth.signInWithEmailAndPassword(email.trim(), password).await()
        Unit
    }.mapError()

    suspend fun register(name: String, email: String, password: String, role: Role): Result<Unit> = runCatching {
        val result = auth.createUserWithEmailAndPassword(email.trim(), password).await()
        val user = requireNotNull(result.user)
        user.updateProfile(UserProfileChangeRequest.Builder().setDisplayName(name.trim()).build()).await()
        createProfile(user.uid, name.trim(), email.trim(), role)
    }.mapError()

    /** Crea el documento del usuario si no existe (por ejemplo, cuentas creadas desde la consola). */
    suspend fun ensureProfile(uid: String) {
        val user = auth.currentUser ?: return
        if (user.uid != uid) return
        val doc = db.collection(Collections.USERS).document(uid).get().await()
        if (!doc.exists()) {
            createProfile(uid, user.displayName ?: user.email?.substringBefore('@') ?: "Usuario", user.email.orEmpty(), Role.CLIENTE)
        }
    }

    private suspend fun createProfile(uid: String, name: String, email: String, role: Role) {
        db.collection(Collections.USERS).document(uid).set(
            mapOf(
                "name" to name,
                "email" to email,
                // Barman o Comensal. Admin solo lo puede dar otro Admin (lo impiden las reglas).
                "role" to (if (role == Role.ADMIN) Role.CLIENTE else role).id,
                "banned" to false,
                "bio" to "",
                "createdAt" to FieldValue.serverTimestamp()
            )
        ).await()
    }

    suspend fun sendPasswordReset(email: String): Result<Unit> = runCatching {
        auth.sendPasswordResetEmail(email.trim()).await()
        Unit
    }.mapError()

    fun logout() = auth.signOut()

    /** Comprueba la contraseña de la cuenta (para recuperar el PIN de bloqueo). */
    suspend fun verifyPassword(password: String): Result<Unit> = runCatching {
        val user = requireNotNull(auth.currentUser)
        val email = requireNotNull(user.email)
        user.reauthenticate(EmailAuthProvider.getCredential(email, password)).await()
        Unit
    }.mapError()

    private fun <T> Result<T>.mapError(): Result<T> =
        fold({ Result.success(it) }, { Result.failure(Exception(friendlyMessage(it))) })

    private fun friendlyMessage(e: Throwable): String = when (e) {
        is FirebaseAuthWeakPasswordException -> "La contraseña debe tener al menos 6 caracteres."
        is FirebaseAuthUserCollisionException -> "Ya existe una cuenta con ese email."
        is FirebaseAuthInvalidUserException -> "No hay ninguna cuenta con ese email."
        is FirebaseAuthInvalidCredentialsException -> "Email o contraseña incorrectos."
        is FirebaseNetworkException -> "Sin conexión. Revisa tu internet."
        else -> e.message ?: "Algo ha fallado. Inténtalo de nuevo."
    }
}
