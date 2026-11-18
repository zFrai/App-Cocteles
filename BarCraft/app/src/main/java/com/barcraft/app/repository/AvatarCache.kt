package com.barcraft.app.repository

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Fotos de perfil que ya se conocen (uid -> id del archivo en `media`, "" = sin foto).
 *
 * Las publicaciones, comentarios y chats solo guardan el uid del autor. Para pintar su foto se lee
 * su perfil una vez por sesión y se recuerda aquí; las pantallas que ya tienen el perfil completo
 * (listas de personas, Perfil) lo apuntan con [set] para no volver a leerlo.
 */
class AvatarCache(private val users: UserRepository) {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val requested = mutableSetOf<String>()
    private val _photos = MutableStateFlow<Map<String, String>>(emptyMap())
    val photos: StateFlow<Map<String, String>> = _photos

    /** Pide la foto de [uid] si aún no se conoce. */
    fun load(uid: String) {
        if (uid.isBlank() || uid in _photos.value || !requested.add(uid)) return
        scope.launch {
            runCatching { users.photoIdOf(uid) }
                .onSuccess { id -> set(uid, id) }
                .onFailure { requested.remove(uid) } // sin conexión: se reintenta la próxima vez que se vea
        }
    }

    fun set(uid: String, photoId: String) {
        if (uid.isBlank() || _photos.value[uid] == photoId) return
        _photos.update { it + (uid to photoId) }
    }
}
