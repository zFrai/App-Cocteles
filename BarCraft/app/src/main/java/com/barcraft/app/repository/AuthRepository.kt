package com.barcraft.app.repository

import com.barcraft.app.R
import com.barcraft.app.data.remote.Collections
import com.barcraft.app.data.remote.uidFlow
import com.barcraft.app.model.Role
import com.barcraft.app.utils.Txt
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseAuthWeakPasswordException
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.FirebaseNetworkException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.tasks.await

/** Error de cuenta con un mensaje para mostrar. [wrongCredentials] = email o contraseña ya no valen. */
class AuthException(message: String, val wrongCredentials: Boolean = false) : Exception(message)

/** Cuentas de usuario con Firebase Authentication (email + contraseña o Google). */
class AuthRepository(
    private val auth: FirebaseAuth,
    private val db: FirebaseFirestore
) {
    val currentUid: String? get() = auth.currentUser?.uid

    /** Email de la sesión actual (viene de Firebase Auth, no del perfil público). */
    val currentEmail: String get() = auth.currentUser?.email.orEmpty()

    fun observeUid(): Flow<String?> = auth.uidFlow()

    /** ¿La cuenta actual tiene contraseña? (las creadas con Google no tienen). */
    val hasPasswordLogin: Boolean
        get() = auth.currentUser?.providerData?.any { it.providerId == EmailAuthProvider.PROVIDER_ID } == true

    /**
     * Mientras se crea el perfil al registrarse, [ensureProfile] no debe crearlo por su cuenta
     * (lo haría como Comensal y podría pisar el rol elegido).
     */
    @Volatile
    private var creatingProfile = false

    suspend fun login(email: String, password: String): Result<Unit> = runCatching {
        auth.signInWithEmailAndPassword(email.trim(), password).await()
        Unit
    }.mapError()

    suspend fun register(name: String, email: String, password: String, role: Role): Result<Unit> = runCatching {
        creatingProfile = true
        try {
            val result = auth.createUserWithEmailAndPassword(email.trim(), password).await()
            val user = requireNotNull(result.user)
            user.updateProfile(UserProfileChangeRequest.Builder().setDisplayName(name.trim()).build()).await()
            createProfile(user.uid, name.trim(), email.trim(), role)
        } finally {
            creatingProfile = false
        }
    }.mapError()

    /**
     * Entra (o crea la cuenta) con Google. Si es la primera vez, se crea el perfil con [role]
     * y el nombre de la cuenta de Google.
     */
    suspend fun signInWithGoogle(idToken: String, role: Role): Result<Unit> = runCatching {
        creatingProfile = true
        try {
            val result = auth.signInWithCredential(GoogleAuthProvider.getCredential(idToken, null)).await()
            val user = requireNotNull(result.user)
            val exists = db.collection(Collections.USERS).document(user.uid).get().await().exists()
            if (!exists) {
                val email = user.email.orEmpty()
                val name = user.displayName?.takeIf { it.isNotBlank() } ?: email.substringBefore('@').ifBlank { Txt.get(R.string.user_fallback_name) }
                createProfile(user.uid, name, email, role)
            }
        } finally {
            creatingProfile = false
        }
    }.mapError(google = true)

    /** Crea el documento del usuario si no existe (por ejemplo, cuentas creadas desde la consola). */
    suspend fun ensureProfile(uid: String) {
        if (creatingProfile) return
        val user = auth.currentUser ?: return
        if (user.uid != uid) return
        val doc = db.collection(Collections.USERS).document(uid).get().await()
        if (!doc.exists()) {
            createProfile(uid, user.displayName ?: user.email?.substringBefore('@') ?: Txt.get(R.string.user_fallback_name), user.email.orEmpty(), Role.CLIENTE)
        }
    }

    private suspend fun createProfile(uid: String, name: String, email: String, role: Role) {
        // El email no va en el perfil público (lo leería cualquier usuario): se guarda aparte en emails/{uid}.
        // Si falla (p. ej. reglas aún sin publicar) no se bloquea el registro: se reintenta al entrar.
        if (email.isNotBlank()) {
            runCatching { db.collection(Collections.EMAILS).document(uid).set(mapOf("email" to email)).await() }
        }
        db.collection(Collections.USERS).document(uid).set(
            mapOf(
                "name" to name,
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

    /** Recuperar el PIN en cuentas de Google: se confirma la misma cuenta de Google. */
    suspend fun verifyGoogle(idToken: String): Result<Unit> = runCatching {
        val user = requireNotNull(auth.currentUser)
        user.reauthenticate(GoogleAuthProvider.getCredential(idToken, null)).await()
        Unit
    }.mapError(google = true)

    private fun <T> Result<T>.mapError(google: Boolean = false): Result<T> =
        fold({ Result.success(it) }, {
            Result.failure(
                AuthException(
                    friendlyMessage(it, google),
                    wrongCredentials = !google && (it is FirebaseAuthInvalidCredentialsException || it is FirebaseAuthInvalidUserException)
                )
            )
        })

    private fun friendlyMessage(e: Throwable, google: Boolean): String = when {
        google && e is FirebaseAuthUserCollisionException ->
            Txt.get(R.string.auth_error_google_collision)
        google && e is FirebaseAuthInvalidUserException ->
            Txt.get(R.string.auth_error_google_wrong_account)
        google && e is FirebaseAuthInvalidCredentialsException ->
            Txt.get(R.string.auth_error_google_invalid)
        else -> friendlyMessage(e)
    }

    private fun friendlyMessage(e: Throwable): String = when (e) {
        is FirebaseAuthWeakPasswordException -> Txt.get(R.string.auth_error_short_password)
        is FirebaseAuthUserCollisionException -> Txt.get(R.string.auth_error_email_in_use)
        is FirebaseAuthInvalidUserException -> Txt.get(R.string.auth_error_no_account)
        is FirebaseAuthInvalidCredentialsException -> Txt.get(R.string.auth_error_wrong_credentials)
        is FirebaseNetworkException -> Txt.get(R.string.error_no_connection)
        else -> e.message ?: Txt.get(R.string.error_generic)
    }
}
