package com.barcraft.app.repository

import com.barcraft.app.data.remote.Collections
import com.barcraft.app.data.remote.snapshotFlow
import com.barcraft.app.data.remote.toUserProfile
import com.barcraft.app.model.Role
import com.barcraft.app.model.UserProfile
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

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

    /** Solo un Admin puede hacerlo (lo garantizan las reglas de Firestore). */
    suspend fun setRole(uid: String, role: Role) {
        users.document(uid).update("role", role.id).await()
    }

    /** Solo un Admin puede hacerlo (lo garantizan las reglas de Firestore). */
    suspend fun setBanned(uid: String, banned: Boolean) {
        users.document(uid).update("banned", banned).await()
    }
}
