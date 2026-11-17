package com.barcraft.app.ui.profile

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
 * Un toque aplica el tema. El tema activo lleva borde y un tick abajo a la derecha.
 */
@Composable
fun ThemePicker(current: AppTheme, onApply: (AppTheme) -> Unit, modifier: Modifier = Modifier) {
    var expanded by rememberSaveable { mutableStateOf(false) }

    Column(modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        ListItem(
            headlineContent = { Text(stringResource(R.string.theme_picker_title)) },
            supportingContent = { Text(stringResource(current.labelRes)) },
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
                        active = t == current,
                        onClick = { if (t != current) onApply(t) }
                    )
                }
            }
        }
    }
}

@Composable
private fun ThemeCard(
    theme: AppTheme,
    active: Boolean,
    onClick: () -> Unit
) {
    val scheme = theme.colorScheme
    val shape = MaterialTheme.shapes.medium
    Surface(
        shape = shape,
        color = scheme.surfaceContainer,
        contentColor = scheme.onSurface,
        border = if (active) BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else null,
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .clickable(onClick = onClick)
    ) {
        Box {
            Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(theme.emoji, style = MaterialTheme.typography.titleLarge)
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Text(stringResource(theme.labelRes), style = MaterialTheme.typography.titleSmall, color = scheme.primary)
                    Text(stringResource(theme.descriptionRes), style = MaterialTheme.typography.labelSmall, color = scheme.onSurfaceVariant)
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
