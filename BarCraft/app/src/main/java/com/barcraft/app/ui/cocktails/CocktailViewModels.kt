package com.barcraft.app.ui.cocktails

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.barcraft.app.data.local.entity.CocktailEntity
import com.barcraft.app.data.local.entity.CocktailWithDetails
import com.barcraft.app.repository.CocktailDraft
import com.barcraft.app.repository.CocktailRepository
import com.barcraft.app.repository.IngredientDraft
import com.barcraft.app.utils.ImageUtils
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

// ─────────────────────────────────────────────────────────────────────
//  Lista
// ─────────────────────────────────────────────────────────────────────

data class CocktailListState(
    val all: List<CocktailWithDetails> = emptyList(),
    val visible: List<CocktailWithDetails> = emptyList(),
    val query: String = "",
    val category: String? = null,
    val favoritesOnly: Boolean = false,
    val loaded: Boolean = false
)

class CocktailListViewModel(private val repo: CocktailRepository) : ViewModel() {

    private val query = MutableStateFlow("")
    private val category = MutableStateFlow<String?>(null)
    private val favoritesOnly = MutableStateFlow(false)

    val state: StateFlow<CocktailListState> =
        combine(repo.observeAll(), query, category, favoritesOnly) { all, q, cat, fav ->
            val needle = q.trim()
            CocktailListState(
                all = all,
                visible = all.filter { c ->
                    (!fav || c.cocktail.isFavorite) &&
                        (cat == null || c.cocktail.category == cat) &&
                        (needle.isEmpty() ||
                            c.cocktail.name.contains(needle, true) ||
                            c.cocktail.description.contains(needle, true) ||
                            c.ingredients.any { it.name.contains(needle, true) })
                },
                query = q,
                category = cat,
                favoritesOnly = fav,
                loaded = true
            )
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), CocktailListState())

    fun setQuery(value: String) { query.value = value }
    fun setCategory(value: String?) { category.value = value }
    fun toggleFavoritesOnly() { favoritesOnly.update { !it } }

    fun toggleFavorite(c: CocktailEntity) = viewModelScope.launch { repo.setFavorite(c.id, !c.isFavorite) }

    fun delete(c: CocktailEntity) = viewModelScope.launch {
        repo.delete(c)
        ImageUtils.deleteLocal(c.imageUri)
    }
}

// ─────────────────────────────────────────────────────────────────────
//  Detalle
// ─────────────────────────────────────────────────────────────────────

class CocktailDetailViewModel(
    private val repo: CocktailRepository,
    cocktailId: Long,
    /** Con cuenta: quita la copia de Comunidad al borrar la receta. Null sin cuenta. */
    private val unshare: ((Long) -> Unit)? = null
) : ViewModel() {

    val cocktail: StateFlow<CocktailWithDetails?> =
        repo.observe(cocktailId).stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    fun toggleFavorite() = viewModelScope.launch {
        cocktail.value?.cocktail?.let { repo.setFavorite(it.id, !it.isFavorite) }
    }

    fun addRating(voter: String, stars: Int, comment: String) = viewModelScope.launch {
        cocktail.value?.cocktail?.let { repo.addRating(it.id, voter, stars, comment) }
    }

    fun deleteRating(ratingId: Long) = viewModelScope.launch {
        cocktail.value?.cocktail?.let { repo.deleteRating(it.id, ratingId) }
    }

    fun delete(onDone: () -> Unit) = viewModelScope.launch {
        cocktail.value?.cocktail?.let {
            repo.delete(it)
            ImageUtils.deleteLocal(it.imageUri)
            if (it.visibility != "private") unshare?.invoke(it.id)
        }
        onDone()
    }
}

// ─────────────────────────────────────────────────────────────────────
//  Editor
// ─────────────────────────────────────────────────────────────────────

class CocktailEditorViewModel(
    private val repo: CocktailRepository,
    private val cocktailId: Long,
    /** Con cuenta: sube/quita la copia en Comunidad según la visibilidad. Null sin cuenta. */
    private val share: ((CocktailWithDetails) -> Unit)? = null
) : ViewModel() {

    private val _draft = MutableStateFlow(CocktailDraft())
    val draft: StateFlow<CocktailDraft> = _draft.asStateFlow()

    private val _loading = MutableStateFlow(cocktailId > 0)
    val loading: StateFlow<Boolean> = _loading.asStateFlow()

    private val _errors = MutableSharedFlow<String>()
    val errors: SharedFlow<String> = _errors

    val isEditing: Boolean get() = cocktailId > 0

    init {
        if (cocktailId > 0) viewModelScope.launch {
            repo.get(cocktailId)?.let { d ->
                val c = d.cocktail
                _draft.value = CocktailDraft(
                    id = c.id,
                    name = c.name,
                    description = c.description,
                    imageUri = c.imageUri,
                    category = c.category,
                    difficulty = c.difficulty,
                    prepTimeMinutes = c.prepTimeMinutes,
                    hasSmoke = c.hasSmoke,
                    hasGarnish = c.hasGarnish,
                    garnishDescription = c.garnishDescription,
                    isFavorite = c.isFavorite,
                    visibility = c.visibility,
                    ingredients = d.sortedIngredients.map { IngredientDraft(it.name, it.amount, it.type) },
                    steps = d.sortedSteps.map { it.instruction }
                )
            }
            _loading.value = false
        }
    }

    fun update(change: CocktailDraft.() -> CocktailDraft) = _draft.update(change)

    fun addIngredient(ingredient: IngredientDraft) = update { copy(ingredients = ingredients + ingredient) }
    fun updateIngredient(index: Int, ingredient: IngredientDraft) =
        update { copy(ingredients = ingredients.toMutableList().also { it[index] = ingredient }) }
    fun removeIngredient(index: Int) =
        update { copy(ingredients = ingredients.toMutableList().also { it.removeAt(index) }) }

    fun addStep(text: String) = update { copy(steps = steps + text) }
    fun updateStep(index: Int, text: String) = update { copy(steps = steps.toMutableList().also { it[index] = text }) }
    fun removeStep(index: Int) = update { copy(steps = steps.toMutableList().also { it.removeAt(index) }) }
    fun moveStep(from: Int, to: Int) = update {
        if (to !in steps.indices) this
        else copy(steps = steps.toMutableList().also { list -> list.add(to, list.removeAt(from)) })
    }

    fun pickImage(context: Context, uri: Uri) = viewModelScope.launch {
        val stored = ImageUtils.copyToAppStorage(context, uri)
        if (stored == null) _errors.emit("No se pudo copiar la imagen") else update { copy(imageUri = stored) }
    }

    fun save(onSaved: (Long) -> Unit) = viewModelScope.launch {
        val d = _draft.value
        when {
            d.name.isBlank() -> _errors.emit("El nombre es obligatorio")
            d.ingredients.isEmpty() -> _errors.emit("Añade al menos un ingrediente")
            else -> {
                val id = repo.save(d)
                share?.let { publish -> repo.get(id)?.let(publish) }
                onSaved(id)
            }
        }
    }
}
