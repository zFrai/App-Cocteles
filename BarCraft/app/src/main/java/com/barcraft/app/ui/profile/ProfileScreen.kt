@file:OptIn(ExperimentalMaterial3Api::class)

package com.barcraft.app.ui.profile

import android.content.Intent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Logout
import androidx.compose.material.icons.outlined.AdminPanelSettings
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Forum
import androidx.compose.material.icons.outlined.CleaningServices
import androidx.compose.material.icons.outlined.Fingerprint
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Password
import androidx.compose.material.icons.outlined.CloudOff
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.FileDownload
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.NotificationsOff
import androidx.compose.material.icons.outlined.Place
import androidx.compose.material.icons.outlined.Public
import androidx.compose.material.icons.outlined.QrCode2
import androidx.compose.material.icons.automirrored.outlined.Login
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Badge
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.barcraft.app.data.local.LockStore
import com.barcraft.app.model.NotificationPrefs
import com.barcraft.app.model.UserProfile
import com.barcraft.app.ui.components.RoleBadge
import com.barcraft.app.ui.lock.biometricAvailable
import com.barcraft.app.ui.components.SectionCard
import com.barcraft.app.ui.components.SectionTitle
import com.barcraft.app.ui.components.UserAvatar
import com.barcraft.app.ui.navigation.LocalAppSession
import com.barcraft.app.ui.qr.MyQrDialog
import com.barcraft.app.ui.theme.AppTheme
import com.barcraft.app.utils.DateUtils
import com.barcraft.app.utils.appViewModel
import kotlinx.coroutines.flow.flowOf

@Composable
fun ProfileScreen(
    onLogin: () -> Unit,
    onLogout: () -> Unit,
    onOpenAdmin: () -> Unit,
    onOpenMyPosts: () -> Unit,
    onOpenSaved: () -> Unit
) {
    val session = LocalAppSession.current
    val me = session.profile
    val vm = appViewModel { ProfileViewModel(it) }
    val theme by vm.theme.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val snackbar = remember { SnackbarHostState() }
    var editing by remember { mutableStateOf(false) }
    var editingLinks by remember { mutableStateOf(false) }
    var showingQr by remember { mutableStateOf(false) }
    // Fuera de la lista: así el listener de Firestore no se crea de nuevo cada vez que el bloque entra en pantalla.
    val prefs by remember(me?.uid) { me?.let { vm.notificationPrefs(it.uid) } ?: flowOf(NotificationPrefs()) }
        .collectAsStateWithLifecycle(NotificationPrefs())

    LaunchedEffect(Unit) { vm.messages.collect { snackbar.showSnackbar(it) } }

    Scaffold(
        contentWindowInsets = WindowInsets(0),
        snackbarHost = { SnackbarHost(snackbar) },
        topBar = { TopAppBar(title = { Text("Perfil") }) }
    ) { padding ->
        LazyColumn(
            Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // ── Cabecera ──────────────────────────────────────────────
            item {
                SectionCard {
                    Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                        UserAvatar(me?.name ?: "Invitado", size = 84.dp)
                        Spacer(Modifier.height(12.dp))
                        Text(me?.name ?: "Invitado", style = MaterialTheme.typography.headlineSmall)
                        if (me != null) {
                            if (me.handle.isNotBlank()) {
                                // Su identificador único: lo pueden usar otros para encontrarle en la pestaña Buscar.
                                val clipboard = LocalClipboardManager.current
                                Text(
                                    me.handle,
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier
                                        .clickable { clipboard.setText(AnnotatedString(me.handle)) }
                                        .padding(horizontal = 8.dp, vertical = 2.dp)
                                )
                                TextButton(onClick = { showingQr = true }) {
                                    Icon(Icons.Outlined.QrCode2, null)
                                    Spacer(Modifier.width(6.dp))
                                    Text("Mi QR")
                                }
                                Text(
                                    "Comparte tu @nombre #1234 o tu QR para que te encuentren (toca el nombre para copiarlo)",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(Modifier.height(4.dp))
                            }
                            Text(me.email, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Spacer(Modifier.height(8.dp))
                            RoleBadge(me.role, showUser = true)
                            if (me.bio.isNotBlank()) {
                                Spacer(Modifier.height(8.dp))
                                Text(me.bio, textAlign = TextAlign.Center, style = MaterialTheme.typography.bodyMedium)
                            }
                            if (!me.links.isEmpty) {
                                Spacer(Modifier.height(12.dp))
                                WhereToFindMe(me.links)
                            }
                            Spacer(Modifier.height(12.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedButton(onClick = { editing = true }) {
                                    Icon(Icons.Outlined.Edit, null)
                                    Spacer(Modifier.width(8.dp))
                                    Text("Editar perfil")
                                }
                                OutlinedButton(onClick = { editingLinks = true }) {
                                    Icon(Icons.Outlined.Place, null)
                                    Spacer(Modifier.width(8.dp))
                                    Text("Dónde encontrarme")
                                }
                            }
                        } else {
                            Text(
                                if (session.firebaseEnabled) "Modo sin cuenta: solo recetas en este móvil."
                                else "Modo local: Firebase aún no está configurado.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center
                            )
                            if (session.firebaseEnabled) {
                                Spacer(Modifier.height(12.dp))
                                Button(onClick = onLogin) {
                                    Icon(Icons.AutoMirrored.Outlined.Login, null)
                                    Spacer(Modifier.width(8.dp))
                                    Text("Iniciar sesión o registrarme")
                                }
                            }
                        }
                    }
                }
            }

            // ── Mis publicaciones y Guardados ─────────────────────────
            if (me != null) item {
                val savedVm = appViewModel(key = "saved_${me.uid}") { SavedPostsViewModel(it, me) }
                val updated by savedVm.updatedCount.collectAsStateWithLifecycle()
                val itemColors = ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
                SectionCard(contentPadding = PaddingValues(vertical = 4.dp)) {
                    Column {
                        ListItem(
                            headlineContent = { Text("Mis publicaciones") },
                            supportingContent = { Text("Ver, editar o borrar lo que has publicado") },
                            leadingContent = { Icon(Icons.Outlined.Forum, null) },
                            colors = itemColors,
                            modifier = Modifier.clickable(onClick = onOpenMyPosts)
                        )
                        ListItem(
                            headlineContent = { Text("Guardados") },
                            supportingContent = {
                                Text(
                                    if (updated > 0) "$updated ${if (updated == 1) "publicación ha cambiado" else "publicaciones han cambiado"}"
                                    else "Publicaciones de otros que has guardado"
                                )
                            },
                            leadingContent = { Icon(Icons.Outlined.BookmarkBorder, null) },
                            trailingContent = { if (updated > 0) Badge { Text("$updated") } },
                            colors = itemColors,
                            modifier = Modifier.clickable(onClick = onOpenSaved)
                        )
                    }
                }
            }

            // ── Configuración: privacidad y avisos ────────────────────
            if (me != null) item {
                val itemColors = ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    SectionTitle("Configuración")
                    SectionCard(contentPadding = PaddingValues(vertical = 4.dp)) {
                        Column {
                            ListItem(
                                headlineContent = { Text("Cuenta privada") },
                                supportingContent = {
                                    Text(
                                        if (me.privateAccount) "Para seguirte tendrán que enviarte una solicitud"
                                        else "Cualquiera puede seguirte y ver tus publicaciones"
                                    )
                                },
                                leadingContent = { Icon(if (me.privateAccount) Icons.Outlined.Lock else Icons.Outlined.Public, null) },
                                trailingContent = {
                                    Switch(checked = me.privateAccount, onCheckedChange = { vm.setPrivateAccount(me.uid, it) })
                                },
                                colors = itemColors
                            )
                            NotificationSwitch("Avisos de comentarios", prefs.comments, itemColors) {
                                vm.setNotificationPref(me.uid, NotificationPrefs.COMMENTS, it)
                            }
                            NotificationSwitch("Avisos de mensajes", prefs.messages, itemColors) {
                                vm.setNotificationPref(me.uid, NotificationPrefs.MESSAGES, it)
                            }
                            NotificationSwitch("Avisos de nuevos seguidores", prefs.followers, itemColors) {
                                vm.setNotificationPref(me.uid, NotificationPrefs.FOLLOWERS, it)
                            }
                            Text(
                                "Los avisos se guardan ya; las notificaciones en el móvil llegarán en una próxima versión.",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                            )
                        }
                    }
                }
            }

            // ── Administración (solo Admin; antes era una pestaña) ────
            if (me?.isAdmin == true) item {
                SectionCard(contentPadding = PaddingValues(vertical = 4.dp)) {
                    ListItem(
                        headlineContent = { Text("Panel de Admin") },
                        supportingContent = { Text("Usuarios, denuncias y moderación") },
                        leadingContent = { Icon(Icons.Outlined.AdminPanelSettings, null) },
                        colors = ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                        modifier = Modifier.clickable(onClick = onOpenAdmin)
                    )
                }
            }

            // ── Tema ──────────────────────────────────────────────────
            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    SectionTitle("Tema del perfil")
                    AppTheme.entries.chunked(2).forEach { row ->
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            row.forEach { t ->
                                ThemeTile(t, selected = t == theme, Modifier.weight(1f)) { vm.setTheme(t) }
                            }
                        }
                    }
                }
            }

            // ── Seguridad y almacenamiento ────────────────────────────
            if (me != null) item {
                var bio by remember { mutableStateOf(vm.isBiometricEnabled(me.uid)) }
                var downloads by remember { mutableStateOf(vm.downloadsBytes()) }
                val hasBiometric = remember { context.biometricAvailable() }
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    SectionTitle("Seguridad y almacenamiento")
                    SectionCard(contentPadding = PaddingValues(vertical = 4.dp)) {
                        Column {
                            ListItem(
                                headlineContent = { Text("Entrar con huella") },
                                supportingContent = {
                                    Text(if (hasBiometric) "Cada ${LockStore.PIN_EVERY} entradas se pide el PIN" else "Este móvil no tiene huella configurada")
                                },
                                leadingContent = { Icon(Icons.Outlined.Fingerprint, null) },
                                trailingContent = {
                                    Switch(
                                        checked = bio && hasBiometric, enabled = hasBiometric,
                                        onCheckedChange = { vm.setBiometric(me.uid, it); bio = it }
                                    )
                                },
                                colors = ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
                            )
                            ListItem(
                                headlineContent = { Text("Cambiar PIN") },
                                leadingContent = { Icon(Icons.Outlined.Password, null) },
                                colors = ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                                modifier = Modifier.clickable { vm.changePin(me.uid) }
                            )
                            ListItem(
                                headlineContent = { Text("Bloquear ahora") },
                                leadingContent = { Icon(Icons.Outlined.Lock, null) },
                                colors = ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                                modifier = Modifier.clickable { vm.lockNow() }
                            )
                            ListItem(
                                headlineContent = { Text("Fotos y audios descargados") },
                                supportingContent = { Text("Ocupan ${formatBytes(downloads)}. Pulsa para liberar espacio.") },
                                leadingContent = { Icon(Icons.Outlined.CleaningServices, null) },
                                colors = ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                                modifier = Modifier.clickable { vm.clearDownloads(); downloads = vm.downloadsBytes() }
                            )
                        }
                    }
                }
            }

            // ── Datos y cuenta ────────────────────────────────────────
            item {
                SectionCard(contentPadding = PaddingValues(vertical = 4.dp)) {
                    Column {
                        ListItem(
                            headlineContent = { Text("Exportar recetas (JSON)") },
                            supportingContent = { Text("Copia de seguridad para compartir o guardar") },
                            leadingContent = { Icon(Icons.Outlined.FileDownload, null) },
                            colors = ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                            modifier = Modifier.clickable {
                                vm.exportJson { json ->
                                    val send = Intent(Intent.ACTION_SEND).apply {
                                        type = "application/json"
                                        putExtra(Intent.EXTRA_SUBJECT, "Recetas BarCraft")
                                        putExtra(Intent.EXTRA_TEXT, json)
                                    }
                                    context.startActivity(Intent.createChooser(send, "Exportar recetas"))
                                }
                            }
                        )
                        if (!session.firebaseEnabled) ListItem(
                            headlineContent = { Text("Comunidad y chat desactivados") },
                            supportingContent = { Text("Añade app/google-services.json siguiendo FIREBASE_SETUP.md") },
                            leadingContent = { Icon(Icons.Outlined.CloudOff, null) },
                            colors = ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
                        )
                        if (me != null) {
                            ListItem(
                                headlineContent = { Text("Miembro desde") },
                                supportingContent = { Text(DateUtils.date(me.createdAt)) },
                                leadingContent = { Icon(Icons.Outlined.Info, null) },
                                colors = ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
                            )
                            ListItem(
                                headlineContent = { Text("Cerrar sesión", color = MaterialTheme.colorScheme.error) },
                                leadingContent = { Icon(Icons.AutoMirrored.Outlined.Logout, null, tint = MaterialTheme.colorScheme.error) },
                                colors = ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                                modifier = Modifier.clickable(onClick = onLogout)
                            )
                        }
                    }
                }
            }
            item {
                Text(
                    "BarCraft 3.0",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }

    if (editing && me != null) {
        var name by rememberSaveable { mutableStateOf(me.name) }
        var bio by rememberSaveable { mutableStateOf(me.bio) }
        AlertDialog(
            onDismissRequest = { editing = false },
            title = { Text("Editar perfil") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(name, { name = it }, label = { Text("Nombre visible") }, singleLine = true)
                    OutlinedTextField(
                        bio, { bio = it.take(UserProfile.BIO_MAX) },
                        label = { Text("Descripción (opcional)") },
                        supportingText = { Text("${bio.length}/${UserProfile.BIO_MAX}") },
                        minLines = 3
                    )
                }
            },
            confirmButton = {
                Button(onClick = { vm.updateProfile(me.uid, name, bio); editing = false }) { Text("Guardar") }
            },
            dismissButton = { TextButton(onClick = { editing = false }) { Text("Cancelar") } }
        )
    }

    if (showingQr && me != null) {
        MyQrDialog(me, onDismiss = { showingQr = false })
    }

    if (editingLinks && me != null) {
        EditLinksDialog(
            initial = me.links,
            onDismiss = { editingLinks = false },
            onSave = { vm.updateLinks(me.uid, it); editingLinks = false }
        )
    }
}

@Composable
private fun NotificationSwitch(
    title: String,
    checked: Boolean,
    colors: androidx.compose.material3.ListItemColors,
    onChange: (Boolean) -> Unit
) {
    ListItem(
        headlineContent = { Text(title) },
        leadingContent = { Icon(if (checked) Icons.Outlined.Notifications else Icons.Outlined.NotificationsOff, null) },
        trailingContent = { Switch(checked = checked, onCheckedChange = onChange) },
        colors = colors
    )
}

@Composable
private fun ThemeTile(theme: AppTheme, selected: Boolean, modifier: Modifier = Modifier, onClick: () -> Unit) {
    val scheme = theme.colorScheme
    Surface(
        onClick = onClick,
        shape = MaterialTheme.shapes.medium,
        color = scheme.surfaceContainer,
        contentColor = scheme.onSurface,
        border = if (selected) BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else null,
        modifier = modifier
    ) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(theme.emoji, style = MaterialTheme.typography.titleLarge)
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text(theme.label, style = MaterialTheme.typography.titleSmall, color = scheme.primary)
                Text(theme.description, style = MaterialTheme.typography.labelSmall, color = scheme.onSurfaceVariant)
            }
            if (selected) Icon(Icons.Outlined.CheckCircle, null, tint = scheme.primary)
        }
    }
}

private fun formatBytes(bytes: Long): String = when {
    bytes < 1024 -> "$bytes B"
    bytes < 1024 * 1024 -> "%.0f KB".format(bytes / 1024f)
    else -> "%.1f MB".format(bytes / (1024f * 1024f))
}
