@file:OptIn(ExperimentalMaterial3Api::class)

package com.barcraft.app.ui.social

import androidx.compose.ui.res.stringResource
import com.barcraft.app.R
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
import com.barcraft.app.ui.components.LoadProblemBanner
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
                    FitText(stringResource(R.string.feed_title))
                    FitText(
                        stringResource(R.string.feed_subtitle),
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
                text = { FitText(stringResource(R.string.feed_publish)) }
            )
        }
    ) { padding ->
        if (me == null) {
            AccountRequiredState(session, stringResource(R.string.feature_community), onLogin)
            return@Scaffold
        }
        val vm = appViewModel(key = "feed_${me.uid}") { FeedViewModel(it, me) }
        val canPublish = Permissions.canPublishPosts(me.role)
        val posts by vm.posts.collectAsStateWithLifecycle()
        val recipes by vm.recipes.collectAsStateWithLifecycle()
        val recipesProblem by vm.recipesProblem.collectAsStateWithLifecycle()
        val postsProblem by vm.postsProblem.collectAsStateWithLifecycle()
        var toDelete by remember { mutableStateOf<Post?>(null) }
        var toEdit by remember { mutableStateOf<Post?>(null) }
        val savedIds by vm.savedIds.collectAsStateWithLifecycle()
        var tab by rememberSaveable { mutableIntStateOf(0) }
        LaunchedEffect(Unit) { vm.messages.collect { snackbar.showSnackbar(it) } }

        Column(Modifier.fillMaxSize().padding(padding)) {
            TabRow(selectedTabIndex = tab) {
                Tab(selected = tab == 0, onClick = { tab = 0 }, text = { FitText(stringResource(R.string.feed_tab_recipes)) })
                Tab(selected = tab == 1, onClick = { tab = 1 }, text = { FitText(stringResource(R.string.feed_tab_posts)) })
            }
            // Recetas: públicas + las de "Seguidores" de las cuentas que sigo.
            if (tab == 0) {
                SharedRecipeList(recipes, onOpen = { onOpenRecipe(it.authorId, it.id) }, problem = recipesProblem)
                return@Column
            }

            postsProblem?.let { LoadProblemBanner(it) }
            val list = posts
            when {
                list == null -> LoadingBox()
                list.isEmpty() -> EmptyState(
                    icon = Icons.Outlined.Public,
                    title = stringResource(R.string.feed_empty_title),
                    message = if (canPublish) stringResource(R.string.feed_empty_can_publish)
                    else stringResource(R.string.feed_empty_cannot_publish),
                    actionLabel = if (canPublish) stringResource(R.string.feed_empty_action) else null,
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
                            onMessage = { vm.messageAuthor(me, post, onMessageUser) },
                            saved = post.id in savedIds,
                            onToggleSave = { vm.toggleSave(post) },
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
                title = { Text(stringResource(R.string.feed_delete_title)) },
                text = { Text(if (p.authorId == me.uid) stringResource(R.string.feed_delete_mine) else stringResource(R.string.feed_delete_as_admin, p.authorName)) },
                confirmButton = {
                    TextButton(onClick = { vm.delete(p); toDelete = null }) {
                        FitText(stringResource(R.string.action_delete), color = MaterialTheme.colorScheme.error)
                    }
                },
                dismissButton = { TextButton(onClick = { toDelete = null }) { FitText(stringResource(R.string.action_cancel)) } }
            )
        }
    }
}
