@file:OptIn(ExperimentalMaterial3Api::class)

package com.barcraft.app.ui.cocktails

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.Cloud
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material.icons.outlined.Spa
import androidx.compose.material.icons.outlined.StarOutline
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.barcraft.app.data.local.entity.CocktailWithDetails
import com.barcraft.app.data.local.entity.IngredientEntity
import com.barcraft.app.data.local.entity.RatingEntity
import com.barcraft.app.model.Permissions
import com.barcraft.app.ui.components.CocktailImage
import com.barcraft.app.ui.components.DifficultyDots
import com.barcraft.app.ui.components.LoadingBox
import com.barcraft.app.ui.components.SectionCard
import com.barcraft.app.ui.components.SectionTitle
import com.barcraft.app.ui.components.StarRating
import com.barcraft.app.ui.components.UserAvatar
import com.barcraft.app.ui.navigation.LocalAppSession
import com.barcraft.app.utils.DateUtils
import com.barcraft.app.utils.appViewModel

@Composable
fun CocktailDetailScreen(
    cocktailId: Long,
    onBack: () -> Unit,
    onEdit: (Long) -> Unit,
    onShare: (Long) -> Unit
) {
    val vm = appViewModel(key = "detail_$cocktailId") { CocktailDetailViewModel(it.cocktailRepository, cocktailId) }
    val data by vm.cocktail.collectAsStateWithLifecycle()
    val session = LocalAppSession.current

    var tab by rememberSaveable { mutableIntStateOf(0) }
    var menuOpen by remember { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf(false) }
    var ratingDialog by remember { mutableStateOf(false) }

    Scaffold(
        contentWindowInsets = WindowInsets(0),
        topBar = {
            TopAppBar(
                title = { Text(data?.cocktail?.name ?: "") },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Volver") }
                },
                actions = {
                    val c = data?.cocktail
                    if (c != null) {
                        IconButton(onClick = vm::toggleFavorite) {
                            Icon(
                                if (c.isFavorite) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                                contentDescription = "Favorito",
                                tint = if (c.isFavorite) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                            )
                        }
                        IconButton(onClick = { onEdit(c.id) }) { Icon(Icons.Outlined.Edit, "Editar") }
                        Box {
                            IconButton(onClick = { menuOpen = true }) { Icon(Icons.Outlined.MoreVert, "Más") }
                            DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                                if (Permissions.canPublishPosts(session.profile?.role)) DropdownMenuItem(
                                    text = { Text("Compartir en la comunidad") },
                                    leadingIcon = { Icon(Icons.Outlined.Share, null) },
                                    onClick = { menuOpen = false; onShare(c.id) }
                                )
                                DropdownMenuItem(
                                    text = { Text("Eliminar") },
                                    leadingIcon = { Icon(Icons.Outlined.Delete, null) },
                                    onClick = { menuOpen = false; confirmDelete = true }
                                )
                            }
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        },
        floatingActionButton = {
            if (data != null && tab == 2) ExtendedFloatingActionButton(
                onClick = { ratingDialog = true },
                icon = { Icon(Icons.Filled.Star, null) },
                text = { Text("Valorar") }
            )
        }
    ) { padding ->
        val d = data
        if (d == null) {
            LoadingBox(Modifier.padding(padding))
            return@Scaffold
        }
        LazyColumn(
            Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(bottom = 96.dp)
        ) {
            item { Column { DetailHeader(d) } }
            item {
                Column {
                    PrimaryTabRow(selectedTabIndex = tab) {
                        listOf("Ingredientes", "Preparación", "Valoraciones").forEachIndexed { i, label ->
                            Tab(selected = tab == i, onClick = { tab = i }, text = { Text(label) })
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                }
            }
            when (tab) {
                0 -> {
                    if (d.ingredients.isEmpty()) item { InlineEmpty("Sin ingredientes registrados.") }
                    items(d.sortedIngredients, key = { it.id }) { IngredientRow(it) }
                    if (d.cocktail.hasGarnish && d.cocktail.garnishDescription.isNotBlank()) item {
                        SectionCard(Modifier.padding(16.dp)) {
                            Column {
                                SectionTitle("Garnish")
                                Text(d.cocktail.garnishDescription, style = MaterialTheme.typography.bodyLarge)
                            }
                        }
                    }
                }
                1 -> {
                    if (d.steps.isEmpty()) item { InlineEmpty("Aún no hay pasos. Pulsa editar para añadir el procedimiento.") }
                    itemsIndexed(d.sortedSteps, key = { _, s -> s.id }) { i, step -> StepRow(i + 1, step.instruction) }
                }
                else -> {
                    item { RatingSummary(d) }
                    if (d.ratings.isEmpty()) item { InlineEmpty("Aún sin valoraciones. ¡Sé el primero!") }
                    items(d.sortedRatings, key = { it.id }) { RatingRow(it, onDelete = { vm.deleteRating(it.id) }) }
                }
            }
        }
    }

    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text("¿Eliminar este cóctel?") },
            text = { Text("Esta acción no se puede deshacer.") },
            confirmButton = {
                TextButton(onClick = { confirmDelete = false; vm.delete(onBack) }) {
                    Text("Eliminar", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text("Cancelar") } }
        )
    }

    if (ratingDialog) {
        AddRatingDialog(
            defaultName = session.profile?.name.orEmpty(),
            onDismiss = { ratingDialog = false },
            onSubmit = { name, stars, comment ->
                vm.addRating(name, stars, comment)
                ratingDialog = false
            }
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun DetailHeader(d: CocktailWithDetails) {
    val c = d.cocktail
    Box(Modifier.fillMaxWidth().height(240.dp)) {
        CocktailImage(c.imageUri, c.name, Modifier.fillMaxSize())
        Box(
            Modifier.fillMaxSize().background(
                Brush.verticalGradient(listOf(Color.Transparent, MaterialTheme.colorScheme.surface), startY = 300f)
            )
        )
    }
    Column(Modifier.padding(horizontal = 16.dp)) {
        Text(c.name, style = MaterialTheme.typography.headlineMedium)
        if (c.ratingCount > 0) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                StarRating(c.averageRating)
                Spacer(Modifier.width(6.dp))
                Text("%.1f · %d votos".format(c.averageRating, c.ratingCount), style = MaterialTheme.typography.bodySmall)
            }
        }
        Spacer(Modifier.height(10.dp))
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            AssistChip(onClick = {}, label = { Text(c.category) })
            AssistChip(
                onClick = {},
                label = { Text("${c.prepTimeMinutes} min") },
                leadingIcon = { Icon(Icons.Outlined.Schedule, null, Modifier.size(18.dp)) }
            )
            AssistChip(onClick = {}, label = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Dificultad "); DifficultyDots(c.difficulty)
                }
            })
            if (c.hasSmoke) AssistChip(
                onClick = {}, label = { Text("Humo") },
                leadingIcon = { Icon(Icons.Outlined.Cloud, null, Modifier.size(18.dp)) }
            )
            if (c.hasGarnish) AssistChip(
                onClick = {}, label = { Text("Garnish") },
                leadingIcon = { Icon(Icons.Outlined.Spa, null, Modifier.size(18.dp)) }
            )
        }
        if (c.description.isNotBlank()) {
            Spacer(Modifier.height(8.dp))
            Text(c.description, style = MaterialTheme.typography.bodyLarge)
        }
        Spacer(Modifier.height(16.dp))
    }
}

@Composable
private fun IngredientRow(ing: IngredientEntity) {
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(shape = CircleShape, color = typeColor(ing.type), modifier = Modifier.size(10.dp)) {}
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(ing.name, style = MaterialTheme.typography.bodyLarge)
            Text(ing.type, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Text(ing.amount, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
    }
}

@Composable
internal fun typeColor(type: String): Color = when (type) {
    "Alcohol" -> MaterialTheme.colorScheme.primary
    "Mezcla" -> MaterialTheme.colorScheme.tertiary
    "Garnish" -> MaterialTheme.colorScheme.secondary
    else -> MaterialTheme.colorScheme.outline
}

@Composable
private fun StepRow(number: Int, text: String) {
    Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)) {
        Surface(shape = CircleShape, color = MaterialTheme.colorScheme.primaryContainer, modifier = Modifier.size(32.dp)) {
            Box(contentAlignment = Alignment.Center) {
                Text(
                    "$number",
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    fontWeight = FontWeight.Bold
                )
            }
        }
        Spacer(Modifier.width(14.dp))
        Text(text, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f).padding(top = 4.dp))
    }
}

@Composable
private fun RatingSummary(d: CocktailWithDetails) {
    if (d.ratings.isEmpty()) return
    SectionCard(Modifier.padding(horizontal = 16.dp, vertical = 4.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("%.1f".format(d.cocktail.averageRating), style = MaterialTheme.typography.displaySmall)
            Spacer(Modifier.width(16.dp))
            Column {
                StarRating(d.cocktail.averageRating, starSize = 20.dp)
                Text("Media de ${d.ratings.size} votos", style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

@Composable
private fun RatingRow(r: RatingEntity, onDelete: () -> Unit) {
    Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)) {
        UserAvatar(r.voterName, size = 36.dp)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(r.voterName, style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f))
                Text(DateUtils.date(r.createdAt), style = MaterialTheme.typography.labelSmall)
            }
            StarRating(r.stars.toFloat(), starSize = 14.dp)
            if (r.comment.isNotBlank()) Text(r.comment, style = MaterialTheme.typography.bodyMedium)
        }
        IconButton(onClick = onDelete) {
            Icon(Icons.Outlined.Delete, "Borrar valoración", tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun InlineEmpty(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
    )
}

@Composable
private fun AddRatingDialog(
    defaultName: String,
    onDismiss: () -> Unit,
    onSubmit: (String, Int, String) -> Unit
) {
    var name by rememberSaveable { mutableStateOf(defaultName) }
    var stars by rememberSaveable { mutableIntStateOf(5) }
    var comment by rememberSaveable { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Outlined.StarOutline, null) },
        title = { Text("Añadir valoración") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = name, onValueChange = { name = it },
                    label = { Text("Nombre del votante") }, singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                    StarRating(stars.toFloat(), starSize = 34.dp, onRate = { stars = it })
                    Text("$stars de 6 estrellas", style = MaterialTheme.typography.labelMedium)
                }
                OutlinedTextField(
                    value = comment, onValueChange = { comment = it },
                    label = { Text("Comentario (opcional)") },
                    minLines = 2, modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(onClick = { onSubmit(name, stars, comment) }, enabled = name.isNotBlank()) { Text("Guardar") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } }
    )
}

