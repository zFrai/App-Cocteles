@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalCoroutinesApi::class)

package com.barcraft.app.ui.profile

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.EditNote
import androidx.compose.material.icons.outlined.Forum
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.barcraft.app.AppContainer
import com.barcraft.app.model.Post
import com.barcraft.app.model.SavedPost
import com.barcraft.app.model.UserProfile
import com.barcraft.app.ui.components.EmptyState
import com.barcraft.app.ui.components.LoadingBox
import com.barcraft.app.ui.navigation.LocalAppSession
import com.barcraft.app.ui.social.EditPostDialog
import com.barcraft.app.ui.social.PostCard
import com.barcraft.app.ui.social.launchSafely
import com.barcraft.app.utils.appViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

// ─────────────────────────────────────────────────────────────────────
//  Mis publicaciones: ver, editar y borrar
// ─────────────────────────────────────────────────────────────────────

class MyPostsViewModel(private val container: AppContainer, private val me: UserProfile) : ViewModel() {
    private val social = container.socialRepository

    val posts: StateFlow<List<Post>?> =
        social.observeByAuthor(me.uid).map<List<Post>, List<Post>?> { it }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    private val _messages = MutableSharedFlow<String>()
    val messages: SharedFlow<String> = _messages

    fun edit(post: Post, text: String) = launchSafely(_messages, "No se pudo editar la publicación") {
        social.editPost(post.id, text)
        _messages.emit("Publicación actualizada")
    }

    fun delete(post: Post) = launchSafely(_messages, "No se pudo borrar la publicación") {
        social.deletePost(post.id)
        if (post.mediaId.isNotBlank()) container.mediaRepository.delete(post.mediaId)
        _messages.emit("Publicación eliminada")
    }

    fun toggleLike(post: Post) = launchSafely(_messages, "No se pudo guardar el me gusta") {
        social.setLiked(post.id, me.uid, !post.isLikedBy(me.uid))
    }
}

@Composable
fun MyPostsScreen(onBack: () -> Unit, onOpenPost: (String) -> Unit) {
    val me = LocalAppSession.current.profile ?: return
    val vm = appViewModel(key = "my_posts_${me.uid}") { MyPostsViewModel(it, me) }
    val posts by vm.posts.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    var toEdit by remember { mutableStateOf<Post?>(null) }
    var toDelete by remember { mutableStateOf<Post?>(null) }
    LaunchedEffect(Unit) { vm.messages.collect { snackbar.showSnackbar(it) } }

    Scaffold(
        contentWindowInsets = WindowInsets(0),
        snackbarHost = { SnackbarHost(snackbar) },
        topBar = {
            TopAppBar(
                title = { Text("Mis publicaciones") },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Volver") } }
            )
        }
    ) { padding ->
        val list = posts
        when {
            list == null -> LoadingBox(Modifier.padding(padding))
            list.isEmpty() -> EmptyState(
                Icons.Outlined.Forum, "Aún no has publicado nada",
                "Tus publicaciones de Comunidad aparecerán aquí para editarlas o borrarlas.",
                modifier = Modifier.padding(padding)
            )
            else -> LazyColumn(
                Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(list, key = { it.id }) { post ->
                    PostCard(
                        post = post,
                        myUid = me.uid,
                        canModerate = false,
                        onOpen = { onOpenPost(post.id) },
                        onLike = { vm.toggleLike(post) },
                        onDelete = { toDelete = post },
                        onMessage = null,
                        onEdit = { toEdit = post }
                    )
                }
            }
        }
    }

    toEdit?.let { p ->
        EditPostDialog(p, onDismiss = { toEdit = null }, onSave = { vm.edit(p, it); toEdit = null })
    }
    toDelete?.let { p ->
        AlertDialog(
            onDismissRequest = { toDelete = null },
            title = { Text("¿Eliminar publicación?") },
            text = { Text("Se borrará para todos.") },
            confirmButton = {
                TextButton(onClick = { vm.delete(p); toDelete = null }) {
                    Text("Eliminar", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = { TextButton(onClick = { toDelete = null }) { Text("Cancelar") } }
        )
    }
}

// ─────────────────────────────────────────────────────────────────────
//  Guardados: publicaciones de otros, con aviso si su autor las edita
// ─────────────────────────────────────────────────────────────────────

/** Publicación guardada con su estado actual (post = null si su autor la ha borrado). */
data class SavedEntry(val saved: SavedPost, val post: Post?) {
    /** Su autor la ha cambiado desde la última vez que la vi. */
    val editedSinceSeen: Boolean get() = post != null && post.editedAt > saved.seenEditAt
}

class SavedPostsViewModel(private val container: AppContainer, private val me: UserProfile) : ViewModel() {
    private val social = container.socialRepository

    /** null = cargando. Se escuchan como mucho [MAX_SAVED] publicaciones a la vez. */
    val entries: StateFlow<List<SavedEntry>?> =
        social.observeSaved(me.uid).flatMapLatest { saved ->
            val flows = saved.take(MAX_SAVED).map { s -> social.observePost(s.postId).map { SavedEntry(s, it) } }
            if (flows.isEmpty()) flowOf(emptyList()) else combine(flows) { it.toList() }
        }.map<List<SavedEntry>, List<SavedEntry>?> { it }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    /** Cuántas guardadas han cambiado (para el aviso en Perfil). */
    val updatedCount: StateFlow<Int> =
        entries.map { list -> list?.count { it.editedSinceSeen } ?: 0 }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)

    private val _messages = MutableSharedFlow<String>()
    val messages: SharedFlow<String> = _messages

    /** Al abrirla, el aviso de "ha cambiado su post" desaparece. */
    fun open(entry: SavedEntry) {
        val post = entry.post ?: return
        if (entry.editedSinceSeen) launchSafely(_messages, "No se pudo actualizar el aviso") { social.markEditSeen(me.uid, post) }
    }

    fun unsave(entry: SavedEntry) = launchSafely(_messages, "No se pudo quitar") {
        social.unsavePost(me.uid, entry.saved.postId)
        _messages.emit("Quitada de Guardados")
    }

    fun toggleLike(post: Post) = launchSafely(_messages, "No se pudo guardar el me gusta") {
        social.setLiked(post.id, me.uid, !post.isLikedBy(me.uid))
    }

    private companion object {
        const val MAX_SAVED = 50
    }
}

@Composable
fun SavedPostsScreen(onBack: () -> Unit, onOpenPost: (String) -> Unit) {
    val me = LocalAppSession.current.profile ?: return
    val vm = appViewModel(key = "saved_${me.uid}") { SavedPostsViewModel(it, me) }
    val entries by vm.entries.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    LaunchedEffect(Unit) { vm.messages.collect { snackbar.showSnackbar(it) } }

    Scaffold(
        contentWindowInsets = WindowInsets(0),
        snackbarHost = { SnackbarHost(snackbar) },
        topBar = {
            TopAppBar(
                title = { Text("Guardados") },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Volver") } }
            )
        }
    ) { padding ->
        val list = entries
        when {
            list == null -> LoadingBox(Modifier.padding(padding))
            list.isEmpty() -> EmptyState(
                Icons.Outlined.BookmarkBorder, "Nada guardado",
                "En Comunidad, toca el marcador de una publicación para guardarla aquí.",
                modifier = Modifier.padding(padding)
            )
            else -> LazyColumn(
                Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(list, key = { it.saved.postId }) { entry ->
                    val post = entry.post
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        if (entry.editedSinceSeen && post != null) EditedNotice("${post.authorHandle} ha cambiado su post")
                        if (post == null) {
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                                Text(
                                    "Su autor ha borrado esta publicación.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.weight(1f)
                                )
                                TextButton(onClick = { vm.unsave(entry) }) { Text("Quitar") }
                            }
                        } else {
                            PostCard(
                                post = post,
                                myUid = me.uid,
                                canModerate = false,
                                onOpen = { vm.open(entry); onOpenPost(post.id) },
                                onLike = { vm.toggleLike(post) },
                                onDelete = {},
                                onMessage = null,
                                saved = true,
                                onToggleSave = { vm.unsave(entry) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun EditedNotice(text: String) {
    Surface(
        color = MaterialTheme.colorScheme.tertiaryContainer,
        contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
        shape = MaterialTheme.shapes.small
    ) {
        Row(Modifier.padding(horizontal = 12.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Outlined.EditNote, null, Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text(text, style = MaterialTheme.typography.labelLarge)
        }
    }
}
