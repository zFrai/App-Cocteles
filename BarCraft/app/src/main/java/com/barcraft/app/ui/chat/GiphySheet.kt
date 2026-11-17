@file:OptIn(ExperimentalMaterial3Api::class)

package com.barcraft.app.ui.chat

import com.barcraft.app.ui.components.FitText
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.barcraft.app.model.GiphyItem
import com.barcraft.app.repository.GiphyRepository
import com.barcraft.app.utils.appViewModel

/** Selector de GIFs y stickers de GIPHY. */
@Composable
fun GiphySheet(onDismiss: () -> Unit, onPick: (GiphyItem, GiphyRepository.Kind) -> Unit) {
    val vm = appViewModel { GiphyViewModel(it.giphyRepository) }
    val results by vm.results.collectAsStateWithLifecycle()
    val loading by vm.loading.collectAsStateWithLifecycle()
    val error by vm.error.collectAsStateWithLifecycle()
    var kind by rememberSaveable { mutableStateOf(GiphyRepository.Kind.GIFS) }
    var query by rememberSaveable { mutableStateOf("") }

    LaunchedEffect(kind, query) { vm.load(kind, query) }

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)) {
        Column(Modifier.fillMaxWidth().height(560.dp)) {
            PrimaryTabRow(selectedTabIndex = kind.ordinal) {
                Tab(selected = kind == GiphyRepository.Kind.GIFS, onClick = { kind = GiphyRepository.Kind.GIFS }, text = { FitText("GIFs") })
                Tab(selected = kind == GiphyRepository.Kind.STICKERS, onClick = { kind = GiphyRepository.Kind.STICKERS }, text = { FitText("Stickers") })
            }
            OutlinedTextField(
                value = query, onValueChange = { query = it },
                placeholder = { Text(if (kind == GiphyRepository.Kind.GIFS) "Buscar GIFs (cocktail, cheers…)" else "Buscar stickers") },
                leadingIcon = { Icon(Icons.Filled.Search, null) },
                singleLine = true,
                shape = MaterialTheme.shapes.extraLarge,
                modifier = Modifier.fillMaxWidth().padding(12.dp)
            )
            Box(Modifier.weight(1f).fillMaxWidth()) {
                when {
                    loading && results.isEmpty() -> CircularProgressIndicator(Modifier.align(Alignment.Center))
                    error != null -> Text(error.orEmpty(), Modifier.align(Alignment.Center), color = MaterialTheme.colorScheme.error)
                    else -> LazyVerticalGrid(
                        columns = GridCells.Fixed(3),
                        contentPadding = PaddingValues(8.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        items(results) { item ->
                            AsyncImage(
                                model = item.previewUrl,
                                contentDescription = item.title,
                                contentScale = if (kind == GiphyRepository.Kind.STICKERS) ContentScale.Fit else ContentScale.Crop,
                                modifier = Modifier
                                    .aspectRatio(1f)
                                    .clip(MaterialTheme.shapes.small)
                                    .clickable { onPick(item, kind) }
                            )
                        }
                    }
                }
            }
            Text(
                "Powered by GIPHY",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.align(Alignment.CenterHorizontally).padding(8.dp)
            )
        }
    }
}
