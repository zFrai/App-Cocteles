@file:OptIn(ExperimentalMaterial3Api::class)

package com.barcraft.app.ui.social

import com.barcraft.app.ui.components.FitText
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.material3.TabRow
import androidx.compose.material3.Tab
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Public
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.barcraft.app.model.Permissions
import com.barcraft.app.model.Post
import com.barcraft.app.ui.components.AccountRequiredState
import com.barcraft.app.ui.components.EmptyState
import com.barcraft.app.ui.components.LoadingBox
import com.barcraft.app.ui.navigation.LocalAppSession
import com.barcraft.app.utils.appViewModel

@Composable
fun FeedScreen(
    onLogin: () -> Unit,
    onOpenPost: (String) -> Unit,
    onCompose: () -> Unit,
    onMessageUser: (String) -> Unit,
    onOpenRecipe: (authorId: String, recipeId: String) -> Unit
) {
    val session = LocalAppSession.current
    val me = session.profile
    val snackbar = remember { SnackbarHostState() }

    Scaffold(
        contentWindowInsets = WindowInsets(0),
        snackbarHost = { SnackbarHost(snackbar) },
        topBar = {
            TopAppBar(title = {
                Column {
                    FitText("Comunidad")
                    FitText(
                        "Recetas y publicaciones de BarCraft",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            })
        },
        floatingActionButton = {
            if (me != null && Permissions.canPublishPosts(me.role)) ExtendedFloatingActionButton(
                onClick = onCompose,
                icon = { Icon(Icons.Outlined.Edit, null) },
                text = { FitText("Publicar") }
            )
        }
    ) { padding ->
        if (me == null) {
            AccountRequiredState(session, "La comunidad", onLogin)
            return@Scaffold
        }
        val vm = appViewModel(key = "feed_${me.uid}") { FeedViewModel(it, me) }
        val canPublish = Permissions.canPublishPosts(me.role)
        val posts by vm.posts.collectAsStateWithLifecycle()
        val recipes by vm.recipes.collectAsStateWithLifecycle()
        var toDelete by remember { mutableStateOf<Post?>(null) }
        var tab by rememberSaveable { mutableIntStateOf(0) }
        LaunchedEffect(Unit) { vm.messages.collect { snackbar.showSnackbar(it) } }

        Column(Modifier.fillMaxSize().padding(padding)) {
            TabRow(selectedTabIndex = tab) {
                Tab(selected = tab == 0, onClick = { tab = 0 }, text = { FitText("Recetas") })
                Tab(selected = tab == 1, onClick = { tab = 1 }, text = { FitText("Publicaciones") })
            }
            // Recetas: públicas + las de "Seguidores" de las cuentas que sigo.
            if (tab == 0) {
                SharedRecipeList(recipes, onOpen = { onOpenRecipe(it.authorId, it.id) })
                return@Column
            }

            val list = posts
            when {
                list == null -> LoadingBox()
                list.isEmpty() -> EmptyState(
                    icon = Icons.Outlined.Public,
                    title = "Todavía no hay publicaciones",
                    message = if (canPublish) "Sé el primero en compartir un cóctel con la comunidad."
                    else "Cuando los barmans compartan sus cócteles los verás aquí.",
                    actionLabel = if (canPublish) "Crear publicación" else null,
                    onAction = if (canPublish) onCompose else null
                )
                else -> LazyColumn(
                    Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 96.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(list, key = { it.id }) { post ->
                        PostCard(
                            post = post,
                            myUid = me.uid,
                            canModerate = session.isAdmin,
                            onOpen = { onOpenPost(post.id) },
                            onLike = { vm.toggleLike(post, me.uid) },
                            onDelete = { toDelete = post },
                            onMessage = { vm.messageAuthor(me, post, onMessageUser) }
                        )
                    }
                }
            }
        }

        toDelete?.let { p ->
            AlertDialog(
                onDismissRequest = { toDelete = null },
                title = { Text("¿Eliminar publicación?") },
                text = { Text(if (p.authorId == me.uid) "Se borrará para todos." else "Vas a borrar la publicación de ${p.authorName} como Admin.") },
                confirmButton = {
                    TextButton(onClick = { vm.delete(p); toDelete = null }) {
                        FitText("Eliminar", color = MaterialTheme.colorScheme.error)
                    }
                },
                dismissButton = { TextButton(onClick = { toDelete = null }) { FitText("Cancelar") } }
            )
        }
    }
}
