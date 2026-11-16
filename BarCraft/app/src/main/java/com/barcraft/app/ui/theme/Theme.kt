package com.barcraft.app.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp

/** Temas visuales que el usuario puede elegir en Perfil. */
enum class AppTheme(val label: String, val description: String, val emoji: String, val isLight: Boolean) {
    DELUXE("Deluxe", "Negro y dorado", "🥃", false),
    VOLCAN("Volcán", "Brasas y ámbar", "🌋", false),
    ABISAL("Abisal", "Azul profundo", "🌊", false),
    CIELO("Cielo", "Claro y fresco", "☀️", true);

    val colorScheme: ColorScheme
        get() = when (this) {
            DELUXE -> DeluxeScheme
            VOLCAN -> VolcanScheme
            ABISAL -> AbisalScheme
            CIELO -> CieloScheme
        }
}

private val BarCraftShapes = Shapes(
    extraSmall = RoundedCornerShape(6.dp),
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(32.dp)
)

@Composable
fun BarCraftTheme(theme: AppTheme = AppTheme.DELUXE, content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = theme.colorScheme,
        typography = BarCraftTypography,
        shapes = BarCraftShapes,
        content = content
    )
}
