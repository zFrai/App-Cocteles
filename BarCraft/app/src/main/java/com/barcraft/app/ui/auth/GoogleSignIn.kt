package com.barcraft.app.ui.auth

import android.annotation.SuppressLint
import android.content.Context
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import androidx.credentials.exceptions.NoCredentialException
import com.barcraft.app.ui.lock.findFragmentActivity
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenParsingException

/** Resultado de pedir a Google la cuenta con la que entrar. */
sealed interface GoogleTokenResult {
    data class Success(val idToken: String) : GoogleTokenResult
    /** La persona cerró el selector de cuentas: no es un error. */
    data object Cancelled : GoogleTokenResult
    data class Error(val message: String) : GoogleTokenResult
}

/**
 * Muestra el selector de cuentas de Google del sistema y devuelve el token para Firebase.
 *
 * El ID de cliente web (`default_web_client_id`) lo genera el plugin de Google Services a partir de
 * google-services.json cuando el proveedor Google está activado en Firebase Authentication.
 * Se busca en tiempo de ejecución para que la app compile aunque aún no exista.
 */
suspend fun requestGoogleIdToken(context: Context): GoogleTokenResult {
    val clientId = webClientId(context) ?: return GoogleTokenResult.Error(
        "Google no está configurado: activa el proveedor Google en Firebase Authentication y vuelve a descargar google-services.json."
    )
    val activityContext: Context = context.findFragmentActivity() ?: context
    val request = GetCredentialRequest.Builder()
        .addCredentialOption(GetSignInWithGoogleOption.Builder(clientId).build())
        .build()
    return try {
        val credential = CredentialManager.create(activityContext).getCredential(activityContext, request).credential
        if (credential is CustomCredential && credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
            GoogleTokenResult.Success(GoogleIdTokenCredential.createFrom(credential.data).idToken)
        } else {
            GoogleTokenResult.Error("Google ha devuelto una respuesta inesperada. Inténtalo de nuevo.")
        }
    } catch (e: GetCredentialCancellationException) {
        GoogleTokenResult.Cancelled
    } catch (e: NoCredentialException) {
        GoogleTokenResult.Error("No hay ninguna cuenta de Google en este móvil. Añádela en Ajustes > Cuentas.")
    } catch (e: GetCredentialException) {
        GoogleTokenResult.Error("No se pudo iniciar sesión con Google. ${e.message.orEmpty()}".trim())
    } catch (e: GoogleIdTokenParsingException) {
        GoogleTokenResult.Error("No se pudo leer la cuenta de Google. Inténtalo de nuevo.")
    }
}

@SuppressLint("DiscouragedApi")
private fun webClientId(context: Context): String? {
    val id = context.resources.getIdentifier("default_web_client_id", "string", context.packageName)
    return if (id == 0) null else context.getString(id).takeIf { it.isNotBlank() }
}
