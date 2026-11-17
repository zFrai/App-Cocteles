package com.barcraft.app.ui.lock

import android.content.Context
import android.content.ContextWrapper
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_STRONG
import androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_WEAK
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import com.barcraft.app.R
import javax.crypto.Cipher

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
        .setTitle(activity.getString(R.string.biometric_unlock_title))
        .setSubtitle(activity.getString(R.string.biometric_unlock_subtitle))
        .setNegativeButtonText(activity.getString(R.string.lock_use_pin))
        .setAllowedAuthenticators(BIOMETRIC_WEAK)
        .build()
    prompt.authenticate(info)
    return prompt
}

/** ¿Hay huella "fuerte" (la que permite usar claves cifradas del Keystore)? */
fun Context.strongBiometricAvailable(): Boolean =
    BiometricManager.from(this).canAuthenticate(BIOMETRIC_STRONG) == BiometricManager.BIOMETRIC_SUCCESS

/**
 * Diálogo de huella que además autoriza un [Cipher] del Keystore (para cifrar o leer credenciales).
 * [onError] recibe null si la persona simplemente canceló, o el mensaje del sistema si algo falló.
 */
fun showCryptoBiometricPrompt(
    activity: FragmentActivity,
    cipher: Cipher,
    title: String,
    subtitle: String,
    onSuccess: (Cipher) -> Unit,
    onError: (String?) -> Unit
): BiometricPrompt {
    val prompt = BiometricPrompt(
        activity,
        ContextCompat.getMainExecutor(activity),
        object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                val authorized = result.cryptoObject?.cipher
                if (authorized != null) onSuccess(authorized) else onError(activity.getString(R.string.biometric_error_retry))
            }

            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                val cancelled = errorCode == BiometricPrompt.ERROR_USER_CANCELED ||
                    errorCode == BiometricPrompt.ERROR_NEGATIVE_BUTTON ||
                    errorCode == BiometricPrompt.ERROR_CANCELED
                onError(if (cancelled) null else errString.toString())
            }
        }
    )
    val info = BiometricPrompt.PromptInfo.Builder()
        .setTitle(title)
        .setSubtitle(subtitle)
        .setNegativeButtonText(activity.getString(R.string.action_cancel))
        .setAllowedAuthenticators(BIOMETRIC_STRONG)
        .build()
    prompt.authenticate(info, BiometricPrompt.CryptoObject(cipher))
    return prompt
}
