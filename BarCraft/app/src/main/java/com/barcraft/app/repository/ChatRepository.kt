package com.barcraft.app.repository

import com.barcraft.app.data.remote.Collections
import com.barcraft.app.data.remote.snapshotFlow
import com.barcraft.app.data.remote.toChatMessage
import com.barcraft.app.data.remote.toChatRoom
import com.barcraft.app.model.ChatMessage
import com.barcraft.app.model.ChatRoom
import com.barcraft.app.model.MessageType
import com.barcraft.app.model.UserProfile
import com.google.firebase.firestore.FieldPath
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

/** Contenido de un mensaje nuevo. */
data class OutgoingMessage(
    val type: MessageType,
    val text: String = "",
    val mediaId: String = "",
    val gifUrl: String = "",
    val durationMs: Long = 0L
)

/** Chat privado en tiempo real entre usuarios (Cloud Firestore). */
class ChatRepository(private val db: FirebaseFirestore) {

    private val chats get() = db.collection(Collections.CHATS)

    /** Mismo id para la misma pareja de usuarios, se abra desde donde se abra. */
    fun roomIdFor(uidA: String, uidB: String): String = listOf(uidA, uidB).sorted().joinToString("_")

    fun observeRooms(myUid: String): Flow<List<ChatRoom>> =
        chats.whereArrayContains("participants", myUid).snapshotFlow().map { snap ->
            snap.documents.map { it.toChatRoom() }.sortedByDescending { it.updatedAt }
        }

    fun observeRoom(roomId: String): Flow<ChatRoom?> =
        chats.document(roomId).snapshotFlow().map { if (it.exists()) it.toChatRoom() else null }

    fun observeMessages(roomId: String): Flow<List<ChatMessage>> =
        chats.document(roomId).collection(Collections.MESSAGES)
            .orderBy("createdAt", Query.Direction.ASCENDING)
            .limitToLast(300)
            .snapshotFlow()
            .map { snap -> snap.documents.map { it.toChatMessage() } }

    /** Crea (o reutiliza) la conversación con otro usuario y devuelve su id. */
    suspend fun openRoom(me: UserProfile, otherUid: String, otherName: String): String {
        val id = roomIdFor(me.uid, otherUid)
        chats.document(id).set(
            mapOf(
                "participants" to listOf(me.uid, otherUid).sorted(),
                "names" to mapOf(me.uid to me.name, otherUid to otherName)
            ),
            SetOptions.merge()
        ).await()
        return id
    }

    suspend fun send(roomId: String, me: UserProfile, message: OutgoingMessage) {
        val room = chats.document(roomId)
        val doc = room.collection(Collections.MESSAGES).document()
        val preview = ChatMessage(
            id = "", senderId = me.uid, senderName = me.name, type = message.type,
            text = message.text, createdAt = 0
        ).preview
        db.batch()
            .set(
                doc,
                mapOf(
                    "senderId" to me.uid,
                    "senderName" to me.name,
                    "type" to message.type.id,
                    "text" to message.text.trim(),
                    "mediaId" to message.mediaId,
                    "gifUrl" to message.gifUrl,
                    "durationMs" to message.durationMs,
                    "reactions" to emptyMap<String, String>(),
                    "createdAt" to FieldValue.serverTimestamp()
                )
            )
            .update(
                room,
                mapOf(
                    "lastMessage" to preview.trim().take(120),
                    "lastSenderId" to me.uid,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )
            .commit()
            .await()
    }

    /** Pone, cambia o quita (emoji = null) mi reacción a un mensaje. */
    suspend fun react(roomId: String, messageId: String, myUid: String, emoji: String?) {
        val doc = chats.document(roomId).collection(Collections.MESSAGES).document(messageId)
        doc.update(FieldPath.of("reactions", myUid), emoji ?: FieldValue.delete()).await()
    }

    suspend fun deleteMessage(roomId: String, messageId: String) {
        chats.document(roomId).collection(Collections.MESSAGES).document(messageId).delete().await()
    }
}
