package com.barcraft.app.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.barcraft.app.AppContainer
import com.barcraft.app.model.Role
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class AuthUiState(
    val loading: Boolean = false,
    val error: String? = null,
    val info: String? = null
)

class AuthViewModel(private val container: AppContainer) : ViewModel() {

    private val _state = MutableStateFlow(AuthUiState())
    val state: StateFlow<AuthUiState> = _state.asStateFlow()

    private val emailRegex = Regex("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$")

    fun login(email: String, password: String) {
        if (!emailRegex.matches(email.trim())) return fail("Escribe un email válido.")
        if (password.isEmpty()) return fail("Escribe tu contraseña.")
        run { container.authRepository.login(email, password) }
    }

    fun register(name: String, email: String, password: String, confirm: String, role: Role) {
        if (name.trim().length < 2) return fail("El nombre debe tener al menos 2 letras.")
        if (!emailRegex.matches(email.trim())) return fail("Escribe un email válido.")
        if (password.length < 6) return fail("La contraseña debe tener al menos 6 caracteres.")
        if (password != confirm) return fail("Las contraseñas no coinciden.")
        run { container.authRepository.register(name, email, password, role) }
    }

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

    private fun fail(message: String) = _state.update { it.copy(error = message, info = null) }

    private fun run(block: suspend () -> Result<Unit>) {
        viewModelScope.launch {
            _state.update { it.copy(loading = true, error = null, info = null) }
            val result = block()
            if (result.isSuccess) {
                container.settings.setGuestMode(false)
                container.appLock.unlock() // acaba de escribir su contraseña: no pedimos también el PIN
            }
            _state.update { it.copy(loading = false, error = result.exceptionOrNull()?.message) }
        }
    }
}
