package com.barcraft.app.model

/**
 * Quién puede ver una receta. Se guarda como texto en Room (`cocktails.visibility`) y en Firestore
 * (`users/{uid}/recipes/{id}.visibility`). Las privadas no se suben.
 */
enum class RecipeVisibility(val id: String, val label: String, val description: String) {
    PRIVATE("private", "Privada", "Solo tú, en este móvil"),
    FOLLOWERS("followers", "Seguidores", "Tus seguidores la ven en Comunidad"),
    PUBLIC("public", "Pública", "Cualquiera la ve en Comunidad");

    companion object {
        fun from(id: String?): RecipeVisibility = entries.firstOrNull { it.id == id } ?: PRIVATE
    }
}
