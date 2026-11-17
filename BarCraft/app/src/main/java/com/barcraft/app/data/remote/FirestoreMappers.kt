package com.barcraft.app.data.remote

import com.barcraft.app.model.ChatMessage
import com.barcraft.app.model.ChatRoom
import com.barcraft.app.model.Comment
import com.barcraft.app.model.Follow
import com.barcraft.app.model.Friendship
import com.barcraft.app.model.MessageType
import com.barcraft.app.model.NotificationPrefs
import com.barcraft.app.model.PrivateSettings
import com.barcraft.app.model.Post
import com.barcraft.app.model.ProfileLinks
import com.barcraft.app.model.Report
import com.barcraft.app.model.ReportReason
import com.barcraft.app.model.ReportStatus
import com.barcraft.app.model.Role
import com.barcraft.app.model.UserProfile
import com.google.firebase.firestore.DocumentSnapshot

// Conversión manual documento -> modelo. Evita depender de constructores vacíos.

private fun DocumentSnapshot.millis(field: String): Long =
    getTimestamp(field, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)?.toDate()?.time
        ?: System.currentTimeMillis()

@Suppress("UNCHECKED_CAST")
private fun DocumentSnapshot.stringList(field: String): List<String> =
    (get(field) as? List<*>)?.filterIsInstance<String>() ?: emptyList()

@Suppress("UNCHECKED_CAST")
private fun DocumentSnapshot.stringMap(field: String): Map<String, String> =
    (get(field) as? Map<*, *>)
        ?.mapNotNull { (k, v) -> if (k is String && v is String) k to v else null }
        ?.toMap() ?: emptyMap()

fun DocumentSnapshot.toUserProfile(): UserProfile? {
    if (!exists()) return null
    return UserProfile(
        uid = id,
        name = getString("name").orEmpty().ifBlank { "Sin nombre" },
        email = getString("email").orEmpty(),
        role = Role.from(getString("role")),
        banned = getBoolean("banned") ?: false,
        bio = getString("bio").orEmpty(),
        createdAt = millis("createdAt"),
        tag = getString("tag").orEmpty(),
        links = ProfileLinks.fromMap(stringMap("links")),
        privateAccount = getBoolean("privateAccount") ?: false
    )
}

fun DocumentSnapshot.toChatRoom(): ChatRoom = ChatRoom(
    id = id,
    participants = stringList("participants"),
    names = stringMap("names"),
    lastMessage = getString("lastMessage").orEmpty(),
    lastSenderId = getString("lastSenderId").orEmpty(),
    updatedAt = millis("updatedAt"),
    blockedBy = stringList("blockedBy")
)

fun DocumentSnapshot.toChatMessage(): ChatMessage = ChatMessage(
    id = id,
    senderId = getString("senderId").orEmpty(),
    senderName = getString("senderName").orEmpty(),
    type = MessageType.from(getString("type")),
    text = getString("text").orEmpty(),
    mediaId = getString("mediaId").orEmpty(),
    gifUrl = getString("gifUrl").orEmpty(),
    durationMs = getLong("durationMs") ?: 0L,
    reactions = stringMap("reactions"),
    createdAt = millis("createdAt")
)

fun DocumentSnapshot.toFollow(): Follow = Follow(
    id = id,
    followerId = getString("followerId").orEmpty(),
    followedId = getString("followedId").orEmpty(),
    accepted = getString("status") == "accepted",
    createdAt = millis("createdAt")
)

fun DocumentSnapshot.toFriendship(): Friendship = Friendship(
    id = id,
    users = stringList("users"),
    names = stringMap("names"),
    requesterId = getString("requesterId").orEmpty(),
    accepted = getString("status") == "accepted",
    createdAt = millis("createdAt")
)

fun DocumentSnapshot.toPrivateSettings(): PrivateSettings =
    if (!exists()) PrivateSettings()
    else PrivateSettings(
        favorites = stringList("favorites"),
        blocked = stringList("blocked"),
        followsMigrated = getBoolean("followsMigrated") ?: false,
        notifications = NotificationPrefs(
            comments = getBoolean(NotificationPrefs.COMMENTS) ?: true,
            messages = getBoolean(NotificationPrefs.MESSAGES) ?: true,
            followers = getBoolean(NotificationPrefs.FOLLOWERS) ?: true
        )
    )

fun DocumentSnapshot.toPost(): Post = Post(
    id = id,
    authorId = getString("authorId").orEmpty(),
    authorName = getString("authorName").orEmpty(),
    authorRole = Role.from(getString("authorRole")),
    text = getString("text").orEmpty(),
    imageUrl = getString("imageUrl").orEmpty(),
    mediaId = getString("mediaId").orEmpty(),
    cocktailName = getString("cocktailName").orEmpty(),
    cocktailSummary = getString("cocktailSummary").orEmpty(),
    likes = stringList("likes"),
    commentCount = (getLong("commentCount") ?: 0L).toInt().coerceAtLeast(0),
    createdAt = millis("createdAt")
)

fun DocumentSnapshot.toComment(): Comment = Comment(
    id = id,
    authorId = getString("authorId").orEmpty(),
    authorName = getString("authorName").orEmpty(),
    text = getString("text").orEmpty(),
    createdAt = millis("createdAt")
)

fun DocumentSnapshot.toReport(): Report = Report(
    id = id,
    reporterId = getString("reporterId").orEmpty(),
    reporterName = getString("reporterName").orEmpty(),
    reportedId = getString("reportedId").orEmpty(),
    reportedName = getString("reportedName").orEmpty(),
    roomId = getString("roomId").orEmpty(),
    reason = ReportReason.from(getString("reason")),
    details = getString("details").orEmpty(),
    messageId = getString("messageId").orEmpty(),
    messageText = getString("messageText").orEmpty(),
    status = ReportStatus.from(getString("status")),
    createdAt = millis("createdAt")
)
