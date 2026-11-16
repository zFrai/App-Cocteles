package com.barcraft.app.ui.chat

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Chat
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.Block
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.PersonAdd
import androidx.compose.material.icons.outlined.PersonRemove
import androidx.compose.material.icons.outlined.StarOutline
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.barcraft.app.model.FriendState
import com.barcraft.app.ui.components.RoleBadge
import com.barcraft.app.ui.components.UserAvatar

/** Fila de una persona con sus acciones (amistad, favorito, bloqueo, chat). */
@Composable
fun PersonRow(
    person: Person,
    vm: PeopleViewModel,
    onOpenChat: (String) -> Unit,
    showPrimaryAction: Boolean = true
) {
    var menu by remember { mutableStateOf(false) }
    val p = person.profile
    val openChat = { vm.openChat(p.uid, p.name, onOpenChat) }

    ListItem(
        leadingContent = { UserAvatar(p.name) },
        headlineContent = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(p.name, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f, fill = false))
                if (person.favorite) {
                    Spacer(Modifier.width(4.dp))
                    Icon(Icons.Filled.Star, "Favorito", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                }
                Spacer(Modifier.width(6.dp))
                RoleBadge(p.role, showUser = true)
            }
        },
        supportingContent = {
            val status = when (person.state) {
                FriendState.FRIENDS -> p.bio.ifBlank { "Amigo" }
                FriendState.REQUEST_SENT -> "Solicitud enviada"
                FriendState.REQUEST_RECEIVED -> "Quiere ser tu amigo"
                FriendState.NONE -> p.bio.ifBlank { p.role.description }
            }
            // "@laura4821 · Amigo": el @tag distingue a personas con el mismo nombre.
            Text(
                if (p.handle.isBlank()) status else "${p.handle} · $status",
                maxLines = 1, overflow = TextOverflow.Ellipsis
            )
        },
        trailingContent = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (showPrimaryAction) when (person.state) {
                    FriendState.NONE -> OutlinedButton(onClick = { vm.sendRequest(person) }) {
                        Icon(Icons.Outlined.PersonAdd, null, Modifier.size(18.dp)); Spacer(Modifier.width(4.dp)); Text("Añadir")
                    }
                    FriendState.REQUEST_RECEIVED -> FilledTonalButton(onClick = { vm.accept(person) }) { Text("Aceptar") }
                    FriendState.REQUEST_SENT -> OutlinedButton(onClick = { vm.removeFriend(person) }) { Text("Cancelar") }
                    FriendState.FRIENDS -> IconButton(onClick = openChat) { Icon(Icons.AutoMirrored.Outlined.Chat, "Escribir") }
                }
                Box {
                    IconButton(onClick = { menu = true }) { Icon(Icons.Outlined.MoreVert, "Más opciones") }
                    DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                        DropdownMenuItem(
                            text = { Text("Enviar mensaje") },
                            leadingIcon = { Icon(Icons.AutoMirrored.Outlined.Chat, null) },
                            onClick = { menu = false; openChat() }
                        )
                        DropdownMenuItem(
                            text = { Text(if (person.favorite) "Quitar de favoritos" else "Añadir a favoritos") },
                            leadingIcon = { Icon(if (person.favorite) Icons.Filled.Star else Icons.Outlined.StarOutline, null) },
                            onClick = { menu = false; vm.toggleFavorite(person) }
                        )
                        if (person.state == FriendState.FRIENDS) DropdownMenuItem(
                            text = { Text("Eliminar de amigos") },
                            leadingIcon = { Icon(Icons.Outlined.PersonRemove, null) },
                            onClick = { menu = false; vm.removeFriend(person) }
                        )
                        if (person.state == FriendState.REQUEST_RECEIVED) DropdownMenuItem(
                            text = { Text("Rechazar solicitud") },
                            leadingIcon = { Icon(Icons.Outlined.PersonRemove, null) },
                            onClick = { menu = false; vm.removeFriend(person) }
                        )
                        DropdownMenuItem(
                            text = { Text("Bloquear", color = MaterialTheme.colorScheme.error) },
                            leadingIcon = { Icon(Icons.Outlined.Block, null, tint = MaterialTheme.colorScheme.error) },
                            onClick = { menu = false; vm.block(person) }
                        )
                    }
                }
            }
        },
        modifier = Modifier.clickable { if (person.state == FriendState.FRIENDS) openChat() else menu = true }
    )
}
