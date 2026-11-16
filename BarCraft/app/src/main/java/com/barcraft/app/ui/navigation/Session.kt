@file:OptIn(ExperimentalCoroutinesApi::class)

package com.barcraft.app.ui.navigation

import androidx.compose.runtime.compositionLocalOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.barcraft.app.AppContainer
import com.barcraft.app.model.Role
import com.barcraft.app.model.UserProfile
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn

/** En qué situación está la persona que usa la app. */
sealed interface SessionState {
    data object Loading : SessionState
    data object SignedOut : SessionState
    /** Sin cuenta: solo recetas locales. [firebaseEnabled] = false si falta google-services.json. */
    data class Local(val firebaseEnabled: Boolean) : SessionState
    data class SignedIn(val profile: UserProfile) : SessionState
    data class Banned(val profile: UserProfile) : SessionState
}

/** Datos de sesión que cualquier pantalla puede leer. */
data class AppSession(
    val profile: UserProfile? = null,
    val firebaseEnabled: Boolean = false
) {
    val isSignedIn: Boolean get() = profile != null
    val isAdmin: Boolean get() = profile?.isAdmin == true
    /** Rol actual. Sin cuenta se trata como comensal. */
    val role: Role get() = profile?.role ?: Role.CLIENTE
    val uid: String? get() = profile?.uid
}

val LocalAppSession = compositionLocalOf { AppSession() }

class SessionViewModel(private val container: AppContainer) : ViewModel() {

    val state: StateFlow<SessionState> =
        if (!container.firebaseEnabled) {
            MutableStateFlow(SessionState.Local(firebaseEnabled = false))
        } else {
            combine(container.authRepository.observeUid(), container.settings.guestMode) { uid, guest -> uid to guest }
                .flatMapLatest { (uid, guest) ->
                    when {
                        uid != null -> container.userRepository.observeUser(uid)
                            .onEach { if (it == null) runCatching { container.authRepository.ensureProfile(uid) } }
                            .map { profile ->
                                when {
                                    profile == null -> SessionState.Loading
                                    profile.banned -> SessionState.Banned(profile)
                                    else -> SessionState.SignedIn(profile)
                                }
                            }
                        guest -> flowOf(SessionState.Local(firebaseEnabled = true))
                        else -> flowOf(SessionState.SignedOut)
                    }
                }
                .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SessionState.Loading)
        }

    fun continueAsGuest() = container.settings.setGuestMode(true)

    /** Sale del modo invitado para ir a la pantalla de inicio de sesión. */
    fun goToLogin() = container.settings.setGuestMode(false)

    fun logout() {
        container.settings.setGuestMode(false)
        if (container.firebaseEnabled) container.authRepository.logout()
    }
}
