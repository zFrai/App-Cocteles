package com.barcraft.app.model

/** Perfil público de un usuario (colección `users/{uid}` en Firestore). */
data class UserProfile(
    val uid: String,
    val name: String,
    val email: String = "",
    val role: Role = Role.CLIENTE,
    val banned: Boolean = false,
    val bio: String = "",
    val createdAt: Long = 0L,
    /**
     * Identificador único y fijo, sin la @ (p. ej. "laura4821"). Se asigna solo la primera vez
     * que la persona abre la app con esta versión (ver UserRepository.ensureTag). Vacío si aún no tiene.
     */
    val tag: String = ""
) {
    /** "@laura4821", o vacío si aún no tiene tag. */
    val handle: String get() = if (tag.isBlank()) "" else "@$tag"

    /**
     * Búsqueda de personas: "@lau" busca solo por tag (empieza por); cualquier otro texto
     * busca en el nombre o en el tag.
     */
    fun matches(query: String): Boolean {
        val q = query.trim()
        if (q.isEmpty()) return true
        if (q.startsWith("@")) {
            val t = q.removePrefix("@").lowercase()
            return t.isEmpty() || tag.startsWith(t)
        }
        return name.contains(q, ignoreCase = true) || tag.contains(q.lowercase())
    }

    val isAdmin: Boolean get() = role == Role.ADMIN
    val isBarman: Boolean get() = role == Role.BARMAN
}
