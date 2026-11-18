package com.barcraft.app.ui.chat

import androidx.compose.ui.res.stringResource
import com.barcraft.app.R
import com.barcraft.app.ui.components.FitText
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
import androidx.compose.material.icons.outlined.Lock
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
import com.barcraft.app.model.FollowState
import com.barcraft.app.ui.components.RoleBadge
import com.barcraft.app.ui.components.UserAvatar

/** Fila de una persona con sus acciones (seguir, solicitudes, favorito, bloqueo, chat). */
@Composable
fun PersonRow(
    person: Person,
    vm: PeopleViewModel,
    onOpenChat: (String) -> Unit,
    showPrimaryAction: Boolean = true
) {
    var menu by remember { mutableStateOf(false) }
    val p = person.profile
    // Tipo explícito: onClick espera () -> Unit.
    val openChat: () -> Unit = { vm.openChat(person, onOpenChat) }

    ListItem(
        leadingContent = { UserAvatar(p.name, uid = p.uid, photoId = p.photoId) },
        headlineContent = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(p.name, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f, fill = false))
                if (p.privateAccount) {
                    Spacer(Modifier.width(4.dp))
                    Icon(Icons.Outlined.Lock, stringResource(R.string.person_private_account), tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(14.dp))
                }
                if (person.favorite) {
                    Spacer(Modifier.width(4.dp))
                    Icon(Icons.Filled.Star, stringResource(R.string.cocktail_favorite), tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                }
                Spacer(Modifier.width(6.dp))
                RoleBadge(p.role, showUser = true)
            }
        },
        supportingContent = {
            val status = when {
                person.followsMe == FollowState.REQUESTED -> stringResource(R.string.person_wants_to_follow)
                person.mutual -> stringResource(R.string.person_mutual)
                person.iFollow == FollowState.REQUESTED -> stringResource(R.string.person_request_sent)
                person.followsMe == FollowState.FOLLOWING -> stringResource(R.string.person_follows_you)
                person.iFollow == FollowState.FOLLOWING -> stringResource(R.string.person_following)
                else -> p.bio.ifBlank { stringResource(p.role.descriptionRes) }
            }
            // "@laura #4821 · Os seguís": el tag distingue a personas con el mismo nombre.
            Text(
                if (p.handle.isBlank()) status else stringResource(R.string.person_handle_status, p.handle, status),
                maxLines = 1, overflow = TextOverflow.Ellipsis
            )
        },
        trailingContent = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (showPrimaryAction) when {
                    person.followsMe == FollowState.REQUESTED ->
                        FilledTonalButton(onClick = { vm.acceptFollower(person) }) { FitText(stringResource(R.string.person_accept)) }
                    person.iFollow == FollowState.NONE -> OutlinedButton(onClick = { vm.follow(person) }) {
                        Icon(Icons.Outlined.PersonAdd, null, Modifier.size(18.dp)); Spacer(Modifier.width(4.dp))
                        FitText(if (p.privateAccount) stringResource(R.string.person_request) else stringResource(R.string.person_follow))
                    }
                    person.iFollow == FollowState.REQUESTED -> OutlinedButton(onClick = { vm.unfollow(person) }) { FitText(stringResource(R.string.action_cancel)) }
                    person.canChat -> IconButton(onClick = openChat) { Icon(Icons.AutoMirrored.Outlined.Chat, stringResource(R.string.person_write)) }
                }
                Box {
                    IconButton(onClick = { menu = true }) { Icon(Icons.Outlined.MoreVert, stringResource(R.string.chat_more_options)) }
                    DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                        if (person.canChat) DropdownMenuItem(
                            text = { FitText(stringResource(R.string.person_send_message)) },
                            leadingIcon = { Icon(Icons.AutoMirrored.Outlined.Chat, null) },
                            onClick = { menu = false; openChat() }
                        )
                        DropdownMenuItem(
                            text = { FitText(if (person.favorite) stringResource(R.string.person_remove_favorite) else stringResource(R.string.person_add_favorite)) },
                            leadingIcon = { Icon(if (person.favorite) Icons.Filled.Star else Icons.Outlined.StarOutline, null) },
                            onClick = { menu = false; vm.toggleFavorite(person) }
                        )
                        if (person.iFollow == FollowState.FOLLOWING) DropdownMenuItem(
                            text = { FitText(stringResource(R.string.person_unfollow)) },
                            leadingIcon = { Icon(Icons.Outlined.PersonRemove, null) },
                            onClick = { menu = false; vm.unfollow(person) }
                        )
                        if (person.followsMe == FollowState.FOLLOWING) DropdownMenuItem(
                            text = { FitText(stringResource(R.string.person_remove_follower)) },
                            leadingIcon = { Icon(Icons.Outlined.PersonRemove, null) },
                            onClick = { menu = false; vm.removeFollower(person) }
                        )
                        if (person.followsMe == FollowState.REQUESTED) DropdownMenuItem(
                            text = { FitText(stringResource(R.string.person_reject_request)) },
                            leadingIcon = { Icon(Icons.Outlined.PersonRemove, null) },
                            onClick = { menu = false; vm.removeFollower(person) }
                        )
                        DropdownMenuItem(
                            text = { FitText(stringResource(R.string.person_block), color = MaterialTheme.colorScheme.error) },
                            leadingIcon = { Icon(Icons.Outlined.Block, null, tint = MaterialTheme.colorScheme.error) },
                            onClick = { menu = false; vm.block(person) }
                        )
                    }
                }
            }
        },
        modifier = Modifier.clickable { if (person.canChat && person.iFollow == FollowState.FOLLOWING) openChat() else menu = true }
    )
}
