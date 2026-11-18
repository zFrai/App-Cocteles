package com.barcraft.app.repository

import com.barcraft.app.data.remote.Collections
import com.barcraft.app.data.remote.snapshotFlow
import com.barcraft.app.data.remote.snapshotFlowOrFail
import com.barcraft.app.data.remote.toFollow
import com.barcraft.app.data.remote.toFriendship
import com.barcraft.app.data.remote.toPrivateSettings
import com.barcraft.app.model.Follow
import com.barcraft.app.model.PrivateSettings
import com.barcraft.app.model.UserProfile
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

/**
 * Seguidores, favoritos y bloqueos.
 *
 * - Seguir es en un solo sentido: `follows/{followerUid_followedUid}` con estado "accepted" o, si la
 *   cuenta seguida es privada, "pending" hasta que la acepte.
 * - Favoritos y bloqueados son privados: `users/{uid}/private/settings` (solo los ve su dueño).
 * - Al bloquear se borran los seguimientos entre los dos y se marca la conversación
 *   (`chats/{id}.blockedBy`) para que nadie pueda escribir.
 */
class FriendsRepository(private val db: FirebaseFirestore) {

    private val follows get() = db.collection(Collections.FOLLOWS)

    private fun settingsDoc(uid: String) =
        db.collection(Collections.USERS).document(uid)
            .collection(Collections.PRIVATE).document(Collections.SETTINGS_DOC)

    fun pairId(a: String, b: String): String = listOf(a, b).sorted().joinToString("_")

    /** Id del seguimiento: quién sigue + "_" + a quién. Debe coincidir con firestore.rules. */
    fun followId(followerUid: String, followedUid: String): String = "${followerUid}_$followedUid"

    /** A quién sigo yo (y a quién he pedido seguir). Si Firestore la rechaza, el Flow falla (ver orOnProblem). */
    fun observeFollowing(myUid: String): Flow<List<Follow>> =
        follows.whereEqualTo("followerId", myUid).snapshotFlowOrFail()
            .map { snap -> snap.documents.map { it.toFollow() } }

    /** Quién me sigue (y quién me ha pedido seguirme). */
    fun observeFollowers(myUid: String): Flow<List<Follow>> =
        follows.whereEqualTo("followedId", myUid).snapshotFlowOrFail()
            .map { snap -> snap.documents.map { it.toFollow() } }

    fun observeSettings(myUid: String): Flow<PrivateSettings> =
        settingsDoc(myUid).snapshotFlow().map { it.toPrivateSettings() }

    /**
     * Sigue a [other]. Si su cuenta es privada queda como solicitud.
     * Si ya le sigo (o ya se lo he pedido) no hace nada: volver a escribir el documento sería
     * una modificación, y las reglas solo dejan modificarlo a la cuenta seguida (para aceptar).
     */
    suspend fun follow(me: UserProfile, other: UserProfile) {
        val doc = follows.document(followId(me.uid, other.uid))
        if (doc.get().await().exists()) return
        doc.set(
            mapOf(
                "followerId" to me.uid,
                "followedId" to other.uid,
                "status" to if (other.privateAccount) "pending" else "accepted",
                "createdAt" to FieldValue.serverTimestamp()
            )
        ).await()
    }

    /** Deja de seguir o cancela la solicitud enviada. */
    suspend fun unfollow(myUid: String, otherUid: String) {
        follows.document(followId(myUid, otherUid)).delete().await()
    }

    /** Acepta la solicitud de [followerUid] para seguirme. */
    suspend fun acceptFollower(myUid: String, followerUid: String) {
        follows.document(followId(followerUid, myUid)).update("status", "accepted").await()
    }

    /** Rechaza una solicitud o quita a alguien de mis seguidores. */
    suspend fun removeFollower(myUid: String, followerUid: String) {
        follows.document(followId(followerUid, myUid)).delete().await()
    }

    /**
     * Convierte las amistades antiguas en seguimientos, una sola vez por cuenta:
     * - amistad aceptada: sigo a la otra persona (ella hará lo mismo al entrar), así quedáis en seguimiento mutuo;
     * - solicitud que envié: pasa a ser seguimiento (o solicitud si su cuenta es privada).
     * Las solicitudes recibidas las convierte quien las envió. Las amistades antiguas se dejan como estaban.
     */
    suspend fun migrateFriendships(me: UserProfile, profiles: Map<String, UserProfile>) {
        val old = db.collection(Collections.FRIENDSHIPS).whereArrayContains("users", me.uid).get().await()
        for (doc in old.documents) {
            val f = doc.toFriendship()
            val other = profiles[f.otherId(me.uid)] ?: continue
            val accepted = f.accepted
            if (accepted || f.requesterId == me.uid) {
                follows.document(followId(me.uid, other.uid)).set(
                    mapOf(
                        "followerId" to me.uid,
                        "followedId" to other.uid,
                        // Una amistad aceptada se respeta aunque la cuenta sea privada (lo permiten las reglas).
                        "status" to if (accepted || !other.privateAccount) "accepted" else "pending",
                        "fromFriendship" to accepted,
                        "createdAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }
        }
        settingsDoc(me.uid).set(mapOf("followsMigrated" to true), SetOptions.merge()).await()
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
        runCatching { unfollow(myUid, otherUid) }
        runCatching { removeFollower(myUid, otherUid) }
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
