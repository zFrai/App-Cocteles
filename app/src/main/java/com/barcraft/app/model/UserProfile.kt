package com.barcraft.app.model

/** Perfil público de un usuario (colección `users/{uid}` en Firestore). */
data class UserProfile(
    val uid: String,
    val name: String,
    val email: String = "",
    val role: Role = Role.CLIENTE,
    val banned: Boolean = false,
    val bio: String = "",
    val createdAt: Long = 0L
) {
    val isAdmin: Boolean get() = role == Role.ADMIN
    val isBarman: Boolean get() = role == Role.BARMAN
}
