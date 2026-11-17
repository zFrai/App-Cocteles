package com.barcraft.app.ui.qr

import androidx.compose.ui.res.stringResource
import com.barcraft.app.R
import com.barcraft.app.ui.components.FitText
import android.content.Context
import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.barcraft.app.model.UserProfile
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.codescanner.GmsBarcodeScannerOptions
import com.google.mlkit.vision.codescanner.GmsBarcodeScanning
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter

/**
 * QR del perfil. Contiene "barcraft://u/{uid}": al escanearlo en Buscar se sigue a esa cuenta
 * (o se le envía solicitud si es privada).
 */
object ProfileQr {
    private const val PREFIX = "barcraft://u/"

    fun contentFor(uid: String): String = PREFIX + uid

    /** uid del QR, o null si no es un QR de BarCraft. */
    fun parse(raw: String?): String? =
        raw?.trim()?.takeIf { it.startsWith(PREFIX) }?.removePrefix(PREFIX)
            ?.takeIf { it.isNotBlank() && it.all { c -> c.isLetterOrDigit() } }

    fun bitmap(content: String, size: Int = 512): ImageBitmap {
        val matrix = QRCodeWriter().encode(
            content, BarcodeFormat.QR_CODE, size, size, mapOf(EncodeHintType.MARGIN to 1)
        )
        val pixels = IntArray(size * size) { i ->
            if (matrix[i % size, i / size]) android.graphics.Color.BLACK else android.graphics.Color.WHITE
        }
        return Bitmap.createBitmap(pixels, size, size, Bitmap.Config.ARGB_8888).asImageBitmap()
    }

    /**
     * Abre el escáner de QR de Google Play services (no necesita permiso de cámara).
     * [onUid] recibe el uid leído; [onError] un texto para mostrar.
     */
    fun scan(context: Context, onUid: (String) -> Unit, onError: (String) -> Unit) {
        val options = GmsBarcodeScannerOptions.Builder()
            .setBarcodeFormats(Barcode.FORMAT_QR_CODE)
            .build()
        GmsBarcodeScanning.getClient(context, options).startScan()
            .addOnSuccessListener { barcode ->
                val uid = parse(barcode.rawValue)
                if (uid != null) onUid(uid) else onError(context.getString(R.string.qr_not_barcraft))
            }
            .addOnFailureListener { onError(context.getString(R.string.qr_scanner_failed)) }
        // Si la persona cierra el escáner sin leer nada (cancelado) no se muestra nada.
    }
}

/** Diálogo con mi QR y mi @nombre #1234 para que otra persona me siga. */
@Composable
fun MyQrDialog(me: UserProfile, onDismiss: () -> Unit) {
    val qr = remember(me.uid) { ProfileQr.bitmap(ProfileQr.contentFor(me.uid)) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.qr_my_title)) },
        text = {
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Image(
                    bitmap = qr,
                    contentDescription = stringResource(R.string.qr_image_description),
                    modifier = Modifier.size(220.dp).background(Color.White).padding(8.dp)
                )
                if (me.handle.isNotBlank()) Text(me.handle, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                Text(
                    if (me.privateAccount) stringResource(R.string.qr_scan_hint_private) else stringResource(R.string.qr_scan_hint),
                    style = MaterialTheme.typography.bodySmall,
                    textAlign = TextAlign.Center
                )
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { FitText(stringResource(R.string.action_close)) } }
    )
}
