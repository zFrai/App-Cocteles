package com.barcraft.app.model

/**
 * Tipos de usuario. Se guarda como texto en Firestore (campo `role` de `users/{uid}`).
 *
 * - BARMAN: profesional. Crea recetas y publica en la comunidad.
 * - CLIENTE: comensal. Ve, valora, comenta y chatea. Puede usar la app sin cuenta.
 * - ADMIN: administra la app (roles, bloqueos y moderación).
 */
enum class Role(val id: String, val label: String, val description: String) {
    BARMAN("barman", "Barman", "Creo cócteles y los comparto"),
    CLIENTE("cliente", "Comensal", "Descubro, valoro y pregunto"),
    ADMIN("admin", "Admin", "Administro BarCraft");

    companion object {
        /** Roles que se pueden elegir al registrarse (Admin solo lo da otro Admin). */
        val selfAssignable = listOf(BARMAN, CLIENTE)

        fun from(id: String?): Role = when (id) {
            "admin" -> ADMIN
            "barman", "user" -> BARMAN // "user" era el rol de la versión 2
            else -> CLIENTE
        }
    }
}

/**
 * Qué puede hacer cada rol. Todo está aquí para poder ajustarlo fácilmente mientras
 * se decide el papel final de los comensales. Recuerda reflejar los cambios en firestore.rules.
 */
object Permissions {
    fun canPublishPosts(role: Role?): Boolean = role == Role.BARMAN || role == Role.ADMIN
    fun canComment(role: Role?): Boolean = role != null
    fun canModerate(role: Role?): Boolean = role == Role.ADMIN
    fun canManageUsers(role: Role?): Boolean = role == Role.ADMIN
    /** Borrar valoraciones de un cóctel. Invitados y comensales solo pueden añadirlas. */
    fun canDeleteRatings(role: Role?): Boolean = role == Role.BARMAN || role == Role.ADMIN
}
