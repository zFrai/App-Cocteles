package com.barcraft.app.repository

import androidx.room.withTransaction
import com.barcraft.app.data.local.AppDatabase
import com.barcraft.app.data.local.entity.CocktailEntity
import com.barcraft.app.data.local.entity.CocktailWithDetails
import com.barcraft.app.data.local.entity.IngredientEntity
import com.barcraft.app.data.local.entity.RatingEntity
import com.barcraft.app.data.local.entity.StepEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import org.json.JSONArray
import org.json.JSONObject

/** Datos de una receta tal como los rellena el editor. */
data class CocktailDraft(
    val id: Long = 0,
    val name: String = "",
    val description: String = "",
    val imageUri: String = "",
    val category: String = "Clásico",
    val difficulty: Int = 2,
    val prepTimeMinutes: Int = 5,
    val hasSmoke: Boolean = false,
    val hasGarnish: Boolean = false,
    val garnishDescription: String = "",
    val isFavorite: Boolean = false,
    val ingredients: List<IngredientDraft> = emptyList(),
    val steps: List<String> = emptyList()
)

data class IngredientDraft(val name: String, val amount: String, val type: String)

/** Única fuente de verdad de las recetas locales (Room). */
class CocktailRepository(private val db: AppDatabase) {

    private val dao = db.cocktailDao()

    fun observeAll(): Flow<List<CocktailWithDetails>> = dao.observeAll()

    fun observe(id: Long): Flow<CocktailWithDetails?> = dao.observeById(id)

    suspend fun get(id: Long): CocktailWithDetails? = dao.getById(id)

    /** Crea o actualiza la receta sin perder sus valoraciones. Devuelve el id. */
    suspend fun save(draft: CocktailDraft): Long = db.withTransaction {
        val existing = if (draft.id > 0) dao.getEntity(draft.id) else null
        val base = existing ?: CocktailEntity(name = draft.name)
        val entity = base.copy(
            name = draft.name.trim(),
            description = draft.description.trim(),
            imageUri = draft.imageUri.trim(),
            category = draft.category,
            difficulty = draft.difficulty,
            prepTimeMinutes = draft.prepTimeMinutes,
            hasSmoke = draft.hasSmoke,
            hasGarnish = draft.hasGarnish,
            garnishDescription = if (draft.hasGarnish) draft.garnishDescription.trim() else "",
            isFavorite = draft.isFavorite
        )
        val id = if (existing != null) {
            dao.updateCocktail(entity); entity.id
        } else {
            dao.insertCocktail(entity.copy(id = 0))
        }

        dao.deleteIngredients(id)
        dao.insertIngredients(draft.ingredients.mapIndexed { i, ing ->
            IngredientEntity(cocktailId = id, name = ing.name.trim(), amount = ing.amount.trim(), type = ing.type, position = i)
        })
        dao.deleteSteps(id)
        dao.insertSteps(draft.steps.filter { it.isNotBlank() }.mapIndexed { i, s ->
            StepEntity(cocktailId = id, position = i, instruction = s.trim())
        })
        id
    }

    suspend fun delete(cocktail: CocktailEntity) = dao.deleteCocktail(cocktail)

    suspend fun setFavorite(id: Long, favorite: Boolean) = dao.setFavorite(id, favorite)

    suspend fun addRating(cocktailId: Long, voterName: String, stars: Int, comment: String) {
        db.withTransaction {
            dao.insertRating(
                RatingEntity(
                    cocktailId = cocktailId,
                    voterName = voterName.trim().ifBlank { "Anónimo" },
                    stars = stars.coerceIn(1, 6),
                    comment = comment.trim()
                )
            )
            dao.refreshRatingStats(cocktailId)
        }
    }

    suspend fun deleteRating(cocktailId: Long, ratingId: Long) {
        db.withTransaction {
            dao.deleteRating(ratingId)
            dao.refreshRatingStats(cocktailId)
        }
    }

    /** Copia de seguridad de todas las recetas en JSON. */
    suspend fun exportJson(): String {
        val array = JSONArray()
        dao.observeAll().first().forEach { c ->
            array.put(JSONObject().apply {
                put("nombre", c.cocktail.name)
                put("descripcion", c.cocktail.description)
                put("categoria", c.cocktail.category)
                put("dificultad", c.cocktail.difficulty)
                put("tiempoMin", c.cocktail.prepTimeMinutes)
                put("humo", c.cocktail.hasSmoke)
                put("garnish", c.cocktail.hasGarnish)
                put("descripcionGarnish", c.cocktail.garnishDescription)
                put("imagen", c.cocktail.imageUri)
                put("ingredientes", JSONArray().apply {
                    c.sortedIngredients.forEach {
                        put(JSONObject().put("nombre", it.name).put("cantidad", it.amount).put("tipo", it.type))
                    }
                })
                put("pasos", JSONArray(c.sortedSteps.map { it.instruction }))
                put("valoraciones", JSONArray().apply {
                    c.sortedRatings.forEach {
                        put(JSONObject().put("votante", it.voterName).put("estrellas", it.stars).put("comentario", it.comment))
                    }
                })
            })
        }
        return array.toString(2)
    }
}
