package com.barcraft.app.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CloudOff
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.runtime.Composable
import com.barcraft.app.ui.navigation.AppSession

/** Se muestra en Comunidad y Chats cuando no hay cuenta o falta configurar Firebase. */
@Composable
fun AccountRequiredState(session: AppSession, feature: String, onLogin: () -> Unit) {
    if (!session.firebaseEnabled) {
        EmptyState(
            icon = Icons.Outlined.CloudOff,
            title = "Falta conectar Firebase",
            message = "$feature necesita un servidor. Añade el archivo google-services.json en la carpeta app/ " +
                "(los pasos están en FIREBASE_SETUP.md) y vuelve a compilar. Mientras tanto, tus recetas funcionan sin conexión."
        )
    } else {
        EmptyState(
            icon = Icons.Outlined.Lock,
            title = "Inicia sesión para entrar",
            message = "$feature es para usuarios con cuenta. Tus recetas locales se quedan como están.",
            actionLabel = "Iniciar sesión o registrarme",
            onAction = onLogin
        )
    }
}
