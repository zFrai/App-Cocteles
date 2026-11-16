package com.barcraft.app.ui.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.barcraft.app.AppContainer
import com.barcraft.app.ui.social.launchSafely
import com.barcraft.app.ui.theme.AppTheme
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
            viewModelScope.launch { _messages.emit("El nombre debe tener al menos 2 letras") }
            return
        }
        launchSafely(_messages, "No se pudo guardar el perfil") {
            container.userRepository.updateProfile(uid, name, bio)
            _messages.emit("Perfil actualizado")
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
        viewModelScope.launch { _messages.emit("Descargas borradas. Se volverán a bajar al abrirlas.") }
    }

    fun exportJson(onReady: (String) -> Unit) = viewModelScope.launch {
        onReady(container.cocktailRepository.exportJson())
    }
}
