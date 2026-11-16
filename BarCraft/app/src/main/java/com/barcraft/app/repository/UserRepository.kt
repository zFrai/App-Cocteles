package com.barcraft.app.repository

import com.barcraft.app.data.remote.Collections
import com.barcraft.app.data.remote.snapshotFlow
import com.barcraft.app.data.remote.toUserProfile
import com.barcraft.app.model.Role
import com.barcraft.app.model.UserProfile
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import java.text.Normalizer
import kotlin.random.Random

/** Perfiles de usuario, roles y bloqueos. */
class UserRepository(private val db: FirebaseFirestore) {

    private val users get() = db.collection(Collections.USERS)

    fun observeUser(uid: String): Flow<UserProfile?> =
        users.document(uid).snapshotFlow().map { it.toUserProfile() }

    fun observeAll(): Flow<List<UserProfile>> =
        users.snapshotFlow().map { snap ->
            snap.documents.mapNotNull { it.toUserProfile() }.sortedBy { it.name.lowercase() }
        }

    suspend fun updateProfile(uid: String, name: String, bio: String) {
        users.document(uid).update(mapOf("name" to name.trim(), "bio" to bio.trim())).await()
    }

    /**
     * Da a la persona su @tag si aún no tiene (cuentas nuevas y las que ya existían).
     *
     * El tag es el nombre sin tildes ni espacios + 4 cifras al azar (p. ej. "Laura Gómez" -> "lauragomez4821").
     * Para que sea único se reserva en `tags/{tag}` dentro de la misma transacción que lo guarda en el
     * perfil: si ya lo tiene otra persona se prueban otras cifras. Una vez asignado no cambia, aunque
     * se cambie el nombre (las reglas de Firestore lo impiden).
     */
    suspend fun ensureTag(uid: String, name: String): String {
        val base = tagBase(name)
        repeat(MAX_TAG_ATTEMPTS) { attempt ->
            // Primero 4 cifras; si hay mala suerte varias veces seguidas, 6.
            val digits = if (attempt < 5) Random.nextInt(1000, 10000) else Random.nextInt(100000, 1000000)
            val candidate = "$base$digits"
            val assigned: String? = db.runTransaction { tx ->
                val userRef = users.document(uid)
                val current = tx.get(userRef).getString("tag")
                if (!current.isNullOrBlank()) return@runTransaction current
                val tagRef = db.collection(Collections.TAGS).document(candidate)
                if (tx.get(tagRef).exists()) return@runTransaction null
                tx.set(tagRef, mapOf("uid" to uid, "createdAt" to FieldValue.serverTimestamp()))
                tx.update(userRef, "tag", candidate)
                candidate
            }.await()
            if (assigned != null) return assigned
        }
        error("No se pudo asignar un tag a $uid")
    }

    /** Solo un Admin puede hacerlo (lo garantizan las reglas de Firestore). */
    suspend fun setRole(uid: String, role: Role) {
        users.document(uid).update("role", role.id).await()
    }

    /** Solo un Admin puede hacerlo (lo garantizan las reglas de Firestore). */
    suspend fun setBanned(uid: String, banned: Boolean) {
        users.document(uid).update("banned", banned).await()
    }

    private companion object {
        const val MAX_TAG_ATTEMPTS = 8

        /** "Laura Gómez" -> "lauragomez". Solo a-z y 0-9, máx. 12 letras. Debe cumplir la regla de firestore.rules. */
        fun tagBase(name: String): String {
            val plain = Normalizer.normalize(name, Normalizer.Form.NFD)
                .replace(Regex("\\p{M}+"), "")
                .lowercase()
                .replace(Regex("[^a-z0-9]"), "")
                .take(12)
            return if (plain.length >= 3) plain else "barcraft"
        }
    }
}
