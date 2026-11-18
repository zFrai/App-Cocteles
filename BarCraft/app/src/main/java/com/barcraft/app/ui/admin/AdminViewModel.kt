package com.barcraft.app.ui.admin

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.barcraft.app.R
import com.barcraft.app.model.Post
import com.barcraft.app.model.Report
import com.barcraft.app.model.ReportStatus
import com.barcraft.app.model.Role
import com.barcraft.app.model.UserProfile
import com.barcraft.app.repository.ReportRepository
import com.barcraft.app.repository.SocialRepository
import com.barcraft.app.repository.UserRepository
import com.barcraft.app.ui.social.launchSafely
import com.barcraft.app.utils.Txt
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
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
        social.observeFeed(limit = 200).catch { emit(emptyList()) }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /** Denuncias pendientes de revisar (null = cargando). */
    val openReports: StateFlow<List<Report>?> =
        reports.observeOpen().map<List<Report>, List<Report>?> { it }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    private val _messages = MutableSharedFlow<String>()
    val messages: SharedFlow<String> = _messages

    fun setRole(user: UserProfile, role: Role) =
        launchSafely(_messages, Txt.get(R.string.msg_admin_no_permission)) {
            users.setRole(user.uid, role)
            _messages.emit(Txt.get(R.string.msg_admin_role_changed, user.name, role.label))
        }

    fun setBanned(user: UserProfile, banned: Boolean) =
        launchSafely(_messages, Txt.get(R.string.msg_admin_no_permission)) {
            users.setBanned(user.uid, banned)
            _messages.emit(if (banned) Txt.get(R.string.msg_admin_banned, user.name) else Txt.get(R.string.msg_admin_unbanned, user.name))
        }

    fun deletePost(post: Post) =
        launchSafely(_messages, Txt.get(R.string.msg_post_delete_failed)) {
            social.deletePost(post.id)
            _messages.emit(Txt.get(R.string.msg_post_deleted))
        }

    /** Marca la denuncia como revisada (sin más acción) o la descarta. */
    fun closeReport(report: Report, status: ReportStatus) =
        launchSafely(_messages, Txt.get(R.string.msg_admin_report_failed)) {
            reports.close(report.id, adminUid, status)
            _messages.emit(if (status == ReportStatus.DISMISSED) Txt.get(R.string.msg_admin_report_dismissed) else Txt.get(R.string.msg_admin_report_resolved))
        }

    /** Bloquea la cuenta denunciada y da la denuncia por revisada. */
    fun banReported(report: Report) =
        launchSafely(_messages, Txt.get(R.string.msg_admin_no_permission)) {
            users.setBanned(report.reportedId, true)
            reports.close(report.id, adminUid, ReportStatus.RESOLVED)
            _messages.emit(Txt.get(R.string.msg_admin_banned, report.reportedName))
        }
}
