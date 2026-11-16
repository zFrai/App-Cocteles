package com.barcraft.app

import android.graphics.Color
import android.os.Bundle
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.DisposableEffect
import androidx.fragment.app.FragmentActivity
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.barcraft.app.ui.navigation.BarCraftRoot
import com.barcraft.app.ui.theme.BarCraftTheme

/** FragmentActivity (subclase de ComponentActivity) porque el diálogo de huella lo necesita. */
class MainActivity : FragmentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val container = (application as BarCraftApplication).container

        setContent {
            val theme by container.settings.theme.collectAsStateWithLifecycle()

            // Iconos de la barra de estado legibles en temas claros y oscuros.
            DisposableEffect(theme) {
                val style = if (theme.isLight) {
                    SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT)
                } else {
                    SystemBarStyle.dark(Color.TRANSPARENT)
                }
                enableEdgeToEdge(statusBarStyle = style, navigationBarStyle = style)
                onDispose { }
            }

            BarCraftTheme(theme) {
                BarCraftRoot()
            }
        }
    }
}
