package com.barcraft.app.ui.components

import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.LocalTextStyle
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.takeOrElse
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.isSpecified
import androidx.compose.ui.unit.sp

/**
 * Texto de una sola línea que se encoge para caber en el ancho disponible.
 *
 * Úsalo en lugar de `Text` en botones, pestañas, chips, la barra inferior, menús y títulos:
 * nunca salta de línea ni se corta con "…"; si no cabe, baja el tamaño de letra
 * (como mucho hasta [minFontSize]). Hereda el estilo y el color del sitio donde se pone
 * (por ejemplo, el de un botón o una pestaña), igual que `Text`.
 *
 * NO lo uses en textos largos que sí deben ocupar varias líneas: ingredientes, pasos y
 * descripción de las recetas, y publicaciones o comentarios de la comunidad.
 */
@Composable
fun FitText(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = Color.Unspecified,
    style: TextStyle = LocalTextStyle.current,
    fontWeight: FontWeight? = null,
    textAlign: TextAlign = TextAlign.Unspecified,
    minFontSize: TextUnit = 8.sp
) {
    val textColor = color.takeOrElse { style.color.takeOrElse { LocalContentColor.current } }
    val merged = style.merge(TextStyle(color = textColor, fontWeight = fontWeight, textAlign = textAlign))
    // Tamaño "normal" del sitio donde va el texto; solo se reduce desde ahí, nunca crece.
    val maxSize = if (merged.fontSize.isSpecified && merged.fontSize.isSp) merged.fontSize else 14.sp
    val minSize = if (minFontSize.isSp && minFontSize.value < maxSize.value) minFontSize else maxSize
    BasicText(
        text = text,
        modifier = modifier,
        style = merged,
        maxLines = 1,
        softWrap = false,
        // Solo por si ni con la letra mínima cabe: se recorta sin "…".
        overflow = TextOverflow.Clip,
        autoSize = TextAutoSize.StepBased(
            minFontSize = minSize,
            maxFontSize = maxSize,
            stepSize = 0.5.sp
        )
    )
}
