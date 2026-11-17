package com.barcraft.app.ui.admin

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.barcraft.app.model.Post
import com.barcraft.app.model.Report
import com.barcraft.app.model.ReportStatus
import com.barcraft.app.model.Role
import com.barcraft.app.model.UserProfile
import com.barcraft.app.repository.ReportRepository
import com.barcraft.app.repository.SocialRepository
import com.barcraft.app.repository.UserRepository
import com.barcraft.app.ui.social.launchSafely
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

/** Panel de administración: roles, bloqueos, moderación del muro y denuncias. */
class AdminViewModel(
    private val users: UserRepository,
    private val social: SocialRepository,
    private val reports: ReportRepository,
    private val adminUid: String
) : ViewModel() {

    val allUsers: StateFlow<List<UserProfile>?> =
        // El email ya no está en el perfil público: se cruza con emails/{uid}, que solo lee el Admin.
        combine(users.observeAll(), users.observeEmails().onStart { emit(emptyMap()) }) { list, emails ->
            list.map { u -> u.copy(email = emails[u.uid] ?: u.email) }
        }.map<List<UserProfile>, List<UserProfile>?> { it }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val posts: StateFlow<List<Post>> =
        social.observeFeed(limit = 200).stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /** Denuncias pendientes de revisar (null = cargando). */
    val openReports: StateFlow<List<Report>?> =
        reports.observeOpen().map<List<Report>, List<Report>?> { it }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

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

    /** Marca la denuncia como revisada (sin más acción) o la descarta. */
    fun closeReport(report: Report, status: ReportStatus) =
        launchSafely(_messages, "No se pudo actualizar la denuncia") {
            reports.close(report.id, adminUid, status)
            _messages.emit(if (status == ReportStatus.DISMISSED) "Denuncia descartada" else "Denuncia revisada")
        }

    /** Bloquea la cuenta denunciada y da la denuncia por revisada. */
    fun banReported(report: Report) =
        launchSafely(_messages, "No tienes permiso o no hay conexión") {
            users.setBanned(report.reportedId, true)
            reports.close(report.id, adminUid, ReportStatus.RESOLVED)
            _messages.emit("${report.reportedName} ha sido bloqueado")
        }
}
