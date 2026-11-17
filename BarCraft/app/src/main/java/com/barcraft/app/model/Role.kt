package com.barcraft.app.model

import androidx.annotation.StringRes
import com.barcraft.app.R
import com.barcraft.app.utils.Txt

/**
 * Tipos de usuario. Se guarda como texto en Firestore (campo `role` de `users/{uid}`).
 *
 * - BARMAN: profesional. Crea recetas y publica en la comunidad.
 * - CLIENTE: comensal. Ve, valora, comenta y chatea. Puede usar la app sin cuenta.
 * - ADMIN: administra la app (roles, bloqueos y moderación).
 */
enum class Role(val id: String, @StringRes val labelRes: Int, @StringRes val descriptionRes: Int) {
    BARMAN("barman", R.string.role_barman, R.string.role_barman_desc),
    CLIENTE("cliente", R.string.role_cliente, R.string.role_cliente_desc),
    ADMIN("admin", R.string.role_admin, R.string.role_admin_desc);

    /** Nombre visible (sale de strings.xml). */
    val label: String get() = Txt.get(labelRes)
    val description: String get() = Txt.get(descriptionRes)

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
