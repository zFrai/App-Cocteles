package com.barcraft.app.ui.lock

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.barcraft.app.AppContainer
import com.barcraft.app.data.local.LockStore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class LockUiState(
    val needsSetup: Boolean,
    val pinRequired: Boolean,
    val biometricEnabled: Boolean,
    val biometricFails: Int = 0,
    val message: String? = null,
    val pinError: String? = null,
    val checkingPassword: Boolean = false,
    val passwordError: String? = null
)

/**
 * Reglas del bloqueo:
 * - Se entra con huella si está activada.
 * - Cada [LockStore.PIN_EVERY] entradas con huella se pide el PIN una vez.
 * - Tras [LockStore.MAX_BIOMETRIC_FAILS] huellas fallidas seguidas el PIN es obligatorio.
 * - Si se olvida el PIN, se recupera con la contraseña de la cuenta.
 */
class LockViewModel(private val container: AppContainer, private val uid: String) : ViewModel() {

    private val store = container.lockStore

    private val _state = MutableStateFlow(initialState())
    val state: StateFlow<LockUiState> = _state.asStateFlow()

    private fun initialState(): LockUiState {
        val bio = store.isBiometricEnabled(uid)
        val periodic = store.pinRequiredNow(uid)
        return LockUiState(
            needsSetup = !store.hasPin(uid),
            pinRequired = !bio || periodic,
            biometricEnabled = bio,
            message = if (bio && periodic) "Por seguridad, cada ${LockStore.PIN_EVERY} entradas te pedimos el PIN." else null
        )
    }

    /** Al volver a bloquearse, se reinician los intentos. */
    fun reset() { _state.value = initialState() }

    val unlocksLeft: Int get() = (LockStore.PIN_EVERY - store.biometricUnlocks(uid)).coerceAtLeast(0)

    fun createPin(pin: String, useBiometric: Boolean) {
        store.setPin(uid, pin)
        store.setBiometricEnabled(uid, useBiometric)
        container.appLock.unlock()
        _state.value = initialState()
    }

    fun onBiometricSuccess() {
        store.onBiometricUnlock(uid)
        container.appLock.unlock()
    }

    fun onBiometricFailed(): Boolean {
        val fails = _state.value.biometricFails + 1
        val forcePin = fails >= LockStore.MAX_BIOMETRIC_FAILS
        _state.update {
            it.copy(
                biometricFails = fails,
                pinRequired = it.pinRequired || forcePin,
                message = if (forcePin) "Huella no reconocida ${LockStore.MAX_BIOMETRIC_FAILS} veces. Introduce tu PIN." else it.message
            )
        }
        return forcePin
    }

    /** El diálogo de huella se cerró (botón "Usar PIN", cancelado o bloqueo del sistema). */
    fun onBiometricDismissed() {
        _state.update { it.copy(pinRequired = true) }
    }

    /** Volver a probar con la huella (solo si no se ha forzado el PIN). */
    fun canRetryBiometric(): Boolean {
        val s = _state.value
        return s.biometricEnabled && !store.pinRequiredNow(uid) && s.biometricFails < LockStore.MAX_BIOMETRIC_FAILS
    }

    fun retryBiometric() = _state.update { it.copy(pinRequired = false) }

    fun submitPin(pin: String): Boolean {
        return if (store.verifyPin(uid, pin)) {
            store.onPinUnlock(uid)
            container.appLock.unlock()
            _state.update { it.copy(pinError = null, biometricFails = 0) }
            true
        } else {
            _state.update { it.copy(pinError = "PIN incorrecto") }
            false
        }
    }

    /** "He olvidado el PIN": se comprueba la contraseña de la cuenta y se crea un PIN nuevo. */
    fun recoverWithPassword(password: String) {
        viewModelScope.launch {
            _state.update { it.copy(checkingPassword = true, passwordError = null) }
            val result = container.authRepository.verifyPassword(password)
            if (result.isSuccess) {
                store.clear(uid)
                _state.value = initialState().copy(message = "Contraseña correcta. Crea un PIN nuevo.")
            } else {
                _state.update { it.copy(checkingPassword = false, passwordError = result.exceptionOrNull()?.message) }
            }
        }
    }

    fun setBiometricEnabled(enabled: Boolean) {
        store.setBiometricEnabled(uid, enabled)
        _state.update { it.copy(biometricEnabled = enabled) }
    }
}
