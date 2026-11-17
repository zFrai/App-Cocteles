package com.barcraft.app.ui.components

import android.content.Context
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.PhotoCamera
import androidx.compose.material.icons.outlined.PhotoLibrary
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.core.content.FileProvider
import com.barcraft.app.R
import java.io.File

/** Abre la cámara o la galería y devuelve la foto elegida. */
class PhotoPickerState internal constructor(
    private val launchCamera: () -> Unit,
    private val launchGallery: () -> Unit,
    private val showChooser: () -> Unit
) {
    fun camera() = launchCamera()
    fun gallery() = launchGallery()
    /** Pregunta "Hacer foto" o "Elegir de la galería". */
    fun choose() = showChooser()
}

@Composable
fun rememberPhotoPicker(onPicked: (Uri) -> Unit): PhotoPickerState {
    val context = LocalContext.current
    var pendingCameraUri by rememberSaveable { mutableStateOf<String?>(null) }
    var chooser by remember { mutableStateOf(false) }

    val gallery = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) onPicked(uri)
    }
    // La cámara del sistema se abre con un Intent: no hace falta pedir el permiso CAMERA.
    val camera = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { ok ->
        val uri = pendingCameraUri
        if (ok && uri != null) onPicked(Uri.parse(uri))
        pendingCameraUri = null
    }

    val state = remember {
        PhotoPickerState(
            launchCamera = {
                val uri = newCameraUri(context)
                pendingCameraUri = uri.toString()
                runCatching { camera.launch(uri) }
            },
            launchGallery = {
                gallery.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
            },
            showChooser = { chooser = true }
        )
    }

    if (chooser) {
        AlertDialog(
            onDismissRequest = { chooser = false },
            title = { Text(stringResource(R.string.photo_add_title)) },
            text = {
                Column {
                    ListItem(
                        headlineContent = { Text(stringResource(R.string.photo_take)) },
                        leadingContent = { Icon(Icons.Outlined.PhotoCamera, null) },
                        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                        modifier = Modifier.clickable { chooser = false; state.camera() }
                    )
                    ListItem(
                        headlineContent = { Text(stringResource(R.string.photo_from_gallery)) },
                        leadingContent = { Icon(Icons.Outlined.PhotoLibrary, null) },
                        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                        modifier = Modifier.clickable { chooser = false; state.gallery() }
                    )
                }
            },
            confirmButton = {},
            dismissButton = { TextButton(onClick = { chooser = false }) { FitText(stringResource(R.string.action_cancel)) } }
        )
    }
    return state
}

private fun newCameraUri(context: Context): Uri {
    val dir = File(context.cacheDir, "camera").apply { mkdirs() }
    val file = File(dir, "IMG_${System.currentTimeMillis()}.jpg")
    return FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
}
