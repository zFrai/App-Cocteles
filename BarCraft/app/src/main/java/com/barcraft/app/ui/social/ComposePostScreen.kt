@file:OptIn(ExperimentalMaterial3Api::class)

package com.barcraft.app.ui.social

import androidx.compose.ui.res.stringResource
import com.barcraft.app.R
import com.barcraft.app.ui.components.FitText
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.LocalBar
import androidx.compose.material.icons.outlined.Link
import androidx.compose.material.icons.outlined.PhotoCamera
import androidx.compose.material.icons.outlined.PhotoLibrary
import androidx.compose.material3.Button
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import coil.compose.AsyncImage
import com.barcraft.app.ui.components.rememberPhotoPicker
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.barcraft.app.ui.components.SectionTitle
import com.barcraft.app.ui.components.UserAvatar
import com.barcraft.app.ui.navigation.LocalAppSession
import com.barcraft.app.utils.appViewModel

@Composable
fun ComposePostScreen(initialCocktailId: Long, onDone: () -> Unit, onBack: () -> Unit) {
    val me = LocalAppSession.current.profile ?: return
    val vm = appViewModel(key = "compose_$initialCocktailId") {
        ComposePostViewModel(it, initialCocktailId)
    }
    val cocktails by vm.myCocktails.collectAsStateWithLifecycle()
    val attached by vm.attachedId.collectAsStateWithLifecycle()
    val sending by vm.sending.collectAsStateWithLifecycle()
    val photo by vm.photo.collectAsStateWithLifecycle()
    val picker = rememberPhotoPicker { vm.setPhoto(it) }
    val snackbar = remember { SnackbarHostState() }

    var text by rememberSaveable { mutableStateOf("") }
    var imageUrl by rememberSaveable { mutableStateOf("") }

    LaunchedEffect(Unit) { vm.messages.collect { snackbar.showSnackbar(it) } }

    Scaffold(
        contentWindowInsets = WindowInsets(0),
        snackbarHost = { SnackbarHost(snackbar) },
        topBar = {
            TopAppBar(
                title = { FitText(stringResource(R.string.compose_title)) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Filled.Close, stringResource(R.string.action_cancel)) } },
                actions = {
                    Button(
                        onClick = { vm.publish(me, text, imageUrl, onDone) },
                        enabled = !sending,
                        modifier = Modifier.padding(end = 8.dp)
                    ) {
                        if (sending) CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
                        else FitText(stringResource(R.string.compose_publish))
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            Modifier.fillMaxSize().padding(padding).imePadding(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    UserAvatar(me.name, size = 40.dp)
                    Spacer(Modifier.width(12.dp))
                    Text(me.name, style = MaterialTheme.typography.titleMedium)
                }
            }
            item {
                OutlinedTextField(
                    value = text, onValueChange = { text = it },
                    placeholder = { Text(stringResource(R.string.compose_hint)) },
                    minLines = 5,
                    modifier = Modifier.fillMaxWidth()
                )
            }
            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    val p = photo
                    if (p != null) {
                        Box {
                            AsyncImage(
                                model = p, contentDescription = null, contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxWidth().height(220.dp).clip(MaterialTheme.shapes.medium)
                            )
                            FilledIconButton(
                                onClick = { vm.setPhoto(null) },
                                modifier = Modifier.align(Alignment.TopEnd).padding(8.dp)
                            ) { Icon(Icons.Filled.Close, stringResource(R.string.compose_remove_photo)) }
                        }
                    }
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilledTonalButton(onClick = { picker.camera() }, modifier = Modifier.weight(1f)) {
                            Icon(Icons.Outlined.PhotoCamera, null); Spacer(Modifier.width(6.dp)); FitText(stringResource(R.string.compose_camera))
                        }
                        FilledTonalButton(onClick = { picker.gallery() }, modifier = Modifier.weight(1f)) {
                            Icon(Icons.Outlined.PhotoLibrary, null); Spacer(Modifier.width(6.dp)); FitText(stringResource(R.string.compose_gallery))
                        }
                    }
                }
            }
            if (photo == null) item {
                OutlinedTextField(
                    value = imageUrl, onValueChange = { imageUrl = it },
                    label = { Text(stringResource(R.string.compose_photo_url)) },
                    leadingIcon = { Icon(Icons.Outlined.Link, null) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
            item {
                Column {
                    SectionTitle(stringResource(R.string.compose_attach_recipe))
                    if (cocktails.isEmpty()) {
                        Text(
                            stringResource(R.string.compose_no_recipes),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(cocktails, key = { it.cocktail.id }) { c ->
                                FilterChip(
                                    selected = attached == c.cocktail.id,
                                    onClick = { vm.attach(if (attached == c.cocktail.id) 0 else c.cocktail.id) },
                                    label = { FitText(c.cocktail.name) },
                                    leadingIcon = { Icon(Icons.Filled.LocalBar, null, Modifier.size(18.dp)) }
                                )
                            }
                        }
                        Text(
                            stringResource(R.string.compose_attach_hint),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                    }
                }
            }
        }
    }
}
