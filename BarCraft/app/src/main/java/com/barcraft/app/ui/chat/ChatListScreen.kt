@file:OptIn(ExperimentalMaterial3Api::class)

package com.barcraft.app.ui.chat

import com.barcraft.app.ui.components.FitText
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.Block
import androidx.compose.material.icons.outlined.Forum
import androidx.compose.material.icons.outlined.Group
import androidx.compose.material.icons.outlined.PersonAdd
import androidx.compose.material.icons.outlined.PersonSearch
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.barcraft.app.ui.components.AccountRequiredState
import com.barcraft.app.ui.components.EmptyState
import com.barcraft.app.ui.components.LoadingBox
import com.barcraft.app.ui.components.UserAvatar
import com.barcraft.app.ui.navigation.LocalAppSession
import com.barcraft.app.utils.DateUtils
import com.barcraft.app.utils.appViewModel

@Composable
fun ChatListScreen(onLogin: () -> Unit, onOpenRoom: (String) -> Unit, onFindPeople: () -> Unit) {
    val session = LocalAppSession.current
    val me = session.profile
    val snackbar = remember { SnackbarHostState() }
    var tab by rememberSaveable { mutableIntStateOf(0) }

    Scaffold(
        contentWindowInsets = WindowInsets(0),
        snackbarHost = { SnackbarHost(snackbar) },
        topBar = { TopAppBar(title = { FitText("Chats") }) },
        floatingActionButton = {
            if (me != null) ExtendedFloatingActionButton(
                onClick = onFindPeople,
                icon = { Icon(Icons.Outlined.PersonSearch, null) },
                text = { FitText("Buscar personas") }
            )
        }
    ) { padding ->
        if (me == null) {
            AccountRequiredState(session, "El chat", onLogin)
            return@Scaffold
        }
        val vm = appViewModel(key = "people_${me.uid}") { PeopleViewModel(it, me) }
        val people by vm.state.collectAsStateWithLifecycle()
        val rooms by vm.rooms.collectAsStateWithLifecycle()
        LaunchedEffect(Unit) { vm.messages.collect { snackbar.showSnackbar(it) } }

        Column(Modifier.fillMaxSize().padding(padding)) {
            ScrollableTabRow(selectedTabIndex = tab, edgePadding = 12.dp) {
                Tab(selected = tab == 0, onClick = { tab = 0 }, text = { FitText("Chats") })
                Tab(selected = tab == 1, onClick = { tab = 1 }, text = { FitText("Seguidores (${people.followers.size})") })
                Tab(selected = tab == 2, onClick = { tab = 2 }, text = { FitText("Siguiendo (${people.following.size})") })
                Tab(selected = tab == 3, onClick = { tab = 3 }, text = {
                    BadgedBox(badge = { if (people.incoming.isNotEmpty()) Badge { Text("${people.incoming.size}") } }) {
                        FitText("Solicitudes")
                    }
                })
                Tab(selected = tab == 4, onClick = { tab = 4 }, text = { FitText("Bloqueados") })
            }

            if (!people.loaded) { LoadingBox(); return@Column }

            when (tab) {
                0 -> {
                    val list = rooms
                    when {
                        list == null -> LoadingBox()
                        list.isEmpty() -> EmptyState(
                            Icons.Outlined.Forum, "Sin conversaciones",
                            "Busca a otra persona y empieza a hablar de cócteles en tiempo real.",
                            actionLabel = "Buscar personas", onAction = onFindPeople
                        )
                        else -> {
                            // Favoritos arriba, luego por último mensaje. Los bloqueados no se muestran aquí.
                            val sorted = list
                                .filter { people.byUid(it.otherId(me.uid))?.blocked != true }
                                .sortedWith(
                                    compareByDescending<com.barcraft.app.model.ChatRoom> {
                                        people.byUid(it.otherId(me.uid))?.favorite == true
                                    }.thenByDescending { it.updatedAt }
                                )
                            LazyColumn(contentPadding = PaddingValues(bottom = 96.dp)) {
                                items(sorted, key = { it.id }) { room ->
                                    val name = room.otherName(me.uid)
                                    val fav = people.byUid(room.otherId(me.uid))?.favorite == true
                                    ListItem(
                                        leadingContent = { UserAvatar(name) },
                                        headlineContent = {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Text(name, style = MaterialTheme.typography.titleMedium)
                                                if (fav) {
                                                    Spacer(Modifier.width(4.dp))
                                                    Icon(Icons.Filled.Star, "Favorito", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                                                }
                                            }
                                        },
                                        supportingContent = {
                                            Text(
                                                when {
                                                    room.isBlocked -> "Conversación bloqueada"
                                                    room.lastMessage.isBlank() -> "Di hola 👋"
                                                    room.lastSenderId == me.uid -> "Tú: ${room.lastMessage}"
                                                    else -> room.lastMessage
                                                },
                                                maxLines = 1, overflow = TextOverflow.Ellipsis
                                            )
                                        },
                                        trailingContent = {
                                            if (room.lastMessage.isNotBlank()) Text(DateUtils.relative(room.updatedAt), style = MaterialTheme.typography.labelSmall)
                                        },
                                        modifier = Modifier.clickable { onOpenRoom(room.id) }
                                    )
                                }
                            }
                        }
                    }
                }
                1 -> if (people.followers.isEmpty()) EmptyState(
                    Icons.Outlined.Group, "Aún no te sigue nadie",
                    "Comparte tu @nombre #1234 para que te encuentren. Marca con ★ a tus favoritos para tenerlos siempre arriba."
                ) else LazyColumn(contentPadding = PaddingValues(bottom = 96.dp)) {
                    items(people.followers, key = { it.profile.uid }) { PersonRow(it, vm, onOpenRoom) }
                }
                2 -> if (people.following.isEmpty()) EmptyState(
                    Icons.Outlined.Group, "Aún no sigues a nadie",
                    "Busca personas y síguelas. Para chatear, su cuenta tiene que ser pública o tenéis que seguiros los dos.",
                    actionLabel = "Buscar personas", onAction = onFindPeople
                ) else LazyColumn(contentPadding = PaddingValues(bottom = 96.dp)) {
                    items(people.following, key = { it.profile.uid }) { PersonRow(it, vm, onOpenRoom) }
                }
                3 -> if (people.incoming.isEmpty() && people.outgoing.isEmpty()) EmptyState(
                    Icons.Outlined.PersonAdd, "Sin solicitudes",
                    "Si tu cuenta es privada, aquí verás quién quiere seguirte. También las solicitudes que envíes a cuentas privadas."
                ) else LazyColumn(contentPadding = PaddingValues(bottom = 96.dp)) {
                    if (people.incoming.isNotEmpty()) item { SubHeader("Recibidas") }
                    items(people.incoming, key = { "in_" + it.profile.uid }) { PersonRow(it, vm, onOpenRoom) }
                    if (people.outgoing.isNotEmpty()) item { SubHeader("Enviadas") }
                    items(people.outgoing, key = { "out_" + it.profile.uid }) { PersonRow(it, vm, onOpenRoom) }
                }
                else -> if (people.blocked.isEmpty()) EmptyState(
                    Icons.Outlined.Block, "Nadie bloqueado", "Las personas que bloquees no podrán escribirte."
                ) else LazyColumn(contentPadding = PaddingValues(bottom = 96.dp)) {
                    items(people.blocked, key = { it.profile.uid }) { p ->
                        ListItem(
                            leadingContent = { UserAvatar(p.profile.name) },
                            headlineContent = { Text(p.profile.name) },
                            supportingContent = { Text("Bloqueado") },
                            trailingContent = {
                                OutlinedButton(onClick = { vm.unblock(p.profile.uid, p.profile.name) }) { FitText("Desbloquear") }
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SubHeader(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(start = 16.dp, top = 12.dp, bottom = 4.dp)
    )
}
