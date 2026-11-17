package com.barcraft.app.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.barcraft.app.AppContainer
import com.barcraft.app.data.local.BiometricCredentialStore
import com.barcraft.app.model.Role
import com.barcraft.app.repository.AuthException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.crypto.Cipher

data class AuthUiState(
    val loading: Boolean = false,
    val error: String? = null,
    val info: String? = null,
    /** Cuenta guardada en este móvil para entrar con huella (null si no hay). */
    val fingerprintEmail: String? = null
)

class AuthViewModel(private val container: AppContainer) : ViewModel() {

    private val bioStore = container.biometricLogin

    private val _state = MutableStateFlow(AuthUiState(fingerprintEmail = bioStore.savedEmail))
    val state: StateFlow<AuthUiState> = _state.asStateFlow()

    private val emailRegex = Regex("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$")

    /** Comprueba los campos antes de pedir la huella o llamar a Firebase. */
    fun validateLogin(email: String, password: String): Boolean {
        if (!emailRegex.matches(email.trim())) { fail("Escribe un email válido."); return false }
        if (password.isEmpty()) { fail("Escribe tu contraseña."); return false }
        return true
    }

    /**
     * Entra con email y contraseña. Si llega [authorizedCipher] (huella ya puesta), las credenciales
     * se guardan cifradas para poder entrar con huella la próxima vez, pero solo si el login funciona.
     */
    fun login(email: String, password: String, authorizedCipher: Cipher? = null) {
        if (!validateLogin(email, password)) return
        val pending = authorizedCipher?.let { runCatching { bioStore.seal(it, email.trim(), password) }.getOrNull() }
        run {
            container.authRepository.login(email, password).onSuccess {
                if (pending != null) {
                    bioStore.save(pending)
                    _state.update { it.copy(fingerprintEmail = pending.email) }
                }
            }
        }
    }

    fun register(name: String, email: String, password: String, confirm: String, role: Role) {
        if (name.trim().length < 2) return fail("El nombre debe tener al menos 2 letras.")
        if (!emailRegex.matches(email.trim())) return fail("Escribe un email válido.")
        if (password.length < 6) return fail("La contraseña debe tener al menos 6 caracteres.")
        if (password != confirm) return fail("Las contraseñas no coinciden.")
        run { container.authRepository.register(name, email, password, role) }
    }

    // ---------- Google ----------

    /** Se está abriendo el selector de cuentas de Google. */
    fun onGoogleStarted() = _state.update { it.copy(loading = true, error = null, info = null) }

    fun onGoogleResult(result: GoogleTokenResult, role: Role) {
        when (result) {
            is GoogleTokenResult.Success -> run { container.authRepository.signInWithGoogle(result.idToken, role) }
            is GoogleTokenResult.Error -> fail(result.message)
            GoogleTokenResult.Cancelled -> _state.update { it.copy(loading = false) }
        }
    }

    // ---------- Huella ----------

    /** Cipher para guardar las credenciales (null si este móvil no lo permite). */
    fun cipherToSaveFingerprint(): Cipher? = bioStore.encryptCipher().also {
        if (it == null) fail("Este móvil no permite guardar el acceso con huella. Entra con tu contraseña.")
    }

    /** Cipher para leer las credenciales guardadas (null si ya no valen; el mensaje lo pone esta función). */
    fun cipherToLoginWithFingerprint(): Cipher? = when (val unlock = bioStore.decryptCipher()) {
        is BiometricCredentialStore.Unlock.Ready -> unlock.cipher
        BiometricCredentialStore.Unlock.Invalidated -> {
            _state.update { it.copy(fingerprintEmail = null) }
            fail("Las huellas del móvil han cambiado. Entra con tu contraseña y vuelve a activar la huella.")
            null
        }
        BiometricCredentialStore.Unlock.Missing -> {
            _state.update { it.copy(fingerprintEmail = null) }
            null
        }
    }

    /** La huella se ha reconocido: se leen las credenciales y se entra. */
    fun loginWithFingerprint(authorizedCipher: Cipher) {
        val credentials = bioStore.read(authorizedCipher)
        if (credentials == null) {
            forgetFingerprint()
            return fail("No se pudo leer el acceso guardado. Entra con tu contraseña y vuelve a activar la huella.")
        }
        val (email, password) = credentials
        run {
            val result = container.authRepository.login(email, password)
            val error = result.exceptionOrNull()
            if (error is AuthException && error.wrongCredentials) {
                // La contraseña guardada ya no sirve: se olvida para no seguir fallando.
                forgetFingerprint()
                Result.failure(AuthException("Tu contraseña ha cambiado. Entra con la nueva y vuelve a activar la huella."))
            } else {
                result
            }
        }
    }

    /** Borra el acceso con huella guardado en este móvil. */
    fun forgetFingerprint() {
        bioStore.clear()
        _state.update { it.copy(fingerprintEmail = null) }
    }

    fun showError(message: String) = fail(message)

    // ---------- Contraseña olvidada ----------

    fun resetPassword(email: String) {
        if (!emailRegex.matches(email.trim())) return fail("Escribe tu email arriba para recuperar la contraseña.")
        viewModelScope.launch {
            _state.update { it.copy(loading = true, error = null, info = null) }
            val result = container.authRepository.sendPasswordReset(email)
            _state.update {
                it.copy(
                    loading = false,
                    info = if (result.isSuccess) "Te hemos enviado un email para restablecer la contraseña." else null,
                    error = result.exceptionOrNull()?.message
                )
            }
        }
    }

    fun clearMessages() = _state.update { it.copy(error = null, info = null) }

    private fun fail(message: String) = _state.update { it.copy(loading = false, error = message, info = null) }

    private fun run(block: suspend () -> Result<Unit>) {
        viewModelScope.launch {
            _state.update { it.copy(loading = true, error = null, info = null) }
            val result = block()
            if (result.isSuccess) {
                container.settings.setGuestMode(false)
                container.appLock.unlock() // acaba de identificarse: no pedimos también el PIN
            }
            _state.update { it.copy(loading = false, error = result.exceptionOrNull()?.message) }
        }
    }
}
