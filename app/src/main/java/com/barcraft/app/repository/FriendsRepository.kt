package com.barcraft.app.repository

import com.barcraft.app.data.remote.Collections
import com.barcraft.app.data.remote.snapshotFlow
import com.barcraft.app.data.remote.toFriendship
import com.barcraft.app.data.remote.toPrivateSettings
import com.barcraft.app.model.Friendship
import com.barcraft.app.model.PrivateSettings
import com.barcraft.app.model.UserProfile
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

/**
 * Amigos, favoritos y bloqueos.
 *
 * - La amistad es compartida: `friendships/{uidA_uidB}` con estado "pending" o "accepted".
 * - Favoritos y bloqueados son privados: `users/{uid}/private/settings` (solo los ve su dueño).
 * - Al bloquear también se marca la conversación (`chats/{id}.blockedBy`) para que nadie pueda escribir.
 */
class FriendsRepository(private val db: FirebaseFirestore) {

    private val friendships get() = db.collection(Collections.FRIENDSHIPS)

    private fun settingsDoc(uid: String) =
        db.collection(Collections.USERS).document(uid)
            .collection(Collections.PRIVATE).document(Collections.SETTINGS_DOC)

    fun pairId(a: String, b: String): String = listOf(a, b).sorted().joinToString("_")

    fun observeFriendships(myUid: String): Flow<List<Friendship>> =
        friendships.whereArrayContains("users", myUid).snapshotFlow()
            .map { snap -> snap.documents.map { it.toFriendship() } }

    fun observeSettings(myUid: String): Flow<PrivateSettings> =
        settingsDoc(myUid).snapshotFlow().map { it.toPrivateSettings() }

    suspend fun sendRequest(me: UserProfile, other: UserProfile) {
        friendships.document(pairId(me.uid, other.uid)).set(
            mapOf(
                "users" to listOf(me.uid, other.uid).sorted(),
                "names" to mapOf(me.uid to me.name, other.uid to other.name),
                "requesterId" to me.uid,
                "status" to "pending",
                "createdAt" to FieldValue.serverTimestamp()
            )
        ).await()
    }

    suspend fun accept(friendshipId: String) {
        friendships.document(friendshipId).update("status", "accepted").await()
    }

    /** Sirve para rechazar, cancelar una solicitud o eliminar a un amigo. */
    suspend fun remove(friendshipId: String) {
        friendships.document(friendshipId).delete().await()
    }

    suspend fun setFavorite(myUid: String, otherUid: String, favorite: Boolean) {
        val change = if (favorite) FieldValue.arrayUnion(otherUid) else FieldValue.arrayRemove(otherUid)
        settingsDoc(myUid).set(mapOf("favorites" to change), SetOptions.merge()).await()
    }

    suspend fun block(myUid: String, otherUid: String) {
        settingsDoc(myUid).set(
            mapOf(
                "blocked" to FieldValue.arrayUnion(otherUid),
                "favorites" to FieldValue.arrayRemove(otherUid)
            ),
            SetOptions.merge()
        ).await()
        runCatching { remove(pairId(myUid, otherUid)) }
        markRoom(myUid, otherUid, blocked = true)
    }

    suspend fun unblock(myUid: String, otherUid: String) {
        settingsDoc(myUid).set(mapOf("blocked" to FieldValue.arrayRemove(otherUid)), SetOptions.merge()).await()
        markRoom(myUid, otherUid, blocked = false)
    }

    private suspend fun markRoom(myUid: String, otherUid: String, blocked: Boolean) {
        val room = db.collection(Collections.CHATS).document(pairId(myUid, otherUid))
        val change = if (blocked) FieldValue.arrayUnion(myUid) else FieldValue.arrayRemove(myUid)
        // Si nunca habéis hablado, la conversación no existe y no hay nada que marcar.
        runCatching { room.update("blockedBy", change).await() }
    }
}
