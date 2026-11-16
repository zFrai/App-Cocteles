package com.barcraft.app.data.remote

import android.content.Context
import android.util.Log
import com.google.firebase.FirebaseApp

/**
 * Comprueba si Firebase está configurado (si existe app/google-services.json).
 * Sin configuración la app funciona en modo local: recetas sí, comunidad y chat no.
 */
object FirebaseProvider {

    fun isConfigured(context: Context): Boolean = try {
        FirebaseApp.getApps(context).isNotEmpty() || FirebaseApp.initializeApp(context) != null
    } catch (e: Exception) {
        Log.w("BarCraft", "Firebase no disponible: ${e.message}")
        false
    }
}

/** Nombres de colecciones y campos en Firestore. */
object Collections {
    const val USERS = "users"
    /** Un documento por tag (`tags/{tag}` -> uid) para que no haya dos iguales. */
    const val TAGS = "tags"
    const val CHATS = "chats"
    const val MESSAGES = "messages"
    const val POSTS = "posts"
    const val COMMENTS = "comments"
    const val FRIENDSHIPS = "friendships"
    const val MEDIA = "media"
    const val PRIVATE = "private"
    const val SETTINGS_DOC = "settings"
}
