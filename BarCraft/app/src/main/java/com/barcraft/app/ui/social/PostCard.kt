package com.barcraft.app.ui.social

import androidx.compose.ui.res.stringResource
import com.barcraft.app.R
import com.barcraft.app.ui.components.FitText
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Chat
import androidx.compose.material.icons.automirrored.outlined.Send
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.LocalBar
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.barcraft.app.model.Post
import com.barcraft.app.ui.components.CocktailImage
import com.barcraft.app.ui.components.RemoteImage
import com.barcraft.app.ui.components.RoleBadge
import com.barcraft.app.ui.components.UserAvatar
import com.barcraft.app.utils.DateUtils

/** Tarjeta de publicación del muro. */
@Composable
fun PostCard(
    post: Post,
    myUid: String?,
    canModerate: Boolean,
    onOpen: (() -> Unit)?,
    onLike: () -> Unit,
    onDelete: () -> Unit,
    onMessage: (() -> Unit)?,
    modifier: Modifier = Modifier,
    expandedText: Boolean = false,
    /** Guardar en "Guardados" (solo publicaciones de otros). Null = no se muestra. */
    saved: Boolean = false,
    onToggleSave: (() -> Unit)? = null,
    /** Editar el texto (solo el autor). Null = no se ofrece. */
    onEdit: (() -> Unit)? = null
) {
    var menu by remember { mutableStateOf(false) }
    val isMine = post.authorId == myUid
    val liked = post.isLikedBy(myUid)

    Card(
        onClick = { onOpen?.invoke() },
        enabled = onOpen != null,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
            disabledContainerColor = MaterialTheme.colorScheme.surfaceContainerLow,
            disabledContentColor = MaterialTheme.colorScheme.onSurface
        ),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                UserAvatar(post.authorName, size = 40.dp, uid = post.authorId)
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(post.authorName, style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Spacer(Modifier.width(6.dp))
                        RoleBadge(post.authorRole)
                    }
                    Text(
                        DateUtils.relative(post.createdAt) + if (post.editedAt > 0) stringResource(R.string.post_edited_suffix) else "",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                if (isMine || canModerate) Box {
                    IconButton(onClick = { menu = true }) { Icon(Icons.Outlined.MoreVert, stringResource(R.string.post_options)) }
                    DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                        if (isMine && onEdit != null) DropdownMenuItem(
                            text = { Text(stringResource(R.string.action_edit)) },
                            leadingIcon = { Icon(Icons.Outlined.Edit, null) },
                            onClick = { menu = false; onEdit() }
                        )
                        DropdownMenuItem(
                            text = { FitText(if (isMine) stringResource(R.string.action_delete) else stringResource(R.string.post_delete_moderation)) },
                            leadingIcon = { Icon(Icons.Outlined.Delete, null) },
                            onClick = { menu = false; onDelete() }
                        )
                    }
                }
            }

            if (post.text.isNotBlank()) {
                Spacer(Modifier.height(10.dp))
                Text(
                    post.text,
                    style = MaterialTheme.typography.bodyLarge,
                    maxLines = if (expandedText) Int.MAX_VALUE else 6,
                    overflow = TextOverflow.Ellipsis
                )
            }

            if (post.mediaId.isNotBlank()) {
                Spacer(Modifier.height(10.dp))
                RemoteImage(post.mediaId, Modifier.fillMaxWidth().height(220.dp).clip(MaterialTheme.shapes.medium))
            } else if (post.imageUrl.isNotBlank()) {
                Spacer(Modifier.height(10.dp))
                CocktailImage(
                    post.imageUrl, null,
                    Modifier.fillMaxWidth().height(200.dp).clip(MaterialTheme.shapes.medium)
                )
            }

            if (post.cocktailName.isNotBlank()) {
                Spacer(Modifier.height(10.dp))
                Surface(
                    color = MaterialTheme.colorScheme.secondaryContainer,
                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                    shape = MaterialTheme.shapes.medium,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(Modifier.padding(12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.LocalBar, null, Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text(post.cocktailName, style = MaterialTheme.typography.titleSmall)
                        }
                        if (post.cocktailSummary.isNotBlank()) {
                            Spacer(Modifier.height(4.dp))
                            Text(
                                post.cocktailSummary,
                                style = MaterialTheme.typography.bodyMedium,
                                maxLines = if (expandedText) Int.MAX_VALUE else 4,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(4.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                TextButton(onClick = onLike, enabled = myUid != null) {
                    Icon(
                        if (liked) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder, stringResource(R.string.post_like),
                        tint = if (liked) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(Modifier.width(6.dp))
                    FitText("${post.likes.size}")
                }
                TextButton(onClick = { onOpen?.invoke() }, enabled = onOpen != null) {
                    Icon(Icons.AutoMirrored.Outlined.Chat, stringResource(R.string.post_comments), modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(6.dp))
                    FitText("${post.commentCount}")
                }
                Spacer(Modifier.weight(1f))
                if (!isMine && onToggleSave != null) {
                    IconButton(onClick = onToggleSave) {
                        Icon(
                            if (saved) Icons.Filled.Bookmark else Icons.Outlined.BookmarkBorder,
                            if (saved) stringResource(R.string.post_unsave) else stringResource(R.string.post_save),
                            tint = if (saved) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                if (!isMine && onMessage != null) {
                    TextButton(onClick = onMessage) {
                        Icon(Icons.AutoMirrored.Outlined.Send, null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        FitText(stringResource(R.string.post_message))
                    }
                }
            }
        }
    }
}

/** Diálogo para editar el texto de una publicación propia. */
@Composable
fun EditPostDialog(post: Post, onDismiss: () -> Unit, onSave: (String) -> Unit) {
    var text by androidx.compose.runtime.saveable.rememberSaveable(post.id) { mutableStateOf(post.text) }
    androidx.compose.material3.AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.post_edit_title)) },
        text = {
            Column {
                androidx.compose.material3.OutlinedTextField(
                    value = text, onValueChange = { text = it.take(2000) },
                    minLines = 4, modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    stringResource(R.string.post_edit_warning),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        confirmButton = {
            androidx.compose.material3.Button(onClick = { onSave(text) }, enabled = text.isNotBlank() && text.trim() != post.text) {
                Text(stringResource(R.string.post_save_changes))
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) } }
    )
}
