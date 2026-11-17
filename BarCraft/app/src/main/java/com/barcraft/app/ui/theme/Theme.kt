package com.barcraft.app.ui.theme

import androidx.annotation.StringRes
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import com.barcraft.app.R
import com.barcraft.app.utils.Txt

/** Temas visuales que el usuario puede elegir en Perfil. */
enum class AppTheme(@StringRes val labelRes: Int, @StringRes val descriptionRes: Int, val emoji: String, val isLight: Boolean) {
    DELUXE(R.string.theme_deluxe, R.string.theme_deluxe_desc, "🥃", false),
    VOLCAN(R.string.theme_volcan, R.string.theme_volcan_desc, "🌋", false),
    ABISAL(R.string.theme_abisal, R.string.theme_abisal_desc, "🌊", false),
    CIELO(R.string.theme_cielo, R.string.theme_cielo_desc, "☀️", true);

    /** Nombre visible (sale de strings.xml). */
    val label: String get() = Txt.get(labelRes)
    val description: String get() = Txt.get(descriptionRes)

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
