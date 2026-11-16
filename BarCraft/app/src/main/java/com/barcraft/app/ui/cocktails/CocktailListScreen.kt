@file:OptIn(ExperimentalMaterial3Api::class)

package com.barcraft.app.ui.cocktails

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.LocalBar
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.SearchOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.barcraft.app.data.local.entity.CocktailEntity
import com.barcraft.app.data.local.entity.CocktailWithDetails
import com.barcraft.app.model.CocktailCatalog
import com.barcraft.app.ui.components.CocktailImage
import com.barcraft.app.ui.components.DifficultyDots
import com.barcraft.app.ui.components.EmptyState
import com.barcraft.app.ui.components.LoadingBox
import com.barcraft.app.ui.components.StarRating
import com.barcraft.app.utils.appViewModel
import androidx.compose.foundation.layout.WindowInsets

@Composable
fun CocktailListScreen(
    onOpen: (Long) -> Unit,
    onEdit: (Long) -> Unit,
    onCreate: () -> Unit
) {
    val vm = appViewModel { CocktailListViewModel(it.cocktailRepository) }
    val state by vm.state.collectAsStateWithLifecycle()
    var toDelete by remember { mutableStateOf<CocktailEntity?>(null) }
    val scroll = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    Scaffold(
        modifier = Modifier.nestedScroll(scroll.nestedScrollConnection),
        contentWindowInsets = WindowInsets(0),
        topBar = {
            LargeTopAppBar(
                title = {
                    Column {
                        Text("Mis cócteles")
                        if (state.all.isNotEmpty() && scroll.state.collapsedFraction < 0.5f) {
                            Text(
                                "${state.all.size} recetas en tu barra",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
                actions = {
                    IconButton(onClick = vm::toggleFavoritesOnly) {
                        Icon(
                            if (state.favoritesOnly) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                            contentDescription = "Solo favoritos",
                            tint = if (state.favoritesOnly) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                scrollBehavior = scroll
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onCreate,
                icon = { Icon(Icons.Filled.Add, null) },
                text = { Text("Nuevo cóctel") }
            )
        }
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            OutlinedTextField(
                value = state.query,
                onValueChange = vm::setQuery,
                placeholder = { Text("Buscar por nombre o ingrediente") },
                leadingIcon = { Icon(Icons.Filled.Search, null) },
                trailingIcon = {
                    if (state.query.isNotEmpty()) IconButton(onClick = { vm.setQuery("") }) {
                        Icon(Icons.Filled.Close, contentDescription = "Borrar búsqueda")
                    }
                },
                singleLine = true,
                shape = MaterialTheme.shapes.extraLarge,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp)
            )
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item {
                    FilterChip(
                        selected = state.category == null,
                        onClick = { vm.setCategory(null) },
                        label = { Text("Todas") }
                    )
                }
                items(CocktailCatalog.categories) { cat ->
                    FilterChip(
                        selected = state.category == cat,
                        onClick = { vm.setCategory(if (state.category == cat) null else cat) },
                        label = { Text(cat) },
                        colors = FilterChipDefaults.filterChipColors()
                    )
                }
            }

            when {
                !state.loaded -> LoadingBox()
                state.all.isEmpty() -> EmptyState(
                    icon = Icons.Filled.LocalBar,
                    title = "Empieza tu recetario",
                    message = "Guarda tus cócteles con ingredientes, pasos, humo y garnish. Todo se queda en el móvil y funciona sin conexión.",
                    actionLabel = "Crear mi primer cóctel",
                    onAction = onCreate
                )
                state.visible.isEmpty() -> EmptyState(
                    icon = Icons.Outlined.SearchOff,
                    title = "Sin resultados",
                    message = "Prueba con otra búsqueda o quita los filtros."
                )
                else -> LazyColumn(
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 96.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(state.visible, key = { it.cocktail.id }) { item ->
                        CocktailCard(
                            item = item,
                            onOpen = { onOpen(item.cocktail.id) },
                            onEdit = { onEdit(item.cocktail.id) },
                            onDelete = { toDelete = item.cocktail },
                            onFavorite = { vm.toggleFavorite(item.cocktail) }
                        )
                    }
                }
            }
        }
    }

    toDelete?.let { c ->
        AlertDialog(
            onDismissRequest = { toDelete = null },
            icon = { Icon(Icons.Outlined.Delete, null) },
            title = { Text("¿Eliminar ${c.name}?") },
            text = { Text("Se borrarán también sus ingredientes, pasos y valoraciones.") },
            confirmButton = {
                TextButton(onClick = { vm.delete(c); toDelete = null }) {
                    Text("Eliminar", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = { TextButton(onClick = { toDelete = null }) { Text("Cancelar") } }
        )
    }
}

/** Tarjeta que se expande para ver ingredientes y acciones rápidas. */
@Composable
private fun CocktailCard(
    item: CocktailWithDetails,
    onOpen: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onFavorite: () -> Unit
) {
    val c = item.cocktail
    var expanded by rememberSaveable(c.id) { mutableStateOf(false) }

    ElevatedCard(onClick = onOpen, modifier = Modifier.fillMaxWidth().animateContentSize()) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            CocktailImage(
                model = c.imageUri,
                contentDescription = c.name,
                modifier = Modifier.size(84.dp).clip(MaterialTheme.shapes.small)
            )
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(c.name, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(
                    buildString {
                        append(c.category)
                        if (c.hasSmoke) append(" · Humo")
                        if (c.hasGarnish) append(" · Garnish")
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(Modifier.height(6.dp))
                if (c.ratingCount > 0) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        StarRating(c.averageRating, starSize = 14.dp)
                        Spacer(Modifier.width(6.dp))
                        Text(
                            "%.1f (%d)".format(c.averageRating, c.ratingCount),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Spacer(Modifier.height(4.dp))
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    DifficultyDots(c.difficulty)
                    Spacer(Modifier.width(10.dp))
                    Icon(
                        Icons.Outlined.Schedule, null,
                        modifier = Modifier.size(14.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        " ${c.prepTimeMinutes} min · ${item.ingredients.size} ingr.",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                IconButton(onClick = onFavorite) {
                    Icon(
                        if (c.isFavorite) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                        contentDescription = "Favorito",
                        tint = if (c.isFavorite) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                IconButton(onClick = { expanded = !expanded }) {
                    Icon(
                        if (expanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                        contentDescription = if (expanded) "Contraer" else "Expandir"
                    )
                }
            }
        }
        AnimatedVisibility(expanded) {
            Column(Modifier.padding(start = 16.dp, end = 16.dp, bottom = 8.dp)) {
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                Spacer(Modifier.height(8.dp))
                if (c.description.isNotBlank()) {
                    Text(c.description, style = MaterialTheme.typography.bodyMedium, maxLines = 3, overflow = TextOverflow.Ellipsis)
                    Spacer(Modifier.height(8.dp))
                }
                item.sortedIngredients.take(6).forEach {
                    Text(
                        "• ${it.amount} ${it.name}".replace("  ", " "),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                if (item.ingredients.size > 6) {
                    Text("+${item.ingredients.size - 6} más", style = MaterialTheme.typography.labelSmall)
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onDelete) {
                        Icon(Icons.Outlined.Delete, null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Eliminar")
                    }
                    TextButton(onClick = onEdit) {
                        Icon(Icons.Outlined.Edit, null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Editar")
                    }
                }
            }
        }
    }
}
