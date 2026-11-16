package com.barcraft.app.ui.lock

import android.content.Context
import android.content.ContextWrapper
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_WEAK
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity

fun Context.findFragmentActivity(): FragmentActivity? {
    var ctx: Context = this
    while (ctx is ContextWrapper) {
        if (ctx is FragmentActivity) return ctx
        ctx = ctx.baseContext
    }
    return null
}

/** ¿El móvil tiene huella (u otra biometría) configurada? */
fun Context.biometricAvailable(): Boolean =
    BiometricManager.from(this).canAuthenticate(BIOMETRIC_WEAK) == BiometricManager.BIOMETRIC_SUCCESS

/**
 * Muestra el diálogo del sistema para la huella.
 * [onFailed] se llama en cada huella no reconocida; [onError] cuando el diálogo se cierra sin éxito.
 */
fun showBiometricPrompt(
    activity: FragmentActivity,
    onSuccess: () -> Unit,
    onFailed: () -> Unit,
    onError: () -> Unit
): BiometricPrompt {
    val prompt = BiometricPrompt(
        activity,
        ContextCompat.getMainExecutor(activity),
        object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) = onSuccess()
            override fun onAuthenticationFailed() = onFailed()
            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) = onError()
        }
    )
    val info = BiometricPrompt.PromptInfo.Builder()
        .setTitle("Desbloquear BarCraft")
        .setSubtitle("Usa tu huella para entrar")
        .setNegativeButtonText("Usar PIN")
        .setAllowedAuthenticators(BIOMETRIC_WEAK)
        .build()
    prompt.authenticate(info)
    return prompt
}
