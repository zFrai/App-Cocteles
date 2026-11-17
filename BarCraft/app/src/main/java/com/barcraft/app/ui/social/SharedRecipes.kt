@file:OptIn(ExperimentalMaterial3Api::class)

package com.barcraft.app.ui.social

import com.barcraft.app.ui.components.FitText
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.Group
import androidx.compose.material.icons.outlined.LocalBar
import androidx.compose.material.icons.outlined.Public
import androidx.compose.material.icons.outlined.SaveAlt
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.barcraft.app.AppContainer
import com.barcraft.app.model.RecipeVisibility
import com.barcraft.app.model.SharedRecipe
import com.barcraft.app.repository.CocktailDraft
import com.barcraft.app.repository.IngredientDraft
import com.barcraft.app.ui.components.CocktailImage
import com.barcraft.app.ui.components.DifficultyDots
import com.barcraft.app.ui.components.EmptyState
import com.barcraft.app.ui.components.LoadingBox
import com.barcraft.app.ui.components.SectionCard
import com.barcraft.app.ui.components.SectionTitle
import com.barcraft.app.ui.components.UserAvatar
import com.barcraft.app.utils.appViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn

/** Tarjeta de una receta compartida en Comunidad. */
@Composable
fun SharedRecipeCard(recipe: SharedRecipe, onOpen: () -> Unit) {
    ElevatedCard(Modifier.fillMaxWidth().clickable(onClick = onOpen)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                UserAvatar(recipe.authorName, size = 32.dp)
                Spacer(Modifier.width(8.dp))
                Column(Modifier.weight(1f)) {
                    Text(recipe.authorName, style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    if (recipe.authorHandle.isNotBlank()) Text(
                        recipe.authorHandle,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                VisibilityLabel(recipe.visibility)
            }
            if (recipe.imageUrl.isNotBlank()) CocktailImage(
                recipe.imageUrl, recipe.name,
                Modifier.fillMaxWidth().aspectRatio(16f / 9f).clip(RoundedCornerShape(12.dp))
            )
            Text(recipe.name, style = MaterialTheme.typography.titleLarge)
            if (recipe.description.isNotBlank()) Text(
                recipe.description, style = MaterialTheme.typography.bodyMedium,
                maxLines = 2, overflow = TextOverflow.Ellipsis
            )
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(recipe.category, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                DifficultyDots(recipe.difficulty)
                Icon(Icons.Outlined.Timer, null, Modifier.size(16.dp))
                Text("${recipe.prepTimeMinutes} min", style = MaterialTheme.typography.labelMedium)
                Text("${recipe.ingredients.size} ingredientes", style = MaterialTheme.typography.labelMedium)
            }
        }
    }
}

@Composable
private fun VisibilityLabel(visibility: RecipeVisibility) {
    val icon = if (visibility == RecipeVisibility.PUBLIC) Icons.Outlined.Public else Icons.Outlined.Group
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, null, Modifier.size(14.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.width(4.dp))
        Text(visibility.label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

/** Lista de recetas de Comunidad. */
@Composable
fun SharedRecipeList(recipes: List<SharedRecipe>?, onOpen: (SharedRecipe) -> Unit, modifier: Modifier = Modifier) {
    when {
        recipes == null -> LoadingBox(modifier)
        recipes.isEmpty() -> EmptyState(
            icon = Icons.Outlined.LocalBar,
            title = "Todavía no hay recetas",
            message = "Aquí salen las recetas públicas y las que comparten con sus seguidores las cuentas que sigues. " +
                "Para compartir una tuya, edítala y elige Seguidores o Pública.",
            modifier = modifier
        )
        else -> LazyColumn(
            modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 96.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            itemsIndexed(recipes, key = { _, r -> r.authorId + "/" + r.id }) { _, r ->
                SharedRecipeCard(r, onOpen = { onOpen(r) })
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────
//  Detalle de una receta compartida
// ─────────────────────────────────────────────────────────────────────

class SharedRecipeViewModel(
    private val container: AppContainer,
    authorId: String,
    recipeId: String
) : ViewModel() {

    /** null = cargando; Result.failure = ya no existe o no tienes permiso. */
    val recipe: StateFlow<Result<SharedRecipe>?> =
        container.recipeShareRepository.observe(authorId, recipeId)
            .map<SharedRecipe?, Result<SharedRecipe>?> { r ->
                if (r != null) Result.success(r) else Result.failure(NoSuchElementException())
            }
            .onStart { emit(null) }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    private val _messages = MutableSharedFlow<String>()
    val messages: SharedFlow<String> = _messages

    /** Copia la receta a "Mis recetas" (en el móvil, como Privada). */
    fun saveCopy(r: SharedRecipe) = launchSafely(_messages, "No se pudo guardar la receta") {
        container.cocktailRepository.save(
            CocktailDraft(
                name = r.name,
                description = buildString {
                    append(r.description)
                    if (isNotEmpty()) append("\n\n")
                    append("Receta de ${r.authorName}${if (r.authorHandle.isNotBlank()) " (${r.authorHandle})" else ""}.")
                },
                imageUri = r.imageUrl,
                category = r.category,
                difficulty = r.difficulty,
                prepTimeMinutes = r.prepTimeMinutes,
                hasSmoke = r.hasSmoke,
                hasGarnish = r.garnishDescription.isNotBlank(),
                garnishDescription = r.garnishDescription,
                visibility = RecipeVisibility.PRIVATE.id,
                ingredients = r.ingredients.map { IngredientDraft(it.name, it.amount, it.type) },
                steps = r.steps
            )
        )
        _messages.emit("Guardada en tus recetas")
    }
}

@Composable
fun SharedRecipeScreen(authorId: String, recipeId: String, onBack: () -> Unit) {
    val vm = appViewModel(key = "shared_${authorId}_$recipeId") { SharedRecipeViewModel(it, authorId, recipeId) }
    val state by vm.recipe.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    LaunchedEffect(Unit) { vm.messages.collect { snackbar.showSnackbar(it) } }
    val recipe = state?.getOrNull()

    Scaffold(
        contentWindowInsets = WindowInsets(0),
        snackbarHost = { SnackbarHost(snackbar) },
        topBar = {
            TopAppBar(
                title = { FitText(recipe?.name ?: "Receta") },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Volver") } }
            )
        },
        floatingActionButton = {
            if (recipe != null) ExtendedFloatingActionButton(
                onClick = { vm.saveCopy(recipe) },
                icon = { Icon(Icons.Outlined.SaveAlt, null) },
                text = { FitText("Guardar en mis recetas") }
            )
        }
    ) { padding ->
        when {
            state == null -> LoadingBox(Modifier.padding(padding))
            recipe == null -> EmptyState(
                Icons.Outlined.LocalBar, "Receta no disponible",
                "Su autor la ha borrado o la ha hecho privada.",
                modifier = Modifier.padding(padding)
            )
            else -> LazyColumn(
                Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 96.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item { SharedRecipeCard(recipe, onOpen = {}) }
                item {
                    SectionCard {
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            SectionTitle("Ingredientes")
                            recipe.ingredients.forEach { ing ->
                                Row {
                                    Text(ing.name, Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge)
                                    Text(ing.amount, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.primary)
                                }
                            }
                            if (recipe.garnishDescription.isNotBlank()) Text(
                                "Garnish: ${recipe.garnishDescription}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            if (recipe.hasSmoke) Text(
                                "Con efecto humo",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
                if (recipe.steps.isNotEmpty()) item {
                    SectionCard {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            SectionTitle("Procedimiento")
                            recipe.steps.forEachIndexed { i, step ->
                                Text("${i + 1}. $step", style = MaterialTheme.typography.bodyLarge)
                            }
                        }
                    }
                }
            }
        }
    }
}
