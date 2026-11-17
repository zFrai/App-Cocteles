@file:OptIn(ExperimentalLayoutApi::class)

package com.barcraft.app.ui.profile

import com.barcraft.app.ui.components.FitText
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AlternateEmail
import androidx.compose.material.icons.outlined.CameraAlt
import androidx.compose.material.icons.outlined.Language
import androidx.compose.material.icons.outlined.MusicNote
import androidx.compose.material.icons.outlined.Place
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.unit.dp
import com.barcraft.app.model.ProfileLinks

/** Chips de "Dónde encontrarme": cada uno abre la red o el local en Maps. No pinta nada si está vacío. */
@Composable
fun WhereToFindMe(links: ProfileLinks, modifier: Modifier = Modifier) {
    if (links.isEmpty) return
    val uri = LocalUriHandler.current
    val entries = buildList {
        links.instagramUrl?.let { add(Triple(Icons.Outlined.CameraAlt, "@${links.instagram}", it)) }
        links.tiktokUrl?.let { add(Triple(Icons.Outlined.MusicNote, "@${links.tiktok}", it)) }
        links.xUrl?.let { add(Triple(Icons.Outlined.AlternateEmail, "@${links.x}", it)) }
        links.webUrl?.let { add(Triple(Icons.Outlined.Language, links.web.removePrefix("https://").removePrefix("http://"), it)) }
        links.mapsUrl?.let { add(Triple(Icons.Outlined.Place, links.placeLabel, it)) }
    }
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            "Dónde encontrarme",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
            modifier = Modifier.fillMaxWidth()
        ) {
            entries.forEach { (icon, label, url) -> LinkChip(icon, label) { runCatching { uri.openUri(url) } } }
        }
    }
}

@Composable
private fun LinkChip(icon: ImageVector, label: String, onClick: () -> Unit) {
    AssistChip(
        onClick = onClick,
        label = { FitText(label) },
        leadingIcon = { Icon(icon, null) }
    )
}

/** Diálogo para editar "Dónde encontrarme". Todos los campos son opcionales. */
@Composable
fun EditLinksDialog(initial: ProfileLinks, onDismiss: () -> Unit, onSave: (ProfileLinks) -> Unit) {
    var instagram by rememberSaveable { mutableStateOf(initial.instagram) }
    var tiktok by rememberSaveable { mutableStateOf(initial.tiktok) }
    var x by rememberSaveable { mutableStateOf(initial.x) }
    var web by rememberSaveable { mutableStateOf(initial.web) }
    var placeName by rememberSaveable { mutableStateOf(initial.placeName) }
    var placeAddress by rememberSaveable { mutableStateOf(initial.placeAddress) }
    val max = ProfileLinks.MAX_FIELD

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Dónde encontrarme") },
        text = {
            Column(
                Modifier.heightIn(max = 460.dp).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    "Opcional. Rellena solo lo que quieras que vean los demás.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                OutlinedTextField(instagram, { instagram = it.take(max) }, label = { Text("Instagram (usuario)") }, singleLine = true)
                OutlinedTextField(tiktok, { tiktok = it.take(max) }, label = { Text("TikTok (usuario)") }, singleLine = true)
                OutlinedTextField(x, { x = it.take(max) }, label = { Text("X (usuario)") }, singleLine = true)
                OutlinedTextField(web, { web = it.take(max) }, label = { Text("Web") }, singleLine = true)
                OutlinedTextField(placeName, { placeName = it.take(max) }, label = { Text("Local (nombre)") }, singleLine = true)
                OutlinedTextField(placeAddress, { placeAddress = it.take(max) }, label = { Text("Dirección o ciudad del local") }, singleLine = true)
            }
        },
        confirmButton = {
            Button(onClick = {
                onSave(ProfileLinks(instagram, tiktok, x, web, placeName, placeAddress))
            }) { FitText("Guardar") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { FitText("Cancelar") } }
    )
}
