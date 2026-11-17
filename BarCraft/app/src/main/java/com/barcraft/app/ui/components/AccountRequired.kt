package com.barcraft.app.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CloudOff
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.barcraft.app.R
import com.barcraft.app.ui.navigation.AppSession

/** Se muestra en Comunidad y Chats cuando no hay cuenta o falta configurar Firebase. */
@Composable
fun AccountRequiredState(session: AppSession, feature: String, onLogin: () -> Unit) {
    if (!session.firebaseEnabled) {
        EmptyState(
            icon = Icons.Outlined.CloudOff,
            title = stringResource(R.string.account_required_firebase_title),
            message = stringResource(R.string.account_required_firebase_message, feature)
        )
    } else {
        EmptyState(
            icon = Icons.Outlined.Lock,
            title = stringResource(R.string.account_required_login_title),
            message = stringResource(R.string.account_required_login_message, feature),
            actionLabel = stringResource(R.string.account_required_login_action),
            onAction = onLogin
        )
    }
}
