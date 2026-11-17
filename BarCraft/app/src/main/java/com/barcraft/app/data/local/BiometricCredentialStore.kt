package com.barcraft.app.data.local

import android.content.Context
import android.os.Build
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyPermanentlyInvalidatedException
import android.security.keystore.KeyProperties
import android.util.Base64
import org.json.JSONObject
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/**
 * "Iniciar sesión con huella" después de cerrar sesión.
 *
 * El email y la contraseña se guardan cifrados con una clave del Android Keystore que solo se puede
 * usar tras poner la huella (cada vez). La clave nunca sale del chip de seguridad del móvil y se
 * invalida sola si se añade una huella nueva en Ajustes: entonces hay que volver a activarlo.
 */
class BiometricCredentialStore(context: Context) {

    private val prefs = context.getSharedPreferences("barcraft_bio_login", Context.MODE_PRIVATE)

    /** Datos ya cifrados, a la espera de comprobar que la contraseña es buena. */
    class PendingLogin internal constructor(val email: String, internal val data: String, internal val iv: String)

    /** Email de la cuenta guardada para entrar con huella (null si no hay ninguna). */
    val savedEmail: String?
        get() = if (prefs.contains(KEY_DATA) && prefs.contains(KEY_IV)) prefs.getString(KEY_EMAIL, null) else null

    /** Cipher para cifrar; hay que autorizarlo con la huella. null si el móvil no lo permite. */
    fun encryptCipher(): Cipher? = try {
        newCipher().apply { init(Cipher.ENCRYPT_MODE, getOrCreateKey()) }
    } catch (e: KeyPermanentlyInvalidatedException) {
        // Han cambiado las huellas: se crea una clave nueva.
        deleteKey()
        runCatching { newCipher().apply { init(Cipher.ENCRYPT_MODE, getOrCreateKey()) } }.getOrNull()
    } catch (e: Exception) {
        null
    }

    sealed interface Unlock {
        data class Ready(val cipher: Cipher) : Unlock
        /** Se han añadido o borrado huellas: lo guardado ya no se puede leer. */
        data object Invalidated : Unlock
        data object Missing : Unlock
    }

    /** Cipher para descifrar lo guardado; hay que autorizarlo con la huella. */
    fun decryptCipher(): Unlock {
        val iv = prefs.getString(KEY_IV, null) ?: return Unlock.Missing
        if (!prefs.contains(KEY_DATA)) return Unlock.Missing
        return try {
            val key = loadKey() ?: run { clear(); return Unlock.Invalidated }
            Unlock.Ready(newCipher().apply { init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(128, iv.fromB64())) })
        } catch (e: KeyPermanentlyInvalidatedException) {
            clear()
            Unlock.Invalidated
        } catch (e: Exception) {
            clear()
            Unlock.Invalidated
        }
    }

    /** Cifra las credenciales con un cipher ya autorizado (no las guarda todavía). */
    fun seal(cipher: Cipher, email: String, password: String): PendingLogin {
        val json = JSONObject().put("e", email).put("p", password).toString()
        val data = cipher.doFinal(json.toByteArray(Charsets.UTF_8))
        return PendingLogin(email, data.toB64(), cipher.iv.toB64())
    }

    /** Guarda las credenciales (solo tras comprobar que el login ha funcionado). */
    fun save(pending: PendingLogin) {
        prefs.edit()
            .putString(KEY_EMAIL, pending.email)
            .putString(KEY_DATA, pending.data)
            .putString(KEY_IV, pending.iv)
            .apply()
    }

    /** Descifra con un cipher ya autorizado: (email, contraseña), o null si no se puede. */
    fun read(cipher: Cipher): Pair<String, String>? = runCatching {
        val data = prefs.getString(KEY_DATA, null) ?: return null
        val json = JSONObject(String(cipher.doFinal(data.fromB64()), Charsets.UTF_8))
        json.getString("e") to json.getString("p")
    }.getOrNull()

    /** Borra lo guardado y la clave. */
    fun clear() {
        prefs.edit().clear().apply()
        deleteKey()
    }

    private fun newCipher(): Cipher = Cipher.getInstance(TRANSFORMATION)

    private fun keyStore(): KeyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }

    private fun loadKey(): SecretKey? = keyStore().getKey(KEY_ALIAS, null) as? SecretKey

    private fun getOrCreateKey(): SecretKey {
        loadKey()?.let { return it }
        val spec = KeyGenParameterSpec.Builder(
            KEY_ALIAS,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
        )
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setKeySize(256)
            .setUserAuthenticationRequired(true)
            .setInvalidatedByBiometricEnrollment(true)
            .apply {
                // Huella en cada uso (0 segundos de validez), solo biometría fuerte.
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    setUserAuthenticationParameters(0, KeyProperties.AUTH_BIOMETRIC_STRONG)
                }
            }
            .build()
        return KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEYSTORE)
            .apply { init(spec) }
            .generateKey()
    }

    private fun deleteKey() {
        runCatching { keyStore().deleteEntry(KEY_ALIAS) }
    }

    private fun ByteArray.toB64(): String = Base64.encodeToString(this, Base64.NO_WRAP)
    private fun String.fromB64(): ByteArray = Base64.decode(this, Base64.NO_WRAP)

    private companion object {
        const val ANDROID_KEYSTORE = "AndroidKeyStore"
        const val KEY_ALIAS = "barcraft_biometric_login"
        const val TRANSFORMATION = "AES/GCM/NoPadding"
        const val KEY_EMAIL = "email"
        const val KEY_DATA = "data"
        const val KEY_IV = "iv"
    }
}
