package com.barcraft.app.data.local

import android.content.Context
import com.barcraft.app.ui.theme.AppTheme
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Preferencias sencillas del dispositivo: tema visual y modo invitado. */
class SettingsStore(context: Context) {

    private val prefs = context.getSharedPreferences("barcraft_settings", Context.MODE_PRIVATE)

    private val _theme = MutableStateFlow(readTheme())
    val theme: StateFlow<AppTheme> = _theme.asStateFlow()

    private val _guestMode = MutableStateFlow(prefs.getBoolean(KEY_GUEST, false))
    val guestMode: StateFlow<Boolean> = _guestMode.asStateFlow()

    fun setTheme(theme: AppTheme) {
        prefs.edit().putString(KEY_THEME, theme.name).apply()
        _theme.value = theme
    }

    fun setGuestMode(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_GUEST, enabled).apply()
        _guestMode.value = enabled
    }

    private fun readTheme(): AppTheme {
        val name = prefs.getString(KEY_THEME, null)
        return AppTheme.entries.firstOrNull { it.name == name } ?: AppTheme.DELUXE
    }

    private companion object {
        const val KEY_THEME = "app_theme"
        const val KEY_GUEST = "guest_mode"
    }
}
