@file:OptIn(ExperimentalMaterial3Api::class)

package com.barcraft.app.ui.cocktails

import androidx.compose.ui.res.stringResource
import com.barcraft.app.R
import com.barcraft.app.ui.components.FitText
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.outlined.AddPhotoAlternate
import androidx.compose.material.icons.outlined.ArrowDownward
import androidx.compose.material.icons.outlined.ArrowUpward
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Link
import androidx.compose.material.icons.outlined.PlaylistAdd
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.barcraft.app.data.local.entity.CocktailWithDetails
import com.barcraft.app.model.CocktailCatalog
import com.barcraft.app.model.RecipeVisibility
import com.barcraft.app.repository.IngredientDraft
import com.barcraft.app.ui.components.CocktailImage
import com.barcraft.app.ui.components.LoadingBox
import com.barcraft.app.ui.components.SectionCard
import com.barcraft.app.ui.components.SectionTitle
import com.barcraft.app.ui.components.rememberPhotoPicker
import com.barcraft.app.ui.navigation.LocalAppSession
import com.barcraft.app.utils.ImageUtils
import com.barcraft.app.utils.appViewModel
import kotlin.math.roundToInt

@Composable
fun CocktailEditorScreen(cocktailId: Long, onDone: () -> Unit) {
    val me = LocalAppSession.current.profile
    val vm = appViewModel(key = "editor_$cocktailId") { c ->
        CocktailEditorViewModel(
            c.cocktailRepository, cocktailId,
            share = me?.let { p -> { recipe: CocktailWithDetails -> c.recipeShareRepository.sync(p, recipe) } }
        )
    }
    val draft by vm.draft.collectAsStateWithLifecycle()
    val loading by vm.loading.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val snackbar = remember { SnackbarHostState() }

    var ingredientDialog by remember { mutableStateOf<Int?>(null) } // -1 = nuevo, n = editar
    var newStep by rememberSaveable { mutableStateOf("") }

    // Cámara o galería: la foto se copia dentro de la app (la receta es local).
    val picker = rememberPhotoPicker { uri -> vm.pickImage(context, uri) }

    LaunchedEffect(Unit) { vm.errors.collect { snackbar.showSnackbar(it) } }

    Scaffold(
        contentWindowInsets = WindowInsets(0),
        snackbarHost = { SnackbarHost(snackbar) },
        topBar = {
            TopAppBar(
                title = { FitText(if (vm.isEditing) stringResource(R.string.editor_title_edit) else stringResource(R.string.editor_title_new)) },
                navigationIcon = { IconButton(onClick = onDone) { Icon(Icons.Filled.Close, stringResource(R.string.action_close)) } },
                actions = {
                    Button(onClick = { vm.save { onDone() } }, modifier = Modifier.padding(end = 8.dp)) {
                        FitText(stringResource(R.string.action_save))
                    }
                }
            )
        }
    ) { padding ->
        if (loading) { LoadingBox(Modifier.padding(padding)); return@Scaffold }

        LazyColumn(
            Modifier.fillMaxSize().padding(padding).imePadding(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // ── Imagen ────────────────────────────────────────────────
            item {
              Column {
                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(190.dp)
                        .clip(MaterialTheme.shapes.large)
                        .clickable { picker.choose() }
                ) {
                    CocktailImage(draft.imageUri, null, Modifier.fillMaxSize())
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.9f),
                        modifier = Modifier.align(Alignment.BottomEnd).padding(12.dp)
                    ) {
                        Row(Modifier.padding(horizontal = 14.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Outlined.AddPhotoAlternate, null, Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text(if (draft.imageUri.isBlank()) stringResource(R.string.editor_choose_photo) else stringResource(R.string.editor_change_photo), style = MaterialTheme.typography.labelLarge)
                        }
                    }
                }
                Spacer(Modifier.height(8.dp))
                var urlText by rememberSaveable(draft.id) {
                    mutableStateOf(if (ImageUtils.isRemote(draft.imageUri)) draft.imageUri else "")
                }
                OutlinedTextField(
                    value = urlText,
                    onValueChange = {
                        urlText = it
                        if (ImageUtils.isRemote(it.trim())) vm.update { copy(imageUri = it.trim()) }
                    },
                    label = { Text(stringResource(R.string.editor_image_url)) },
                    leadingIcon = { Icon(Icons.Outlined.Link, null) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
            }

            // ── Datos básicos ─────────────────────────────────────────
            item {
                SectionCard {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        SectionTitle(stringResource(R.string.editor_section_basic))
                        OutlinedTextField(
                            value = draft.name, onValueChange = { vm.update { copy(name = it) } },
                            label = { Text(stringResource(R.string.editor_name)) }, singleLine = true, modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = draft.description, onValueChange = { vm.update { copy(description = it) } },
                            label = { Text(stringResource(R.string.editor_description)) }, minLines = 3, modifier = Modifier.fillMaxWidth()
                        )
                        CategoryPicker(draft.category) { vm.update { copy(category = it) } }
                        Text(stringResource(R.string.editor_difficulty, draft.difficulty, CocktailCatalog.MAX_DIFFICULTY), style = MaterialTheme.typography.labelLarge)
                        Slider(
                            value = draft.difficulty.toFloat(),
                            onValueChange = { vm.update { copy(difficulty = it.roundToInt()) } },
                            valueRange = 1f..5f, steps = 3
                        )
                        Text(stringResource(R.string.editor_prep_time, draft.prepTimeMinutes), style = MaterialTheme.typography.labelLarge)
                        Slider(
                            value = draft.prepTimeMinutes.toFloat(),
                            onValueChange = { vm.update { copy(prepTimeMinutes = it.roundToInt()) } },
                            valueRange = 1f..30f
                        )
                    }
                }
            }

            // ── Extras ────────────────────────────────────────────────
            item {
                SectionCard(contentPadding = PaddingValues(vertical = 8.dp)) {
                    Column {
                        ToggleItem(stringResource(R.string.editor_smoke), stringResource(R.string.editor_smoke_desc), draft.hasSmoke) {
                            vm.update { copy(hasSmoke = it) }
                        }
                        ToggleItem(stringResource(R.string.editor_garnish), stringResource(R.string.editor_garnish_desc), draft.hasGarnish) {
                            vm.update { copy(hasGarnish = it) }
                        }
                        if (draft.hasGarnish) OutlinedTextField(
                            value = draft.garnishDescription,
                            onValueChange = { vm.update { copy(garnishDescription = it) } },
                            label = { Text(stringResource(R.string.editor_garnish_detail)) },
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)
                        )
                        ToggleItem(stringResource(R.string.editor_favorite), stringResource(R.string.editor_favorite_desc), draft.isFavorite) {
                            vm.update { copy(isFavorite = it) }
                        }
                    }
                }
            }

            // ── Quién puede verla ─────────────────────────────────────
            item {
                SectionCard {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        SectionTitle(stringResource(R.string.editor_section_visibility))
                        val current = RecipeVisibility.from(draft.visibility)
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            RecipeVisibility.entries.forEach { v ->
                                FilterChip(
                                    modifier = Modifier.weight(1f),
                                    selected = current == v,
                                    enabled = me != null || v == RecipeVisibility.PRIVATE,
                                    onClick = { vm.update { copy(visibility = v.id) } },
                                    label = { FitText(stringResource(v.labelRes)) }
                                )
                            }
                        }
                        Text(
                            if (me == null) stringResource(R.string.editor_visibility_login)
                            else stringResource(current.descriptionRes),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        if (me != null && current != RecipeVisibility.PRIVATE && draft.imageUri.startsWith("file:")) Text(
                            stringResource(R.string.editor_visibility_local_photo),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // ── Ingredientes ──────────────────────────────────────────
            item {
                SectionCard {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        SectionTitle(stringResource(R.string.editor_section_ingredients, draft.ingredients.size))
                        draft.ingredients.forEachIndexed { i, ing ->
                            ListItem(
                                headlineContent = { Text(ing.name) },
                                supportingContent = { Text(stringResource(R.string.editor_ingredient_detail, ing.amount, ing.type)) },
                                leadingContent = {
                                    Surface(shape = CircleShape, color = typeColor(ing.type), modifier = Modifier.size(10.dp)) {}
                                },
                                trailingContent = {
                                    IconButton(onClick = { vm.removeIngredient(i) }) { Icon(Icons.Outlined.Delete, stringResource(R.string.action_remove)) }
                                },
                                colors = ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                                modifier = Modifier.clickable { ingredientDialog = i }
                            )
                        }
                        FilledTonalButton(onClick = { ingredientDialog = -1 }, modifier = Modifier.fillMaxWidth()) {
                            Icon(Icons.Outlined.PlaylistAdd, null)
                            Spacer(Modifier.width(8.dp))
                            FitText(stringResource(R.string.editor_add_ingredient))
                        }
                    }
                }
            }

            // ── Procedimiento ─────────────────────────────────────────
            item {
                SectionCard {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        SectionTitle(stringResource(R.string.editor_section_steps))
                        draft.steps.forEachIndexed { i, step ->
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(stringResource(R.string.editor_step_number, i + 1), fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                Spacer(Modifier.width(8.dp))
                                OutlinedTextField(
                                    value = step, onValueChange = { vm.updateStep(i, it) },
                                    modifier = Modifier.weight(1f)
                                )
                                Column {
                                    IconButton(onClick = { vm.moveStep(i, i - 1) }, enabled = i > 0, modifier = Modifier.size(32.dp)) {
                                        Icon(Icons.Outlined.ArrowUpward, stringResource(R.string.action_move_up), Modifier.size(18.dp))
                                    }
                                    IconButton(onClick = { vm.moveStep(i, i + 1) }, enabled = i < draft.steps.lastIndex, modifier = Modifier.size(32.dp)) {
                                        Icon(Icons.Outlined.ArrowDownward, stringResource(R.string.action_move_down), Modifier.size(18.dp))
                                    }
                                }
                                IconButton(onClick = { vm.removeStep(i) }) { Icon(Icons.Outlined.Delete, stringResource(R.string.editor_remove_step)) }
                            }
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            OutlinedTextField(
                                value = newStep, onValueChange = { newStep = it },
                                label = { Text(stringResource(R.string.editor_step_hint, draft.steps.size + 1)) },
                                modifier = Modifier.weight(1f)
                            )
                            Spacer(Modifier.width(8.dp))
                            FilledTonalButton(
                                onClick = { vm.addStep(newStep.trim()); newStep = "" },
                                enabled = newStep.isNotBlank()
                            ) { FitText(stringResource(R.string.action_add)) }
                        }
                    }
                }
            }
            item { Spacer(Modifier.height(32.dp)) }
        }
    }

    ingredientDialog?.let { index ->
        IngredientDialog(
            initial = draft.ingredients.getOrNull(index),
            onDismiss = { ingredientDialog = null },
            onSave = {
                if (index >= 0) vm.updateIngredient(index, it) else vm.addIngredient(it)
                ingredientDialog = null
            }
        )
    }
}

@Composable
private fun CategoryPicker(selected: String, onSelect: (String) -> Unit) {
    var open by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(expanded = open, onExpandedChange = { open = it }) {
        OutlinedTextField(
            value = selected, onValueChange = {}, readOnly = true,
            label = { Text(stringResource(R.string.editor_category)) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(open) },
            modifier = Modifier.fillMaxWidth().menuAnchor(MenuAnchorType.PrimaryNotEditable)
        )
        ExposedDropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            CocktailCatalog.categories.forEach { cat ->
                DropdownMenuItem(text = { FitText(cat) }, onClick = { onSelect(cat); open = false })
            }
        }
    }
}

@Composable
private fun ToggleItem(title: String, subtitle: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    ListItem(
        headlineContent = { Text(title) },
        supportingContent = { Text(subtitle) },
        trailingContent = { Switch(checked = checked, onCheckedChange = onChange) },
        colors = ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        modifier = Modifier.clickable { onChange(!checked) }
    )
}

@Composable
private fun IngredientDialog(
    initial: IngredientDraft?,
    onDismiss: () -> Unit,
    onSave: (IngredientDraft) -> Unit
) {
    var name by rememberSaveable { mutableStateOf(initial?.name.orEmpty()) }
    var amount by rememberSaveable { mutableStateOf(initial?.amount.orEmpty()) }
    var type by rememberSaveable { mutableStateOf(initial?.type ?: CocktailCatalog.ingredientTypes.first()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initial == null) stringResource(R.string.ingredient_new) else stringResource(R.string.ingredient_edit)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = name, onValueChange = { name = it },
                    label = { Text(stringResource(R.string.ingredient_name)) }, singleLine = true, modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = amount, onValueChange = { amount = it },
                    label = { Text(stringResource(R.string.ingredient_amount)) }, singleLine = true, modifier = Modifier.fillMaxWidth()
                )
                Text(stringResource(R.string.ingredient_type), style = MaterialTheme.typography.labelLarge)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(CocktailCatalog.ingredientTypes) { t ->
                        FilterChip(selected = type == t, onClick = { type = t }, label = { FitText(t) })
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onSave(IngredientDraft(name.trim(), amount.trim(), type)) },
                enabled = name.isNotBlank()
            ) { FitText(stringResource(R.string.action_save)) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { FitText(stringResource(R.string.action_cancel)) } }
    )
}
