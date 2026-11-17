@file:OptIn(ExperimentalMaterial3Api::class)

package com.barcraft.app.ui.chat

import androidx.compose.ui.res.stringResource
import com.barcraft.app.R
import com.barcraft.app.ui.components.FitText
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.PersonSearch
import androidx.compose.material.icons.outlined.QrCodeScanner
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.barcraft.app.model.Role
import com.barcraft.app.ui.components.EmptyState
import com.barcraft.app.ui.components.LoadingBox
import com.barcraft.app.ui.navigation.LocalAppSession
import com.barcraft.app.ui.qr.ProfileQr
import com.barcraft.app.utils.appViewModel
import kotlinx.coroutines.launch

/**
 * Pestaña Buscar: directorio de personas para buscar, añadir, marcar favoritos, bloquear y escribir.
 * [onBack] es null cuando se muestra como pestaña de la barra inferior.
 */
@Composable
fun PeopleScreen(onBack: (() -> Unit)?, onRoomReady: (String) -> Unit) {
    val me = LocalAppSession.current.profile ?: return
    val vm = appViewModel(key = "people_dir_${me.uid}") { PeopleViewModel(it, me) }
    val people by vm.state.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    var query by rememberSaveable { mutableStateOf("") }
    var roleFilter by rememberSaveable { mutableStateOf<Role?>(null) }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    LaunchedEffect(Unit) { vm.messages.collect { snackbar.showSnackbar(it) } }

    Scaffold(
        contentWindowInsets = WindowInsets(0),
        snackbarHost = { SnackbarHost(snackbar) },
        topBar = {
            TopAppBar(
                title = { FitText(stringResource(R.string.people_title)) },
                navigationIcon = {
                    if (onBack != null) IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.action_back)) }
                },
                actions = {
                    // Escanear el QR de otro perfil: le sigue (o le envía solicitud) automáticamente.
                    IconButton(onClick = {
                        ProfileQr.scan(
                            context,
                            onUid = { uid ->
                                people.byUid(uid)?.let { query = it.profile.name }
                                vm.followFromQr(uid)
                            },
                            onError = { msg -> scope.launch { snackbar.showSnackbar(msg) } }
                        )
                    }) { Icon(Icons.Outlined.QrCodeScanner, stringResource(R.string.people_scan_qr)) }
                }
            )
        }
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            OutlinedTextField(
                value = query, onValueChange = { query = it },
                placeholder = { Text(stringResource(R.string.people_search_hint)) },
                leadingIcon = { Icon(Icons.Filled.Search, null) },
                singleLine = true,
                shape = MaterialTheme.shapes.extraLarge,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)
            )
            LazyRow(contentPadding = PaddingValues(horizontal = 16.dp), horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(8.dp)) {
                item { FilterChip(selected = roleFilter == null, onClick = { roleFilter = null }, label = { FitText(stringResource(R.string.people_filter_all)) }) }
                items(Role.entries) { r ->
                    FilterChip(selected = roleFilter == r, onClick = { roleFilter = if (roleFilter == r) null else r }, label = { FitText(stringResource(r.labelRes)) })
                }
            }
            val list = people.everyone.filter {
                !it.blocked && !it.profile.banned &&
                    (roleFilter == null || it.profile.role == roleFilter) &&
                    it.profile.matches(query)
            }.sortedWith(compareByDescending<Person> { it.favorite }.thenBy { it.profile.name.lowercase() })

            when {
                !people.loaded -> LoadingBox()
                list.isEmpty() -> EmptyState(
                    Icons.Outlined.PersonSearch, stringResource(R.string.people_empty_title),
                    if (query.isBlank()) stringResource(R.string.people_empty_message) else stringResource(R.string.people_no_match, query)
                )
                else -> LazyColumn(contentPadding = PaddingValues(bottom = 24.dp)) {
                    items(list, key = { it.profile.uid }) { person ->
                        PersonRow(person, vm, onOpenChat = onRoomReady)
                    }
                }
            }
        }
    }
}
