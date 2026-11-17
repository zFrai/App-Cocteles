@file:OptIn(ExperimentalMaterial3Api::class)

package com.barcraft.app.ui.admin

import androidx.compose.ui.res.stringResource
import com.barcraft.app.R
import com.barcraft.app.ui.components.FitText
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.Block
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Flag
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
import com.barcraft.app.model.Report
import com.barcraft.app.model.ReportStatus
import com.barcraft.app.model.Role
import com.barcraft.app.model.UserProfile
import com.barcraft.app.ui.components.EmptyState
import com.barcraft.app.ui.components.LoadingBox
import com.barcraft.app.ui.components.RoleBadge
import com.barcraft.app.ui.components.UserAvatar
import com.barcraft.app.ui.navigation.LocalAppSession
import com.barcraft.app.utils.DateUtils
import com.barcraft.app.utils.appViewModel

@Composable
fun AdminScreen(onOpenPost: (String) -> Unit, onBack: () -> Unit) {
    val me = LocalAppSession.current.profile ?: return
    val vm = appViewModel { AdminViewModel(it.userRepository, it.socialRepository, it.reportRepository, me.uid) }
    val openReports by vm.openReports.collectAsStateWithLifecycle()
    val users by vm.allUsers.collectAsStateWithLifecycle()
    val posts by vm.posts.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    var tab by rememberSaveable { mutableIntStateOf(0) }
    var query by rememberSaveable { mutableStateOf("") }
    var postToDelete by remember { mutableStateOf<Post?>(null) }
    var reportToBan by remember { mutableStateOf<Report?>(null) }

    LaunchedEffect(Unit) { vm.messages.collect { snackbar.showSnackbar(it) } }

    Scaffold(
        contentWindowInsets = WindowInsets(0),
        snackbarHost = { SnackbarHost(snackbar) },
        topBar = {
            TopAppBar(
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.action_back)) } },
                title = {
                    Column {
                        FitText(stringResource(R.string.admin_title))
                        FitText(
                            stringResource(R.string.admin_subtitle),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            )
        }
    ) { padding ->
        val list = users
        if (list == null) { LoadingBox(Modifier.padding(padding)); return@Scaffold }

        Column(Modifier.fillMaxSize().padding(padding)) {
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                StatTile(stringResource(R.string.admin_stat_barmans), list.count { it.role == Role.BARMAN }, Modifier.weight(1f))
                StatTile(stringResource(R.string.admin_stat_clients), list.count { it.role == Role.CLIENTE }, Modifier.weight(1f))
                StatTile(stringResource(R.string.admin_stat_admins), list.count { it.isAdmin }, Modifier.weight(1f))
                StatTile(stringResource(R.string.admin_stat_banned), list.count { it.banned }, Modifier.weight(1f))
            }
            PrimaryTabRow(selectedTabIndex = tab) {
                Tab(selected = tab == 0, onClick = { tab = 0 }, text = { FitText(stringResource(R.string.admin_tab_users)) })
                Tab(selected = tab == 1, onClick = { tab = 1 }, text = { FitText(stringResource(R.string.admin_tab_posts)) })
                val pending = openReports?.size ?: 0
                Tab(
                    selected = tab == 2, onClick = { tab = 2 },
                    text = {
                        FitText(
                            if (pending > 0) stringResource(R.string.admin_tab_reports_count, pending) else stringResource(R.string.admin_tab_reports),
                            color = if (pending > 0) MaterialTheme.colorScheme.error else androidx.compose.ui.graphics.Color.Unspecified
                        )
                    }
                )
            }
            if (tab == 2) {
                val pending = openReports
                when {
                    pending == null -> LoadingBox()
                    pending.isEmpty() -> EmptyState(
                        Icons.Outlined.Flag, stringResource(R.string.admin_no_reports_title),
                        stringResource(R.string.admin_no_reports_message)
                    )
                    else -> LazyColumn(contentPadding = PaddingValues(bottom = 24.dp)) {
                        items(pending, key = { it.id }) { report ->
                            ReportRow(
                                report = report,
                                reportedIsBanned = list.firstOrNull { it.uid == report.reportedId }?.banned == true,
                                onResolve = { vm.closeReport(report, ReportStatus.RESOLVED) },
                                onDismiss = { vm.closeReport(report, ReportStatus.DISMISSED) },
                                onBan = { reportToBan = report }
                            )
                        }
                    }
                }
            } else if (tab == 0) {
                OutlinedTextField(
                    value = query, onValueChange = { query = it },
                    placeholder = { Text(stringResource(R.string.admin_search_hint)) },
                    leadingIcon = { Icon(Icons.Filled.Search, null) },
                    singleLine = true,
                    shape = MaterialTheme.shapes.extraLarge,
                    modifier = Modifier.fillMaxWidth().padding(16.dp)
                )
                val filtered = list.filter {
                    it.matches(query) || it.email.contains(query.trim(), true)
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
                                    post.text.ifBlank { post.cocktailName.ifBlank { stringResource(R.string.admin_post_no_text) } },
                                    maxLines = 2, overflow = TextOverflow.Ellipsis
                                )
                            },
                            supportingContent = {
                                Text(stringResource(R.string.admin_post_summary, post.authorName, DateUtils.relative(post.createdAt), post.likes.size, post.commentCount))
                            },
                            trailingContent = {
                                Row {
                                    TextButton(onClick = { onOpenPost(post.id) }) { FitText(stringResource(R.string.admin_view)) }
                                    TextButton(onClick = { postToDelete = post }) {
                                        FitText(stringResource(R.string.action_delete_short), color = MaterialTheme.colorScheme.error)
                                    }
                                }
                            }
                        )
                    }
                }
            }
        }
    }

    reportToBan?.let { r ->
        AlertDialog(
            onDismissRequest = { reportToBan = null },
            title = { Text(stringResource(R.string.admin_ban_title, r.reportedName)) },
            text = { Text(stringResource(R.string.admin_ban_message)) },
            confirmButton = {
                TextButton(onClick = { vm.banReported(r); reportToBan = null }) {
                    FitText(stringResource(R.string.admin_block), color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = { TextButton(onClick = { reportToBan = null }) { FitText(stringResource(R.string.action_cancel)) } }
        )
    }

    postToDelete?.let { p ->
        AlertDialog(
            onDismissRequest = { postToDelete = null },
            title = { Text(stringResource(R.string.admin_delete_post_title)) },
            text = { Text(stringResource(R.string.admin_delete_post_message, p.authorName)) },
            confirmButton = {
                TextButton(onClick = { vm.deletePost(p); postToDelete = null }) {
                    FitText(stringResource(R.string.action_delete_short), color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = { TextButton(onClick = { postToDelete = null }) { FitText(stringResource(R.string.action_cancel)) } }
        )
    }
}

@Composable
private fun ReportRow(
    report: Report,
    reportedIsBanned: Boolean,
    onResolve: () -> Unit,
    onDismiss: () -> Unit,
    onBan: () -> Unit
) {
    Surface(
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp)
    ) {
        Column(Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                UserAvatar(report.reportedName, size = 36.dp)
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Text(report.reportedName, style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(
                        stringResource(report.reason.labelRes),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.error
                    )
                }
                Text(DateUtils.relative(report.createdAt), style = MaterialTheme.typography.labelSmall)
            }
            if (report.messageText.isNotBlank()) {
                Surface(
                    shape = MaterialTheme.shapes.small,
                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
                ) {
                    Text(
                        stringResource(R.string.admin_report_message, report.messageText),
                        style = MaterialTheme.typography.bodySmall,
                        maxLines = 4, overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(8.dp)
                    )
                }
            }
            if (report.details.isNotBlank()) {
                Text(report.details, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(top = 8.dp))
            }
            Text(
                stringResource(R.string.admin_reported_by, report.reporterName) + if (reportedIsBanned) stringResource(R.string.admin_already_banned_suffix) else "",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 6.dp)
            )
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                // weight(fill = false): cada botón ocupa lo que necesita y solo encoge si no caben.
                TextButton(onClick = onDismiss, modifier = Modifier.weight(1f, fill = false)) { FitText(stringResource(R.string.admin_dismiss)) }
                TextButton(onClick = onResolve, modifier = Modifier.weight(1f, fill = false)) { FitText(stringResource(R.string.admin_resolve)) }
                if (!reportedIsBanned) TextButton(onClick = onBan, modifier = Modifier.weight(1f, fill = false)) {
                    FitText(stringResource(R.string.admin_ban_account), color = MaterialTheme.colorScheme.error)
                }
            }
        }
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
                Text(user.name + if (isMe) stringResource(R.string.admin_you_suffix) else "", maxLines = 1, overflow = TextOverflow.Ellipsis)
                Spacer(Modifier.width(6.dp))
                RoleBadge(user.role, showUser = true)
            }
        },
        supportingContent = {
            Text(
                listOf(if (user.banned) stringResource(R.string.admin_banned_label) else "", user.handle, user.email)
                    .filter { it.isNotBlank() }.joinToString(" · "),
                color = if (user.banned) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1, overflow = TextOverflow.Ellipsis
            )
        },
        trailingContent = {
            if (!isMe) Box {
                IconButton(onClick = { menu = true }) { Icon(Icons.Outlined.MoreVert, stringResource(R.string.admin_actions)) }
                DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                    Role.entries.filter { it != user.role }.forEach { r ->
                        DropdownMenuItem(
                            text = { FitText(stringResource(R.string.admin_change_role, stringResource(r.labelRes))) },
                            leadingIcon = {
                                Icon(if (r == Role.ADMIN) Icons.Outlined.Shield else Icons.Outlined.PersonOff, null)
                            },
                            onClick = { menu = false; onRole(r) }
                        )
                    }
                    if (user.banned) DropdownMenuItem(
                        text = { FitText(stringResource(R.string.admin_unban)) },
                        leadingIcon = { Icon(Icons.Outlined.CheckCircle, null) },
                        onClick = { menu = false; onBan(false) }
                    ) else DropdownMenuItem(
                        text = { FitText(stringResource(R.string.admin_ban_account)) },
                        leadingIcon = { Icon(Icons.Outlined.Block, null) },
                        onClick = { menu = false; onBan(true) }
                    )
                }
            }
        }
    )
}
