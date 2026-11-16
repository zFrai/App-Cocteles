package com.barcraft.app.ui.admin

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.barcraft.app.model.Post
import com.barcraft.app.model.Role
import com.barcraft.app.model.UserProfile
import com.barcraft.app.repository.SocialRepository
import com.barcraft.app.repository.UserRepository
import com.barcraft.app.ui.social.launchSafely
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

/** Panel de administración: roles, bloqueos y moderación del muro. */
class AdminViewModel(
    private val users: UserRepository,
    private val social: SocialRepository
) : ViewModel() {

    val allUsers: StateFlow<List<UserProfile>?> =
        users.observeAll().map<List<UserProfile>, List<UserProfile>?> { it }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val posts: StateFlow<List<Post>> =
        social.observeFeed(limit = 200).stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _messages = MutableSharedFlow<String>()
    val messages: SharedFlow<String> = _messages

    fun setRole(user: UserProfile, role: Role) =
        launchSafely(_messages, "No tienes permiso o no hay conexión") {
            users.setRole(user.uid, role)
            _messages.emit("${user.name} ahora es ${role.label}")
        }

    fun setBanned(user: UserProfile, banned: Boolean) =
        launchSafely(_messages, "No tienes permiso o no hay conexión") {
            users.setBanned(user.uid, banned)
            _messages.emit(if (banned) "${user.name} ha sido bloqueado" else "${user.name} ya puede volver a entrar")
        }

    fun deletePost(post: Post) =
        launchSafely(_messages, "No se pudo borrar la publicación") {
            social.deletePost(post.id)
            _messages.emit("Publicación eliminada")
        }
}
