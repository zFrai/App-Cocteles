package com.barcraft.app.data.local

import android.content.Context
import java.security.SecureRandom
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

/**
 * Bloqueo de la app con PIN + huella, guardado solo en este móvil.
 * El PIN nunca se guarda en claro: se guarda un hash PBKDF2 con sal.
 */
class LockStore(context: Context) {

    private val prefs = context.getSharedPreferences("barcraft_lock", Context.MODE_PRIVATE)

    fun hasPin(uid: String): Boolean = prefs.contains(key(uid, "hash"))

    fun setPin(uid: String, pin: String) {
        val salt = ByteArray(16).also { SecureRandom().nextBytes(it) }
        prefs.edit()
            .putString(key(uid, "salt"), salt.toHex())
            .putString(key(uid, "hash"), hash(pin, salt))
            .putInt(key(uid, "count"), 0)
            .apply()
    }

    fun verifyPin(uid: String, pin: String): Boolean {
        val salt = prefs.getString(key(uid, "salt"), null)?.fromHex() ?: return false
        return prefs.getString(key(uid, "hash"), null) == hash(pin, salt)
    }

    fun isBiometricEnabled(uid: String): Boolean = prefs.getBoolean(key(uid, "bio"), false)

    fun setBiometricEnabled(uid: String, enabled: Boolean) {
        prefs.edit().putBoolean(key(uid, "bio"), enabled).apply()
    }

    /** Entradas con huella desde la última vez que se escribió el PIN. */
    fun biometricUnlocks(uid: String): Int = prefs.getInt(key(uid, "count"), 0)

    fun onBiometricUnlock(uid: String) {
        prefs.edit().putInt(key(uid, "count"), biometricUnlocks(uid) + 1).apply()
    }

    fun onPinUnlock(uid: String) {
        prefs.edit().putInt(key(uid, "count"), 0).apply()
    }

    /** ¿Toca pedir el PIN aunque la huella esté activa? */
    fun pinRequiredNow(uid: String): Boolean = biometricUnlocks(uid) >= PIN_EVERY

    fun clear(uid: String) {
        prefs.edit().apply { listOf("hash", "salt", "bio", "count").forEach { remove(key(uid, it)) } }.apply()
    }

    private fun key(uid: String, name: String) = "${uid}_$name"

    private fun hash(pin: String, salt: ByteArray): String {
        val spec = PBEKeySpec(pin.toCharArray(), salt, 20_000, 256)
        return SecretKeyFactory.getInstance("PBKDF2WithHmacSHA1").generateSecret(spec).encoded.toHex()
    }

    private fun ByteArray.toHex() = joinToString("") { "%02x".format(it) }
    private fun String.fromHex() = chunked(2).map { it.toInt(16).toByte() }.toByteArray()

    companion object {
        /** Cada cuántas entradas con huella se pide el PIN. */
        const val PIN_EVERY = 30
        /** Huellas fallidas seguidas antes de obligar a usar el PIN. */
        const val MAX_BIOMETRIC_FAILS = 3
        /** PIN de 4 a 6 cifras. */
        val PIN_LENGTH = 4..6
    }
}
