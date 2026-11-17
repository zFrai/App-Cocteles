package com.barcraft.app.ui.profile

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.ExpandLess
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.barcraft.app.R
import com.barcraft.app.ui.theme.AppTheme

/**
 * Selector de "Tema del perfil" como desplegable de fichas.
 * Un toque marca la ficha; un doble toque la aplica. El tema activo lleva un tick abajo a la derecha.
 */
@Composable
fun ThemePicker(current: AppTheme, onApply: (AppTheme) -> Unit, modifier: Modifier = Modifier) {
    var expanded by rememberSaveable { mutableStateOf(false) }
    var marked by rememberSaveable { mutableStateOf(current.name) }
    // Al cerrar el desplegable o cambiar el tema, la marca vuelve al tema actual.
    LaunchedEffect(expanded, current) { if (!expanded) marked = current.name }

    Column(modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        ListItem(
            headlineContent = { Text(stringResource(R.string.theme_picker_title)) },
            supportingContent = { Text(current.label) },
            leadingContent = { Icon(Icons.Outlined.Palette, null) },
            trailingContent = {
                Icon(
                    if (expanded) Icons.Outlined.ExpandLess else Icons.Outlined.ExpandMore,
                    stringResource(if (expanded) R.string.theme_picker_collapse else R.string.theme_picker_expand)
                )
            },
            colors = ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
            modifier = Modifier.clip(MaterialTheme.shapes.medium).clickable { expanded = !expanded }
        )
        AnimatedVisibility(expanded) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    stringResource(R.string.theme_picker_hint),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 4.dp)
                )
                AppTheme.entries.forEach { t ->
                    ThemeCard(
                        theme = t,
                        marked = t.name == marked,
                        active = t == current,
                        onClick = { marked = t.name },
                        onDoubleClick = {
                            marked = t.name
                            if (t != current) onApply(t)
                        }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ThemeCard(
    theme: AppTheme,
    marked: Boolean,
    active: Boolean,
    onClick: () -> Unit,
    onDoubleClick: () -> Unit
) {
    val scheme = theme.colorScheme
    val shape = MaterialTheme.shapes.medium
    Surface(
        shape = shape,
        color = scheme.surfaceContainer,
        contentColor = scheme.onSurface,
        border = if (marked) BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else null,
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .combinedClickable(onClick = onClick, onDoubleClick = onDoubleClick)
    ) {
        Box {
            Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(theme.emoji, style = MaterialTheme.typography.titleLarge)
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Text(theme.label, style = MaterialTheme.typography.titleSmall, color = scheme.primary)
                    Text(theme.description, style = MaterialTheme.typography.labelSmall, color = scheme.onSurfaceVariant)
                }
                Spacer(Modifier.width(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    listOf(scheme.primary, scheme.secondary, scheme.tertiary, scheme.background).forEach {
                        ColorDot(it, outline = scheme.outline)
                    }
                }
            }
            if (active) {
                Icon(
                    Icons.Outlined.CheckCircle,
                    stringResource(R.string.theme_picker_current),
                    tint = scheme.primary,
                    modifier = Modifier.align(Alignment.BottomEnd).padding(4.dp).size(18.dp)
                )
            }
        }
    }
}

@Composable
private fun ColorDot(color: Color, outline: Color) {
    Box(
        Modifier
            .size(14.dp)
            .clip(CircleShape)
            .background(color)
            .border(1.dp, outline.copy(alpha = 0.5f), CircleShape)
    )
}
