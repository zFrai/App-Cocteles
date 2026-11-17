package com.barcraft.app.model

/**
 * Seguimiento en un solo sentido (`follows/{followerUid_followedUid}`), como en Instagram.
 * Si la cuenta seguida es privada empieza como solicitud ([accepted] = false) hasta que la acepta.
 */
data class Follow(
    val id: String,
    val followerId: String,
    val followedId: String,
    val accepted: Boolean,
    val createdAt: Long
)

/** Estado de un seguimiento visto desde un lado. */
enum class FollowState { NONE, REQUESTED, FOLLOWING }

/**
 * Amistad del sistema anterior (`friendships/{uidA_uidB}`). Ya no se crean: al entrar se convierten
 * en seguimientos (ver FriendsRepository.migrateFriendships).
 */
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
    /** true cuando las amistades antiguas ya se han convertido en seguimientos. */
    val followsMigrated: Boolean = false,
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

