@file:OptIn(ExperimentalMaterial3Api::class)

package com.barcraft.app.ui.admin

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.Block
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.PersonOff
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.barcraft.app.model.Post
import com.barcraft.app.model.Role
import com.barcraft.app.model.UserProfile
import com.barcraft.app.ui.components.LoadingBox
import com.barcraft.app.ui.components.RoleBadge
import com.barcraft.app.ui.components.UserAvatar
import com.barcraft.app.ui.navigation.LocalAppSession
import com.barcraft.app.utils.DateUtils
import com.barcraft.app.utils.appViewModel

@Composable
fun AdminScreen(onOpenPost: (String) -> Unit) {
    val me = LocalAppSession.current.profile ?: return
    val vm = appViewModel { AdminViewModel(it.userRepository, it.socialRepository) }
    val users by vm.allUsers.collectAsStateWithLifecycle()
    val posts by vm.posts.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    var tab by rememberSaveable { mutableIntStateOf(0) }
    var query by rememberSaveable { mutableStateOf("") }
    var postToDelete by remember { mutableStateOf<Post?>(null) }

    LaunchedEffect(Unit) { vm.messages.collect { snackbar.showSnackbar(it) } }

    Scaffold(
        contentWindowInsets = WindowInsets(0),
        snackbarHost = { SnackbarHost(snackbar) },
        topBar = {
            TopAppBar(title = {
                Column {
                    Text("Panel de Admin")
                    Text(
                        "Gestiona usuarios y modera la comunidad",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            })
        }
    ) { padding ->
        val list = users
        if (list == null) { LoadingBox(Modifier.padding(padding)); return@Scaffold }

        Column(Modifier.fillMaxSize().padding(padding)) {
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                StatTile("Barmans", list.count { it.role == Role.BARMAN }, Modifier.weight(1f))
                StatTile("Comensales", list.count { it.role == Role.CLIENTE }, Modifier.weight(1f))
                StatTile("Admins", list.count { it.isAdmin }, Modifier.weight(1f))
                StatTile("Bloq.", list.count { it.banned }, Modifier.weight(1f))
            }
            PrimaryTabRow(selectedTabIndex = tab) {
                Tab(selected = tab == 0, onClick = { tab = 0 }, text = { Text("Usuarios") })
                Tab(selected = tab == 1, onClick = { tab = 1 }, text = { Text("Publicaciones") })
            }
            if (tab == 0) {
                OutlinedTextField(
                    value = query, onValueChange = { query = it },
                    placeholder = { Text("Buscar usuario o email") },
                    leadingIcon = { Icon(Icons.Filled.Search, null) },
                    singleLine = true,
                    shape = MaterialTheme.shapes.extraLarge,
                    modifier = Modifier.fillMaxWidth().padding(16.dp)
                )
                val filtered = list.filter {
                    query.isBlank() || it.name.contains(query, true) || it.email.contains(query, true)
                }
                LazyColumn(contentPadding = PaddingValues(bottom = 24.dp)) {
                    items(filtered, key = { it.uid }) { user ->
                        UserAdminRow(
                            user = user,
                            isMe = user.uid == me.uid,
                            onRole = { vm.setRole(user, it) },
                            onBan = { vm.setBanned(user, it) }
                        )
                    }
                }
            } else {
                LazyColumn(contentPadding = PaddingValues(bottom = 24.dp)) {
                    items(posts, key = { it.id }) { post ->
                        ListItem(
                            leadingContent = { UserAvatar(post.authorName, size = 36.dp) },
                            headlineContent = {
                                Text(
                                    post.text.ifBlank { post.cocktailName.ifBlank { "(sin texto)" } },
                                    maxLines = 2, overflow = TextOverflow.Ellipsis
                                )
                            },
                            supportingContent = {
                                Text("${post.authorName} · ${DateUtils.relative(post.createdAt)} · ${post.likes.size} ♥ · ${post.commentCount} coment.")
                            },
                            trailingContent = {
                                Row {
                                    TextButton(onClick = { onOpenPost(post.id) }) { Text("Ver") }
                                    TextButton(onClick = { postToDelete = post }) {
                                        Text("Borrar", color = MaterialTheme.colorScheme.error)
                                    }
                                }
                            }
                        )
                    }
                }
            }
        }
    }

    postToDelete?.let { p ->
        AlertDialog(
            onDismissRequest = { postToDelete = null },
            title = { Text("¿Borrar publicación?") },
            text = { Text("Se eliminará la publicación de ${p.authorName} para todos.") },
            confirmButton = {
                TextButton(onClick = { vm.deletePost(p); postToDelete = null }) {
                    Text("Borrar", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = { TextButton(onClick = { postToDelete = null }) { Text("Cancelar") } }
        )
    }
}

@Composable
private fun StatTile(label: String, value: Int, modifier: Modifier = Modifier) {
    Surface(shape = MaterialTheme.shapes.medium, color = MaterialTheme.colorScheme.surfaceContainerHigh, modifier = modifier) {
        Column(Modifier.padding(vertical = 12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text("$value", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.height(2.dp))
            Text(label, style = MaterialTheme.typography.labelSmall, maxLines = 1)
        }
    }
}

@Composable
private fun UserAdminRow(
    user: UserProfile,
    isMe: Boolean,
    onRole: (Role) -> Unit,
    onBan: (Boolean) -> Unit
) {
    var menu by remember { mutableStateOf(false) }
    ListItem(
        leadingContent = { UserAvatar(user.name) },
        headlineContent = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(user.name + if (isMe) " (tú)" else "", maxLines = 1, overflow = TextOverflow.Ellipsis)
                Spacer(Modifier.width(6.dp))
                RoleBadge(user.role, showUser = true)
            }
        },
        supportingContent = {
            Text(
                if (user.banned) "Bloqueado · ${user.email}" else user.email,
                color = if (user.banned) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1, overflow = TextOverflow.Ellipsis
            )
        },
        trailingContent = {
            if (!isMe) Box {
                IconButton(onClick = { menu = true }) { Icon(Icons.Outlined.MoreVert, "Acciones") }
                DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                    Role.entries.filter { it != user.role }.forEach { r ->
                        DropdownMenuItem(
                            text = { Text("Cambiar a ${r.label}") },
                            leadingIcon = {
                                Icon(if (r == Role.ADMIN) Icons.Outlined.Shield else Icons.Outlined.PersonOff, null)
                            },
                            onClick = { menu = false; onRole(r) }
                        )
                    }
                    if (user.banned) DropdownMenuItem(
                        text = { Text("Desbloquear") },
                        leadingIcon = { Icon(Icons.Outlined.CheckCircle, null) },
                        onClick = { menu = false; onBan(false) }
                    ) else DropdownMenuItem(
                        text = { Text("Bloquear cuenta") },
                        leadingIcon = { Icon(Icons.Outlined.Block, null) },
                        onClick = { menu = false; onBan(true) }
                    )
                }
            }
        }
    )
}
