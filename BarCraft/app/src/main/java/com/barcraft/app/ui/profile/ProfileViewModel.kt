package com.barcraft.app.ui.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.barcraft.app.AppContainer
import com.barcraft.app.R
import com.barcraft.app.model.NotificationPrefs
import com.barcraft.app.model.ProfileLinks
import com.barcraft.app.ui.social.launchSafely
import com.barcraft.app.ui.theme.AppTheme
import com.barcraft.app.utils.Txt
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class ProfileViewModel(private val container: AppContainer) : ViewModel() {

    val theme: StateFlow<AppTheme> = container.settings.theme

    private val _messages = MutableSharedFlow<String>()
    val messages: SharedFlow<String> = _messages

    fun setTheme(theme: AppTheme) = container.settings.setTheme(theme)

    fun updateProfile(uid: String, name: String, bio: String) {
        if (name.trim().length < 2) {
            viewModelScope.launch { _messages.emit(Txt.get(R.string.msg_name_too_short)) }
            return
        }
        launchSafely(_messages, Txt.get(R.string.msg_profile_save_failed)) {
            container.userRepository.updateProfile(uid, name, bio)
            _messages.emit(Txt.get(R.string.msg_profile_updated))
        }
    }

    fun updateLinks(uid: String, links: ProfileLinks) {
        launchSafely(_messages, Txt.get(R.string.msg_links_save_failed)) {
            container.userRepository.updateLinks(uid, links)
            _messages.emit(Txt.get(R.string.msg_saved))
        }
    }

    // ── Configuración de la cuenta ───────────────────────────────────
    fun setPrivateAccount(uid: String, private: Boolean) {
        launchSafely(_messages, Txt.get(R.string.msg_privacy_failed)) {
            container.userRepository.setPrivateAccount(uid, private)
            _messages.emit(if (private) Txt.get(R.string.msg_account_private) else Txt.get(R.string.msg_account_public))
        }
    }

    fun notificationPrefs(uid: String): Flow<NotificationPrefs> =
        container.userRepository.observeNotificationPrefs(uid)

    fun setNotificationPref(uid: String, key: String, enabled: Boolean) {
        launchSafely(_messages, Txt.get(R.string.msg_notice_save_failed)) {
            container.userRepository.setNotificationPref(uid, key, enabled)
        }
    }

    // ── Seguridad (solo con cuenta) ──────────────────────────────────
    fun isBiometricEnabled(uid: String) = container.lockStore.isBiometricEnabled(uid)
    fun setBiometric(uid: String, enabled: Boolean) = container.lockStore.setBiometricEnabled(uid, enabled)

    /** Borra el PIN y bloquea: al instante se pide crear uno nuevo. */
    fun changePin(uid: String) {
        container.lockStore.clear(uid)
        container.appLock.lock()
    }

    fun lockNow() = container.appLock.lock()

    // ── Descargas de fotos y audios ──────────────────────────────────
    fun downloadsBytes(): Long =
        if (container.firebaseEnabled) container.mediaRepository.downloadedBytes() else 0L

    fun clearDownloads() {
        if (container.firebaseEnabled) container.mediaRepository.clearDownloads()
        viewModelScope.launch { _messages.emit(Txt.get(R.string.msg_downloads_cleared)) }
    }

    fun exportJson(onReady: (String) -> Unit) = viewModelScope.launch {
        onReady(container.cocktailRepository.exportJson())
    }
}
