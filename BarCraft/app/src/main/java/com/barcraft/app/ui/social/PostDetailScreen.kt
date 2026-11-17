@file:OptIn(ExperimentalMaterial3Api::class)

package com.barcraft.app.ui.social

import com.barcraft.app.ui.components.FitText
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.barcraft.app.model.Comment
import com.barcraft.app.ui.components.LoadingBox
import com.barcraft.app.ui.components.UserAvatar
import com.barcraft.app.ui.navigation.LocalAppSession
import com.barcraft.app.utils.DateUtils
import com.barcraft.app.utils.appViewModel

@Composable
fun PostDetailScreen(postId: String, onBack: () -> Unit) {
    val session = LocalAppSession.current
    val me = session.profile ?: return
    val vm = appViewModel(key = "post_$postId") { PostDetailViewModel(it.socialRepository, postId) }
    val post by vm.post.collectAsStateWithLifecycle()
    val comments by vm.comments.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    var text by rememberSaveable { mutableStateOf("") }

    LaunchedEffect(Unit) { vm.messages.collect { snackbar.showSnackbar(it) } }

    Scaffold(
        contentWindowInsets = WindowInsets(0),
        snackbarHost = { SnackbarHost(snackbar) },
        topBar = {
            TopAppBar(
                title = { FitText("Publicación") },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Volver") } }
            )
        },
        bottomBar = {
            Surface(tonalElevation = 3.dp, modifier = Modifier.imePadding()) {
                Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        value = text, onValueChange = { text = it },
                        placeholder = { Text("Escribe un comentario") },
                        maxLines = 4,
                        shape = MaterialTheme.shapes.extraLarge,
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(Modifier.width(8.dp))
                    FilledIconButton(
                        onClick = { vm.comment(me, text); text = "" },
                        enabled = text.isNotBlank()
                    ) { Icon(Icons.AutoMirrored.Filled.Send, "Enviar") }
                }
            }
        }
    ) { padding ->
        val p = post
        if (p == null) { LoadingBox(Modifier.padding(padding)); return@Scaffold }
        LazyColumn(Modifier.fillMaxSize().padding(padding), contentPadding = PaddingValues(16.dp)) {
            item {
              Column {
                PostCard(
                    post = p,
                    myUid = me.uid,
                    canModerate = session.isAdmin,
                    onOpen = null,
                    onLike = { vm.toggleLike(me.uid) },
                    onDelete = { vm.deletePost(onBack) },
                    onMessage = null,
                    expandedText = true
                )
                Spacer(Modifier.padding(8.dp))
                Text("Comentarios (${comments.size})", style = MaterialTheme.typography.titleSmall)
                HorizontalDivider(Modifier.padding(vertical = 8.dp))
              }
            }
            if (comments.isEmpty()) item {
                Text(
                    "Nadie ha comentado todavía.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
            items(comments, key = { it.id }) { c ->
                CommentRow(
                    comment = c,
                    canDelete = c.authorId == me.uid || session.isAdmin,
                    onDelete = { vm.deleteComment(c) }
                )
            }
        }
    }
}

@Composable
private fun CommentRow(comment: Comment, canDelete: Boolean, onDelete: () -> Unit) {
    Row(Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
        UserAvatar(comment.authorName, size = 34.dp)
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(comment.authorName, style = MaterialTheme.typography.titleSmall)
                Spacer(Modifier.width(8.dp))
                Text(
                    DateUtils.relative(comment.createdAt),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Text(comment.text, style = MaterialTheme.typography.bodyMedium)
        }
        if (canDelete) IconButton(onClick = onDelete) {
            Icon(Icons.Outlined.Delete, "Borrar comentario", tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
