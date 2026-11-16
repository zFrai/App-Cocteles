package com.barcraft.app.utils

import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Decide cuándo se bloquea la app: al abrirla y al volver tras más de [graceMillis] en segundo plano.
 * (Así no pide la huella solo por abrir la cámara o el selector de fotos.)
 */
class AppLockController(private val graceMillis: Long = 180_000) : DefaultLifecycleObserver {

    private val _locked = MutableStateFlow(true)
    val locked: StateFlow<Boolean> = _locked.asStateFlow()

    private var backgroundSince = 0L

    fun start() {
        ProcessLifecycleOwner.get().lifecycle.addObserver(this)
    }

    fun unlock() { _locked.value = false }

    fun lock() { _locked.value = true }

    override fun onStop(owner: LifecycleOwner) {
        backgroundSince = System.currentTimeMillis()
    }

    override fun onStart(owner: LifecycleOwner) {
        if (backgroundSince > 0 && System.currentTimeMillis() - backgroundSince > graceMillis) {
            _locked.value = true
        }
    }
}
