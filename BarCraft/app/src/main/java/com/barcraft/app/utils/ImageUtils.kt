package com.barcraft.app.utils

import android.content.Context
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.util.UUID

object ImageUtils {

    /**
     * Copia una imagen elegida de la galería a la carpeta privada de la app.
     * Así la receta sigue mostrando la foto aunque se borre del móvil o se reinicie.
     */
    suspend fun copyToAppStorage(context: Context, source: Uri): String? = withContext(Dispatchers.IO) {
        runCatching {
            val dir = File(context.filesDir, "cocktail_images").apply { mkdirs() }
            val target = File(dir, "${UUID.randomUUID()}.jpg")
            context.contentResolver.openInputStream(source)?.use { input ->
                target.outputStream().use { output -> input.copyTo(output) }
            } ?: return@runCatching null
            Uri.fromFile(target).toString()
        }.getOrNull()
    }

    fun isRemote(uri: String): Boolean = uri.startsWith("http://") || uri.startsWith("https://")

    /** Borra una imagen local de la app (no hace nada con URLs). */
    fun deleteLocal(uri: String) {
        if (!uri.startsWith("file://")) return
        runCatching { Uri.parse(uri).path?.let { File(it).delete() } }
    }
}
