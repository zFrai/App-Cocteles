package com.barcraft.app.repository

import android.util.Log
import com.barcraft.app.data.local.entity.CocktailWithDetails
import com.barcraft.app.data.remote.Collections
import com.barcraft.app.data.remote.snapshotFlow
import com.barcraft.app.data.remote.snapshotFlowOrFail
import com.barcraft.app.data.remote.toSharedRecipe
import com.barcraft.app.model.RecipeVisibility
import com.barcraft.app.model.SharedRecipe
import com.barcraft.app.model.UserProfile
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart

/**
 * Recetas compartidas en Comunidad. Las recetas viven en el móvil (Room); al guardarlas como
 * Seguidores o Pública se sube una copia a `users/{uid}/recipes/{idLocal}`. Si pasan a Privada
 * o se borran, se quita la copia.
 */
class RecipeShareRepository(private val db: FirebaseFirestore) {

    private fun myRecipes(uid: String) =
        db.collection(Collections.USERS).document(uid).collection(Collections.RECIPES)

    /**
     * Sube, actualiza o quita la copia según la visibilidad de la receta.
     * No espera al servidor: Firestore guarda el cambio y lo envía cuando haya conexión.
     */
    fun sync(me: UserProfile, recipe: CocktailWithDetails) {
        val c = recipe.cocktail
        val doc = myRecipes(me.uid).document(c.id.toString())
        val visibility = RecipeVisibility.from(c.visibility)
        if (visibility == RecipeVisibility.PRIVATE) {
            doc.delete().addOnFailureListener(::logFailure) // borrar algo que no existe no da error
            return
        }
        doc.set(
            mapOf(
                "authorId" to me.uid,
                "authorName" to me.name,
                "authorTag" to me.tag,
                "visibility" to visibility.id,
                "name" to c.name.take(MAX_NAME),
                "description" to c.description.take(MAX_TEXT),
                "category" to c.category,
                "difficulty" to c.difficulty,
                "prepTimeMinutes" to c.prepTimeMinutes,
                "hasSmoke" to c.hasSmoke,
                "garnishDescription" to if (c.hasGarnish) c.garnishDescription.take(MAX_TEXT) else "",
                // Las fotos del móvil (file://) no se pueden ver desde otro móvil: solo se comparten URLs.
                "imageUrl" to c.imageUri.takeIf { it.startsWith("http://") || it.startsWith("https://") }.orEmpty(),
                "ingredients" to recipe.sortedIngredients.take(MAX_ITEMS).map {
                    mapOf("name" to it.name, "amount" to it.amount, "type" to it.type)
                },
                "steps" to recipe.sortedSteps.take(MAX_ITEMS).map { it.instruction.take(MAX_TEXT) },
                "updatedAt" to FieldValue.serverTimestamp()
            )
        ).addOnFailureListener(::logFailure)
    }

    /** Al borrar la receta del móvil también se quita de Comunidad. */
    fun unshare(myUid: String, localId: Long) {
        myRecipes(myUid).document(localId.toString()).delete().addOnFailureListener(::logFailure)
    }

    private fun logFailure(e: Exception) {
        Log.w("BarCraft", "No se pudo sincronizar la receta compartida: ${e.message}")
    }

    fun observe(authorId: String, recipeId: String): Flow<SharedRecipe?> =
        myRecipes(authorId).document(recipeId).snapshotFlow()
            .map { if (it.exists()) it.toSharedRecipe() else null }

    /**
     * Comunidad: las públicas de todo el mundo + las de "Seguidores" de las cuentas que sigo.
     * Emite null mientras carga. Las más recientes primero.
     *
     * Si Firestore rechaza la consulta de las públicas (lo normal la primera vez: falta el índice de
     * grupo en `recipes.visibility`), el Flow falla con ese error para que la pantalla lo muestre.
     * Si falla la de una cuenta seguida, solo se omiten sus recetas.
     */
    @OptIn(ExperimentalCoroutinesApi::class)
    fun observeCommunity(myUid: String, followingIds: Flow<List<String>>): Flow<List<SharedRecipe>?> {
        val publicRecipes = db.collectionGroup(Collections.RECIPES)
            .whereEqualTo("visibility", RecipeVisibility.PUBLIC.id)
            .limit(MAX_PUBLIC)
            .snapshotFlowOrFail()
            .map { snap -> snap.documents.map { it.toSharedRecipe() } }

        val forFollowers = followingIds.flatMapLatest { ids ->
            val flows = ids.take(MAX_FOLLOWED).map { authorId ->
                myRecipes(authorId).whereEqualTo("visibility", RecipeVisibility.FOLLOWERS.id).snapshotFlowOrFail()
                    .map { snap -> snap.documents.map { it.toSharedRecipe() } }
                    .onStart { emit(emptyList()) }
                    .catch { emit(emptyList()) } // ya queda en Logcat; el resto de recetas se ve igual
            }
            if (flows.isEmpty()) flowOf(emptyList()) else combine(flows) { lists -> lists.toList().flatten() }
        }.onStart { emit(emptyList()) }

        return combine(publicRecipes, forFollowers) { pub, fol ->
            (pub + fol)
                .filter { it.authorId != myUid }
                .distinctBy { it.authorId + "/" + it.id }
                .sortedByDescending { it.updatedAt }
        }.map<List<SharedRecipe>, List<SharedRecipe>?> { it }.onStart { emit(null) }
    }

    private companion object {
        const val MAX_NAME = 100
        const val MAX_TEXT = 1000
        const val MAX_ITEMS = 40
        const val MAX_PUBLIC = 100L
        /** Cuántas cuentas seguidas se escuchan a la vez (un listener por cuenta). */
        const val MAX_FOLLOWED = 50
    }
}
