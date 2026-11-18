package com.barcraft.app.ui.components

import android.media.MediaPlayer
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.outlined.CloudOff
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.barcraft.app.R
import com.barcraft.app.utils.appContainer
import kotlinx.coroutines.delay
import java.io.File

/** Estado de descarga de un archivo compartido. */
private sealed interface MediaLoad {
    data object Loading : MediaLoad
    data object Unavailable : MediaLoad
    data class Ready(val file: File) : MediaLoad
}

@Composable
private fun rememberMediaFile(mediaId: String): MediaLoad {
    val repo = appContainer().mediaRepository
    val state by produceState<MediaLoad>(MediaLoad.Loading, mediaId) {
        value = repo.localFile(mediaId)?.let { MediaLoad.Ready(it) } ?: MediaLoad.Unavailable
    }
    return state
}

/** Archivo de una foto del servidor: null mientras se descarga o si no hay conexión. */
@Composable
internal fun rememberMediaFileOrNull(mediaId: String): File? =
    (rememberMediaFile(mediaId) as? MediaLoad.Ready)?.file

/** Foto guardada en el servidor: se descarga al móvil la primera vez y después carga sin conexión. */
@Composable
fun RemoteImage(mediaId: String, modifier: Modifier = Modifier, contentScale: ContentScale = ContentScale.Crop) {
    Box(modifier.background(MaterialTheme.colorScheme.surfaceContainerHighest), contentAlignment = Alignment.Center) {
        when (val s = rememberMediaFile(mediaId)) {
            MediaLoad.Loading -> CircularProgressIndicator(Modifier.size(28.dp), strokeWidth = 2.dp)
            MediaLoad.Unavailable -> Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(12.dp)) {
                Icon(Icons.Outlined.CloudOff, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(stringResource(R.string.media_connect_to_download), style = MaterialTheme.typography.labelSmall)
            }
            is MediaLoad.Ready -> AsyncImage(
                model = s.file,
                contentDescription = null,
                contentScale = contentScale,
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}

/** Reproductor de una nota de voz. */
@Composable
fun AudioPlayer(mediaId: String, durationMs: Long, modifier: Modifier = Modifier) {
    val load = rememberMediaFile(mediaId)
    var player by remember { mutableStateOf<MediaPlayer?>(null) }
    var playing by remember { mutableStateOf(false) }
    var progress by remember { mutableFloatStateOf(0f) }

    DisposableEffect(mediaId) {
        onDispose { player?.release(); player = null }
    }
    LaunchedEffect(playing) {
        while (playing) {
            val p = player ?: break
            progress = if (p.duration > 0) p.currentPosition.toFloat() / p.duration else 0f
            delay(200)
        }
    }

    Row(modifier, verticalAlignment = Alignment.CenterVertically) {
        when (load) {
            MediaLoad.Loading -> Box(Modifier.size(40.dp), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
            }
            MediaLoad.Unavailable -> Box(Modifier.size(40.dp), contentAlignment = Alignment.Center) {
                Icon(Icons.Outlined.CloudOff, stringResource(R.string.media_offline))
            }
            is MediaLoad.Ready -> IconButton(onClick = {
                val current = player ?: MediaPlayer().apply {
                    setDataSource(load.file.absolutePath)
                    setOnCompletionListener { playing = false; progress = 0f }
                    prepare()
                }.also { player = it }
                if (playing) { current.pause(); playing = false } else { current.start(); playing = true }
            }) {
                Icon(if (playing) Icons.Filled.Pause else Icons.Filled.PlayArrow, if (playing) stringResource(R.string.media_pause) else stringResource(R.string.media_play))
            }
        }
        Column(Modifier.width(150.dp)) {
            LinearProgressIndicator(progress = { progress }, modifier = Modifier.padding(vertical = 4.dp))
            Text(formatDuration(durationMs), style = MaterialTheme.typography.labelSmall)
        }
        Spacer(Modifier.width(4.dp))
    }
}

fun formatDuration(ms: Long): String {
    val total = ms / 1000
    return "%d:%02d".format(total / 60, total % 60)
}
