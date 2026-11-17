package com.barcraft.app.ui.profile

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
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
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.barcraft.app.R
import com.barcraft.app.ui.theme.AppTheme
import com.barcraft.app.ui.theme.ThemePair
import kotlinx.coroutines.launch

/**
 * Selector de "Tema del perfil" como desplegable de fichas, una por pareja oscuro/claro.
 * Deslizar a la derecha aplica la versión oscura (tick a la derecha) y a la izquierda la clara
 * (tick a la izquierda). Un toque aplica la versión que se ve en la ficha.
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
                ThemePair.entries.forEach { pair ->
                    ThemePairCard(pair, current) { t -> if (t != current) onApply(t) }
                }
            }
        }
    }
}

@Composable
private fun ThemePairCard(pair: ThemePair, current: AppTheme, onApply: (AppTheme) -> Unit) {
    val active = current in pair
    // Versión visible: la activa si la ficha es la del tema actual; si no, la oscura.
    var showDark by rememberSaveable(pair) { mutableStateOf(if (active) current == pair.dark else true) }
    LaunchedEffect(current) { if (active) showDark = current == pair.dark }
    val shown = if (showDark) pair.dark else pair.light
    val scheme = shown.colorScheme
    val shape = MaterialTheme.shapes.medium

    val offset = remember { Animatable(0f) }
    val scope = rememberCoroutineScope()
    val density = LocalDensity.current
    val maxDrag = with(density) { 72.dp.toPx() }
    val threshold = with(density) { 40.dp.toPx() }

    Surface(
        shape = shape,
        color = scheme.surfaceContainer,
        contentColor = scheme.onSurface,
        border = if (active) BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else null,
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer { translationX = offset.value }
            .clip(shape)
            .pointerInput(pair, current) {
                detectHorizontalDragGestures(
                    onDragEnd = {
                        val dx = offset.value
                        when {
                            dx > threshold -> { showDark = true; onApply(pair.dark) }
                            dx < -threshold -> { showDark = false; onApply(pair.light) }
                        }
                        scope.launch { offset.animateTo(0f) }
                    },
                    onDragCancel = { scope.launch { offset.animateTo(0f) } },
                    onHorizontalDrag = { change, delta ->
                        change.consume()
                        scope.launch { offset.snapTo((offset.value + delta).coerceIn(-maxDrag, maxDrag)) }
                    }
                )
            }
            .clickable { onApply(shown) }
    ) {
        Box {
            Row(Modifier.padding(horizontal = 28.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(shown.emoji, style = MaterialTheme.typography.titleLarge)
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Text(stringResource(shown.labelRes), style = MaterialTheme.typography.titleSmall, color = scheme.primary)
                    Text(
                        stringResource(if (showDark) R.string.theme_picker_dark else R.string.theme_picker_light),
                        style = MaterialTheme.typography.labelSmall,
                        color = scheme.onSurfaceVariant
                    )
                }
                Spacer(Modifier.width(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    listOf(scheme.primary, scheme.secondary, scheme.tertiary, scheme.background).forEach {
                        ColorDot(it, outline = scheme.outline)
                    }
                }
            }
            // Flechas laterales: recuerdan hacia dónde deslizar.
            Text(
                "‹ ☀",
                style = MaterialTheme.typography.labelSmall,
                color = scheme.onSurfaceVariant.copy(alpha = if (showDark) 0.8f else 0.3f),
                modifier = Modifier.align(Alignment.CenterStart).padding(start = 6.dp)
            )
            Text(
                "☾ ›",
                style = MaterialTheme.typography.labelSmall,
                color = scheme.onSurfaceVariant.copy(alpha = if (showDark) 0.3f else 0.8f),
                modifier = Modifier.align(Alignment.CenterEnd).padding(end = 6.dp)
            )
            if (active) {
                Icon(
                    Icons.Outlined.CheckCircle,
                    stringResource(R.string.theme_picker_current),
                    tint = scheme.primary,
                    modifier = Modifier
                        .align(if (current == pair.dark) Alignment.BottomEnd else Alignment.BottomStart)
                        .padding(4.dp)
                        .size(18.dp)
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
