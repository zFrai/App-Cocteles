@file:OptIn(ExperimentalMaterial3Api::class)

package com.barcraft.app.ui.profile

import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import com.barcraft.app.BuildConfig
import com.barcraft.app.R
import com.barcraft.app.ui.components.FitText
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
        topBar = { TopAppBar(title = { FitText(stringResource(R.string.profile_title)) }) }
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
                        UserAvatar(me?.name ?: stringResource(R.string.profile_guest), size = 84.dp)
                        Spacer(Modifier.height(12.dp))
                        Text(me?.name ?: stringResource(R.string.profile_guest), style = MaterialTheme.typography.headlineSmall)
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
                                    FitText(stringResource(R.string.profile_my_qr))
                                }
                                Text(
                                    stringResource(R.string.profile_share_handle),
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
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedButton(onClick = { editing = true }, modifier = Modifier.weight(1f)) {
                                    Icon(Icons.Outlined.Edit, null)
                                    Spacer(Modifier.width(8.dp))
                                    FitText(stringResource(R.string.profile_edit))
                                }
                                OutlinedButton(onClick = { editingLinks = true }, modifier = Modifier.weight(1f)) {
                                    Icon(Icons.Outlined.Place, null)
                                    Spacer(Modifier.width(8.dp))
                                    FitText(stringResource(R.string.profile_where_to_find_me))
                                }
                            }
                        } else {
                            Text(
                                if (session.firebaseEnabled) stringResource(R.string.profile_guest_mode)
                                else stringResource(R.string.profile_local_mode),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center
                            )
                            if (session.firebaseEnabled) {
                                Spacer(Modifier.height(12.dp))
                                Button(onClick = onLogin) {
                                    Icon(Icons.AutoMirrored.Outlined.Login, null)
                                    Spacer(Modifier.width(8.dp))
                                    FitText(stringResource(R.string.profile_login))
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
                            headlineContent = { Text(stringResource(R.string.profile_my_posts)) },
                            supportingContent = { Text(stringResource(R.string.profile_my_posts_desc)) },
                            leadingContent = { Icon(Icons.Outlined.Forum, null) },
                            colors = itemColors,
                            modifier = Modifier.clickable(onClick = onOpenMyPosts)
                        )
                        ListItem(
                            headlineContent = { Text(stringResource(R.string.profile_saved)) },
                            supportingContent = {
                                Text(
                                    if (updated > 0) pluralStringResource(R.plurals.profile_saved_updated, updated, updated)
                                    else stringResource(R.string.profile_saved_desc)
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
                    SectionTitle(stringResource(R.string.profile_section_settings))
                    SectionCard(contentPadding = PaddingValues(vertical = 4.dp)) {
                        Column {
                            ListItem(
                                headlineContent = { Text(stringResource(R.string.profile_private_account)) },
                                supportingContent = {
                                    Text(
                                        if (me.privateAccount) stringResource(R.string.profile_private_on)
                                        else stringResource(R.string.profile_private_off)
                                    )
                                },
                                leadingContent = { Icon(if (me.privateAccount) Icons.Outlined.Lock else Icons.Outlined.Public, null) },
                                trailingContent = {
                                    Switch(checked = me.privateAccount, onCheckedChange = { vm.setPrivateAccount(me.uid, it) })
                                },
                                colors = itemColors
                            )
                            NotificationSwitch(stringResource(R.string.profile_notify_comments), prefs.comments, itemColors) {
                                vm.setNotificationPref(me.uid, NotificationPrefs.COMMENTS, it)
                            }
                            NotificationSwitch(stringResource(R.string.profile_notify_messages), prefs.messages, itemColors) {
                                vm.setNotificationPref(me.uid, NotificationPrefs.MESSAGES, it)
                            }
                            NotificationSwitch(stringResource(R.string.profile_notify_followers), prefs.followers, itemColors) {
                                vm.setNotificationPref(me.uid, NotificationPrefs.FOLLOWERS, it)
                            }
                            Text(
                                stringResource(R.string.profile_notify_note),
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
                        headlineContent = { Text(stringResource(R.string.profile_admin_panel)) },
                        supportingContent = { Text(stringResource(R.string.profile_admin_panel_desc)) },
                        leadingContent = { Icon(Icons.Outlined.AdminPanelSettings, null) },
                        colors = ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                        modifier = Modifier.clickable(onClick = onOpenAdmin)
                    )
                }
            }

            // ── Tema ──────────────────────────────────────────────────
            item {
                ThemePicker(current = theme, onApply = vm::setTheme)
            }

            // ── Seguridad y almacenamiento ────────────────────────────
            if (me != null) item {
                var bio by remember { mutableStateOf(vm.isBiometricEnabled(me.uid)) }
                var downloads by remember { mutableStateOf(vm.downloadsBytes()) }
                val hasBiometric = remember { context.biometricAvailable() }
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    SectionTitle(stringResource(R.string.profile_section_security))
                    SectionCard(contentPadding = PaddingValues(vertical = 4.dp)) {
                        Column {
                            ListItem(
                                headlineContent = { Text(stringResource(R.string.profile_fingerprint)) },
                                supportingContent = {
                                    Text(if (hasBiometric) stringResource(R.string.profile_fingerprint_desc, LockStore.PIN_EVERY) else stringResource(R.string.profile_fingerprint_unavailable))
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
                                headlineContent = { Text(stringResource(R.string.profile_change_pin)) },
                                leadingContent = { Icon(Icons.Outlined.Password, null) },
                                colors = ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                                modifier = Modifier.clickable { vm.changePin(me.uid) }
                            )
                            ListItem(
                                headlineContent = { Text(stringResource(R.string.profile_lock_now)) },
                                leadingContent = { Icon(Icons.Outlined.Lock, null) },
                                colors = ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                                modifier = Modifier.clickable { vm.lockNow() }
                            )
                            ListItem(
                                headlineContent = { Text(stringResource(R.string.profile_downloads)) },
                                supportingContent = { Text(stringResource(R.string.profile_downloads_desc, formatBytes(downloads))) },
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
                            headlineContent = { Text(stringResource(R.string.profile_export)) },
                            supportingContent = { Text(stringResource(R.string.profile_export_desc)) },
                            leadingContent = { Icon(Icons.Outlined.FileDownload, null) },
                            colors = ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                            modifier = Modifier.clickable {
                                vm.exportJson { json ->
                                    val send = Intent(Intent.ACTION_SEND).apply {
                                        type = "application/json"
                                        putExtra(Intent.EXTRA_SUBJECT, context.getString(R.string.profile_export_subject))
                                        putExtra(Intent.EXTRA_TEXT, json)
                                    }
                                    context.startActivity(Intent.createChooser(send, context.getString(R.string.profile_export_chooser)))
                                }
                            }
                        )
                        if (!session.firebaseEnabled) ListItem(
                            headlineContent = { Text(stringResource(R.string.profile_firebase_off)) },
                            supportingContent = { Text(stringResource(R.string.profile_firebase_off_desc)) },
                            leadingContent = { Icon(Icons.Outlined.CloudOff, null) },
                            colors = ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
                        )
                        if (me != null) {
                            ListItem(
                                headlineContent = { Text(stringResource(R.string.profile_member_since)) },
                                supportingContent = { Text(DateUtils.date(me.createdAt)) },
                                leadingContent = { Icon(Icons.Outlined.Info, null) },
                                colors = ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
                            )
                            ListItem(
                                headlineContent = { Text(stringResource(R.string.profile_logout), color = MaterialTheme.colorScheme.error) },
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
                    stringResource(R.string.profile_version, BuildConfig.VERSION_NAME),
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
            title = { Text(stringResource(R.string.profile_edit_title)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(name, { name = it }, label = { Text(stringResource(R.string.profile_edit_name)) }, singleLine = true)
                    OutlinedTextField(
                        bio, { bio = it.take(UserProfile.BIO_MAX) },
                        label = { Text(stringResource(R.string.profile_edit_bio)) },
                        supportingText = { Text(stringResource(R.string.profile_edit_bio_counter, bio.length, UserProfile.BIO_MAX)) },
                        minLines = 3
                    )
                }
            },
            confirmButton = {
                Button(onClick = { vm.updateProfile(me.uid, name, bio); editing = false }) { FitText(stringResource(R.string.action_save)) }
            },
            dismissButton = { TextButton(onClick = { editing = false }) { FitText(stringResource(R.string.action_cancel)) } }
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

private fun formatBytes(bytes: Long): String = when {
    bytes < 1024 -> "$bytes B"
    bytes < 1024 * 1024 -> "%.0f KB".format(bytes / 1024f)
    else -> "%.1f MB".format(bytes / (1024f * 1024f))
}
