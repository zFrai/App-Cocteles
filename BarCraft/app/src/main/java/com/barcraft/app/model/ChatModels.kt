package com.barcraft.app.model

import com.barcraft.app.R
import com.barcraft.app.utils.Txt

/** Conversación privada entre dos usuarios (`chats/{roomId}`). */
data class ChatRoom(
    val id: String,
    val participants: List<String>,
    val names: Map<String, String>,
    val lastMessage: String = "",
    val lastSenderId: String = "",
    val updatedAt: Long = 0L,
    /** Quién ha bloqueado esta conversación. Si no está vacío, no se pueden enviar mensajes. */
    val blockedBy: List<String> = emptyList()
) {
    fun otherId(myUid: String): String = participants.firstOrNull { it != myUid } ?: myUid
    fun otherName(myUid: String): String = names[otherId(myUid)] ?: Txt.get(R.string.user_fallback_name)
    val isBlocked: Boolean get() = blockedBy.isNotEmpty()
}

enum class MessageType(val id: String) {
    TEXT("text"), IMAGE("image"), AUDIO("audio"), GIF("gif"), STICKER("sticker");

    companion object {
        fun from(id: String?): MessageType = entries.firstOrNull { it.id == id } ?: TEXT
    }
}

/** Mensaje dentro de una conversación (`chats/{roomId}/messages/{id}`). */
data class ChatMessage(
    val id: String,
    val senderId: String,
    val senderName: String,
    val type: MessageType = MessageType.TEXT,
    val text: String = "",
    /** Foto o audio guardado en la colección `media` (se descarga al móvil al verlo). */
    val mediaId: String = "",
    /** GIF o sticker de GIPHY. */
    val gifUrl: String = "",
    val durationMs: Long = 0L,
    /** uid -> emoji. Cada persona puede dejar una reacción por mensaje. */
    val reactions: Map<String, String> = emptyMap(),
    val createdAt: Long
) {
    val preview: String
        get() = when (type) {
            MessageType.TEXT -> text
            MessageType.IMAGE -> Txt.get(R.string.message_preview_image)
            MessageType.AUDIO -> Txt.get(R.string.message_preview_audio)
            MessageType.GIF -> Txt.get(R.string.message_preview_gif)
            MessageType.STICKER -> Txt.get(R.string.message_preview_sticker)
        }
}

val REACTION_EMOJIS = listOf("❤️", "😂", "🔥", "🍸", "👏", "😮", "😢", "👍")

/** GIF o sticker devuelto por GIPHY. */
data class GiphyItem(val id: String, val title: String, val previewUrl: String, val url: String)
