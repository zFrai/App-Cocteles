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
     * Identificador único y fijo, sin la @ (p. ej. "laura4821", que se muestra como "@laura #4821"). Se asigna solo la primera vez
     * que la persona abre la app con esta versión (ver UserRepository.ensureTag). Vacío si aún no tiene.
     */
    val tag: String = "",
    /** "Dónde encontrarme" (opcional, separado de la descripción). */
    val links: ProfileLinks = ProfileLinks(),
    /**
     * Cuenta privada: quien quiera seguirla tendrá que enviar una solicitud.
     * El seguimiento llega en una versión posterior; de momento solo se guarda la preferencia.
     */
    val privateAccount: Boolean = false,
    /** Foto de perfil: id del archivo en la colección `media` (vacío = se muestra la inicial). */
    val photoId: String = ""
) {
    companion object {
        const val BIO_MAX = 250
    }

    /** "@laura #4821" (nombre + 4 cifras), o vacío si aún no tiene tag. */
    val handle: String
        get() = when {
            tag.isBlank() -> ""
            tag.length > 4 && tag.takeLast(4).all(Char::isDigit) -> "@${tag.dropLast(4)} #${tag.takeLast(4)}"
            else -> "@$tag"
        }

    /**
     * Búsqueda de personas. "@lau", "@laura #48" o "laura#4821" buscan por tag (empieza por);
     * cualquier otro texto busca en el nombre o en el tag. No distingue tildes ni mayúsculas.
     */
    fun matches(query: String): Boolean {
        val q = query.trim()
        if (q.isEmpty()) return true
        val folded = fold(q)
        if (q.startsWith("@") || q.contains("#")) {
            val t = folded.replace("@", "").replace("#", "").replace(" ", "")
            return t.isEmpty() || fold(tag).startsWith(t)
        }
        return fold(name).contains(folded) || fold(tag).contains(folded.replace(" ", ""))
    }

    private fun fold(s: String): String =
        java.text.Normalizer.normalize(s, java.text.Normalizer.Form.NFD)
            .replace(Regex("\\p{M}+"), "")
            .lowercase()

    val isAdmin: Boolean get() = role == Role.ADMIN
    val isBarman: Boolean get() = role == Role.BARMAN
}
