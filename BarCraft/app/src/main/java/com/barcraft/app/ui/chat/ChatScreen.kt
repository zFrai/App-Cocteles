@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)

package com.barcraft.app.ui.chat

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.outlined.AddPhotoAlternate
import androidx.compose.material.icons.outlined.Block
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.Flag
import androidx.compose.material.icons.outlined.Forum
import androidx.compose.material.icons.outlined.Gif
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
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
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.barcraft.app.model.ChatMessage
import com.barcraft.app.model.MessageType
import com.barcraft.app.model.REACTION_EMOJIS
import com.barcraft.app.model.ReportReason
import com.barcraft.app.ui.components.AudioPlayer
import com.barcraft.app.ui.components.EmptyState
import com.barcraft.app.ui.components.LoadingBox
import com.barcraft.app.ui.components.RemoteImage
import com.barcraft.app.ui.components.UserAvatar
import com.barcraft.app.ui.components.formatDuration
import com.barcraft.app.ui.components.rememberPhotoPicker
import com.barcraft.app.ui.navigation.LocalAppSession
import com.barcraft.app.utils.DateUtils
import com.barcraft.app.utils.appViewModel
import kotlinx.coroutines.delay

/** Conversación en tiempo real: texto, fotos, notas de voz, GIFs, stickers y reacciones. */
@Composable
fun ChatScreen(roomId: String, onBack: () -> Unit) {
    val me = LocalAppSession.current.profile ?: return
    val context = LocalContext.current
    val vm = appViewModel(key = "room_$roomId") { ChatRoomViewModel(it, roomId, me) }
    val room by vm.room.collectAsStateWithLifecycle()
    val messages by vm.messages.collectAsStateWithLifecycle()
    val uploading by vm.uploading.collectAsStateWithLifecycle()
    val recording by vm.recording.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }

    var text by rememberSaveable { mutableStateOf("") }
    var selected by remember { mutableStateOf<ChatMessage?>(null) }
    var viewer by remember { mutableStateOf<String?>(null) }
    var giphyOpen by remember { mutableStateOf(false) }
    var menuOpen by remember { mutableStateOf(false) }
    /** Denuncia en curso: null = cerrada. El mensaje dentro es opcional (denuncia de la persona en general). */
    var reporting by remember { mutableStateOf<ReportDraft?>(null) }

    val photoPicker = rememberPhotoPicker { vm.sendImage(it) }
    val micPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) vm.startRecording(context)
    }

    LaunchedEffect(Unit) { vm.errors.collect { snackbar.showSnackbar(it) } }

    val r = room
    val otherName = r?.otherName(me.uid).orEmpty()
    val blocked = r?.isBlocked == true

    Scaffold(
        contentWindowInsets = WindowInsets(0),
        snackbarHost = { SnackbarHost(snackbar) },
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        UserAvatar(otherName.ifBlank { "?" }, size = 36.dp)
                        Spacer(Modifier.width(12.dp))
                        Text(otherName)
                    }
                },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Volver") } },
                actions = {
                    if (r != null) Box {
                        IconButton(onClick = { menuOpen = true }) { Icon(Icons.Outlined.MoreVert, "Más opciones") }
                        DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                            DropdownMenuItem(
                                text = { Text("Denunciar a $otherName") },
                                leadingIcon = { Icon(Icons.Outlined.Flag, null, tint = MaterialTheme.colorScheme.error) },
                                onClick = { menuOpen = false; reporting = ReportDraft(message = null) }
                            )
                        }
                    }
                }
            )
        },
        bottomBar = {
            Surface(tonalElevation = 3.dp, modifier = Modifier.imePadding()) {
                Column {
                    if (uploading) LinearProgressIndicator(Modifier.fillMaxWidth())
                    when {
                        blocked -> Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Outlined.Block, null, tint = MaterialTheme.colorScheme.error)
                            Spacer(Modifier.width(12.dp))
                            Text(
                                if (r?.blockedBy?.contains(me.uid) == true) "Has bloqueado a $otherName. Desbloquéalo en Chats > Bloqueados."
                                else "No puedes enviar mensajes en esta conversación.",
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                        recording -> RecordingBar(
                            elapsed = { vm.recordingElapsed() },
                            onCancel = vm::cancelRecording,
                            onSend = vm::stopAndSend
                        )
                        else -> Row(Modifier.fillMaxWidth().padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
                            IconButton(onClick = { photoPicker.choose() }) { Icon(Icons.Outlined.AddPhotoAlternate, "Foto") }
                            IconButton(onClick = { giphyOpen = true }) { Icon(Icons.Outlined.Gif, "GIFs y stickers") }
                            OutlinedTextField(
                                value = text, onValueChange = { text = it },
                                placeholder = { Text("Mensaje") },
                                maxLines = 5,
                                shape = MaterialTheme.shapes.extraLarge,
                                modifier = Modifier.weight(1f)
                            )
                            Spacer(Modifier.width(6.dp))
                            if (text.isBlank()) {
                                FilledIconButton(onClick = {
                                    val granted = ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) ==
                                        PackageManager.PERMISSION_GRANTED
                                    if (granted) vm.startRecording(context) else micPermission.launch(Manifest.permission.RECORD_AUDIO)
                                }) { Icon(Icons.Filled.Mic, "Grabar nota de voz") }
                            } else {
                                FilledIconButton(onClick = { vm.sendText(text); text = "" }) {
                                    Icon(Icons.AutoMirrored.Filled.Send, "Enviar")
                                }
                            }
                        }
                    }
                }
            }
        }
    ) { padding ->
        val list = messages
        when {
            list == null -> LoadingBox(Modifier.padding(padding))
            list.isEmpty() -> EmptyState(
                Icons.Outlined.Forum, "Empieza la conversación",
                "Los mensajes llegan al instante a $otherName.",
                modifier = Modifier.padding(padding)
            )
            else -> LazyColumn(
                Modifier.fillMaxSize().padding(padding),
                reverseLayout = true,
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                items(list.asReversed(), key = { it.id }) { msg ->
                    MessageItem(
                        message = msg,
                        mine = msg.senderId == me.uid,
                        myUid = me.uid,
                        onLongPress = { selected = msg },
                        onReactionTap = { emoji -> vm.react(msg, emoji) },
                        onOpenImage = { viewer = it }
                    )
                }
            }
        }
    }

    // Reaccionar / borrar
    selected?.let { msg ->
        AlertDialog(
            onDismissRequest = { selected = null },
            title = { Text("Reaccionar") },
            text = {
                Column {
                    Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                        REACTION_EMOJIS.forEach { emoji ->
                            val mineSelected = msg.reactions[me.uid] == emoji
                            Box(
                                Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(if (mineSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerHigh)
                                    .clickable { vm.react(msg, emoji); selected = null },
                                contentAlignment = Alignment.Center
                            ) { Text(emoji, fontSize = 18.sp) }
                        }
                    }
                }
            },
            confirmButton = { TextButton(onClick = { selected = null }) { Text("Cerrar") } },
            dismissButton = {
                if (msg.senderId == me.uid) TextButton(onClick = { vm.delete(msg); selected = null }) {
                    Icon(Icons.Outlined.Delete, null, tint = MaterialTheme.colorScheme.error)
                    Spacer(Modifier.width(6.dp))
                    Text("Borrar para todos", color = MaterialTheme.colorScheme.error)
                } else TextButton(onClick = { reporting = ReportDraft(message = msg); selected = null }) {
                    Icon(Icons.Outlined.Flag, null, tint = MaterialTheme.colorScheme.error)
                    Spacer(Modifier.width(6.dp))
                    Text("Denunciar mensaje", color = MaterialTheme.colorScheme.error)
                }
            }
        )
    }

    reporting?.let { draft ->
        ReportDialog(
            otherName = otherName,
            message = draft.message,
            onDismiss = { reporting = null },
            onSend = { reason, details ->
                vm.report(reason, details, draft.message)
                reporting = null
            }
        )
    }

    viewer?.let { mediaId ->
        ImageViewer(mediaId, onSave = { vm.saveToGallery(mediaId) }, onClose = { viewer = null })
    }

    if (giphyOpen) {
        GiphySheet(
            onDismiss = { giphyOpen = false },
            onPick = { item, kind -> vm.sendGiphy(item, kind); giphyOpen = false }
        )
    }
}

/** Denuncia que se está preparando. [message] = null si se denuncia a la persona en general. */
private data class ReportDraft(val message: ChatMessage?)

/** Elegir motivo y, si se quiere, añadir detalles. La denuncia le llega al Admin. */
@Composable
private fun ReportDialog(
    otherName: String,
    message: ChatMessage?,
    onDismiss: () -> Unit,
    onSend: (ReportReason, String) -> Unit
) {
    var reason by rememberSaveable { mutableStateOf<ReportReason?>(null) }
    var details by rememberSaveable { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Outlined.Flag, null, tint = MaterialTheme.colorScheme.error) },
        title = { Text(if (message != null) "Denunciar mensaje" else "Denunciar a $otherName") },
        text = {
            Column {
                if (message != null) {
                    Surface(
                        shape = MaterialTheme.shapes.small,
                        color = MaterialTheme.colorScheme.surfaceContainerHigh,
                        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
                    ) {
                        Text(
                            "\u201C${message.preview}\u201D",
                            maxLines = 3,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(8.dp)
                        )
                    }
                }
                Text("¿Qué ocurre?", style = MaterialTheme.typography.titleSmall)
                ReportReason.entries.forEach { option ->
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .selectable(selected = reason == option, onClick = { reason = option })
                            .padding(vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(selected = reason == option, onClick = { reason = option })
                        Text(option.label, style = MaterialTheme.typography.bodyMedium)
                    }
                }
                OutlinedTextField(
                    value = details,
                    onValueChange = { if (it.length <= 500) details = it },
                    label = { Text("Detalles (opcional)") },
                    maxLines = 4,
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
                )
                Text(
                    "$otherName no sabrá quién le ha denunciado. Si además no quieres hablar con esta persona, bloquéala desde Chats.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
        },
        confirmButton = {
            TextButton(enabled = reason != null, onClick = { reason?.let { onSend(it, details) } }) {
                Text("Enviar denuncia", color = if (reason != null) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant)
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } }
    )
}

@Composable
private fun RecordingBar(elapsed: () -> Long, onCancel: () -> Unit, onSend: () -> Unit) {
    var ms by remember { mutableLongStateOf(0L) }
    LaunchedEffect(Unit) {
        while (true) { ms = elapsed(); delay(250) }
    }
    Row(Modifier.fillMaxWidth().padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
        IconButton(onClick = onCancel) { Icon(Icons.Filled.Close, "Cancelar") }
        Box(Modifier.size(12.dp).clip(CircleShape).background(MaterialTheme.colorScheme.error))
        Spacer(Modifier.width(10.dp))
        Text("Grabando  ${formatDuration(ms)}", style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f))
        FilledIconButton(onClick = onSend) { Icon(Icons.AutoMirrored.Filled.Send, "Enviar nota de voz") }
    }
}

@Composable
private fun MessageItem(
    message: ChatMessage,
    mine: Boolean,
    myUid: String,
    onLongPress: () -> Unit,
    onReactionTap: (String) -> Unit,
    onOpenImage: (String) -> Unit
) {
    val shape = if (mine) RoundedCornerShape(18.dp, 18.dp, 4.dp, 18.dp) else RoundedCornerShape(18.dp, 18.dp, 18.dp, 4.dp)
    val bubbleColor = if (mine) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerHigh
    val onBubble = if (mine) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface

    Column(Modifier.fillMaxWidth(), horizontalAlignment = if (mine) Alignment.End else Alignment.Start) {
        val clickable = Modifier.combinedClickable(
            onClick = { if (message.type == MessageType.IMAGE) onOpenImage(message.mediaId) },
            onLongClick = onLongPress
        )
        when (message.type) {
            MessageType.STICKER -> AsyncImage(
                model = message.gifUrl, contentDescription = message.text,
                modifier = Modifier.size(140.dp).then(clickable)
            )
            else -> Surface(
                shape = shape, color = bubbleColor, contentColor = onBubble,
                modifier = Modifier.widthIn(max = 300.dp).clip(shape).then(clickable)
            ) {
                Column(Modifier.padding(if (message.type == MessageType.TEXT || message.type == MessageType.AUDIO) 10.dp else 4.dp)) {
                    when (message.type) {
                        MessageType.IMAGE -> RemoteImage(
                            message.mediaId,
                            Modifier.size(width = 240.dp, height = 240.dp).clip(RoundedCornerShape(14.dp))
                        )
                        MessageType.GIF -> AsyncImage(
                            model = message.gifUrl, contentDescription = message.text,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.size(width = 220.dp, height = 180.dp).clip(RoundedCornerShape(14.dp))
                        )
                        MessageType.AUDIO -> AudioPlayer(message.mediaId, message.durationMs)
                        else -> Text(message.text, style = MaterialTheme.typography.bodyLarge)
                    }
                    Text(
                        DateUtils.time(message.createdAt),
                        style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier.align(Alignment.End).padding(horizontal = 4.dp)
                    )
                }
            }
        }
        if (message.reactions.isNotEmpty()) {
            Row(Modifier.padding(top = 2.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                message.reactions.values.groupingBy { it }.eachCount().forEach { (emoji, count) ->
                    val minePick = message.reactions[myUid] == emoji
                    Surface(
                        shape = CircleShape,
                        color = if (minePick) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surfaceContainerHighest,
                        modifier = Modifier.clickable { onReactionTap(emoji) }
                    ) {
                        Text(
                            if (count > 1) "$emoji $count" else emoji,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                            fontSize = 13.sp
                        )
                    }
                }
            }
        }
    }
}

/** Foto a pantalla completa con opción de guardarla en la galería. */
@Composable
fun ImageViewer(mediaId: String, onSave: () -> Unit, onClose: () -> Unit) {
    Dialog(onDismissRequest = onClose, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(color = MaterialTheme.colorScheme.scrim, modifier = Modifier.fillMaxSize()) {
            Box {
                RemoteImage(mediaId, Modifier.fillMaxSize(), contentScale = ContentScale.Fit)
                Row(Modifier.align(Alignment.TopEnd).padding(16.dp)) {
                    FilledIconButton(onClick = onSave) { Icon(Icons.Outlined.Download, "Guardar en la galería") }
                    Spacer(Modifier.width(8.dp))
                    FilledIconButton(onClick = onClose) { Icon(Icons.Filled.Close, "Cerrar") }
                }
            }
        }
    }
}

