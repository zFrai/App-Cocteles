package com.barcraft.app.model

/** Perfil público de un usuario (colección `users/{uid}` en Firestore). */
data class UserProfile(
    val uid: String,
    val name: String,
    val email: String = "",
    val role: Role = Role.CLIENTE,
    val banned: Boolean = false,
    /** Descripción, máximo [BIO_MAX] caracteres. */
    val bio: String = "",
    val createdAt: Long = 0L,
    /**
     * Identificador único y fijo, sin la @ (p. ej. "laura4821"). Se asigna solo la primera vez
     * que la persona abre la app con esta versión (ver UserRepository.ensureTag). Vacío si aún no tiene.
     */
    val tag: String = "",
    /** "Dónde encontrarme" (opcional, separado de la descripción). */
    val links: ProfileLinks = ProfileLinks(),
    /**
     * Cuenta privada: quien quiera seguirla tendrá que enviar una solicitud.
     * El seguimiento llega en una versión posterior; de momento solo se guarda la preferencia.
     */
    val privateAccount: Boolean = false
) {
    companion object {
        const val BIO_MAX = 250
    }

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
