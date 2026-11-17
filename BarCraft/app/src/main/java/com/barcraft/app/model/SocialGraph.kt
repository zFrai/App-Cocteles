package com.barcraft.app.model

/** Relación de amistad entre dos usuarios (`friendships/{uidA_uidB}`). */
data class Friendship(
    val id: String,
    val users: List<String>,
    val names: Map<String, String>,
    val requesterId: String,
    val accepted: Boolean,
    val createdAt: Long
) {
    fun otherId(myUid: String): String = users.firstOrNull { it != myUid } ?: myUid
    fun otherName(myUid: String): String = names[otherId(myUid)] ?: "Usuario"
    fun isIncomingFor(myUid: String): Boolean = !accepted && requesterId != myUid
}

/** Ajustes privados de cada usuario (`users/{uid}/private/settings`): solo los ve él. */
data class PrivateSettings(
    val favorites: List<String> = emptyList(),
    val blocked: List<String> = emptyList(),
    val notifications: NotificationPrefs = NotificationPrefs()
)

/**
 * Qué avisos quiere recibir cada uno. Se guardan ya en `users/{uid}/private/settings`; los avisos
 * push reales necesitan Cloud Functions (plan Blaze de Firebase) y llegarán más adelante.
 */
data class NotificationPrefs(
    val comments: Boolean = true,
    val messages: Boolean = true,
    val followers: Boolean = true
) {
    companion object {
        const val COMMENTS = "notifyComments"
        const val MESSAGES = "notifyMessages"
        const val FOLLOWERS = "notifyFollowers"
    }
}

/** Estado de la relación con otra persona, para pintar botones. */
enum class FriendState { NONE, REQUEST_SENT, REQUEST_RECEIVED, FRIENDS }
