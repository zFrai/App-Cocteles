package com.barcraft.app.model

/** Ingrediente de una receta compartida. */
data class SharedIngredient(val name: String, val amount: String, val type: String)

/**
 * Receta que alguien ha compartido como Seguidores o Pública (`users/{authorId}/recipes/{id}`).
 * El id del documento es el id local (Room) de la receta en el móvil de su autor.
 */
data class SharedRecipe(
    val id: String,
    val authorId: String,
    val authorName: String,
    /** Tag del autor sin @ (p. ej. "laura4821"). */
    val authorTag: String,
    val visibility: RecipeVisibility,
    val name: String,
    val description: String,
    val category: String,
    val difficulty: Int,
    val prepTimeMinutes: Int,
    val hasSmoke: Boolean,
    val garnishDescription: String,
    /** Solo URLs http(s): las fotos guardadas en el móvil no se suben. */
    val imageUrl: String,
    val ingredients: List<SharedIngredient>,
    val steps: List<String>,
    val updatedAt: Long
) {
    /** "@laura #4821", o vacío. */
    val authorHandle: String
        get() = UserProfile(uid = authorId, name = authorName, tag = authorTag).handle
}
