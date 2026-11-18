package com.barcraft.app.repository

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import com.barcraft.app.data.remote.Collections
import com.google.firebase.firestore.Blob
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File

/**
 * Fotos y notas de voz compartidas (chat y comunidad).
 *
 * El archivo se guarda en el servidor (colección `media` de Firestore, como binario de hasta ~900 KB)
 * y se descarga al móvil la primera vez que se ve. A partir de ahí se abre desde el móvil,
 * también sin conexión. Desde Perfil se pueden borrar las descargas para liberar espacio.
 *
 * No usa Firebase Storage porque desde 2024 exige el plan de pago.
 */
class MediaRepository(private val db: FirebaseFirestore, context: Context) {

    private val appContext = context.applicationContext
    private val dir = File(appContext.filesDir, "media").apply { mkdirs() }
    private val media get() = db.collection(Collections.MEDIA)
    private val downloadLock = Mutex()

    /** Comprime la foto (máx. 1280 px) y la sube. Devuelve el id del archivo. */
    suspend fun uploadImage(ownerUid: String, source: Uri): String {
        val bytes = withContext(Dispatchers.Default) { compressImage(source) }
        return upload(ownerUid, bytes, "image/jpeg")
    }

    /** Foto de perfil: se recorta en cuadrado por el centro (se ve en círculo) y se reduce a [AVATAR_SIDE] px. */
    suspend fun uploadAvatar(ownerUid: String, source: Uri): String {
        val bytes = withContext(Dispatchers.Default) {
            val full = decode(source)
            val side = minOf(full.width, full.height)
            val square = Bitmap.createBitmap(full, (full.width - side) / 2, (full.height - side) / 2, side, side)
            val scaled = if (side > AVATAR_SIDE) Bitmap.createScaledBitmap(square, AVATAR_SIDE, AVATAR_SIDE, true) else square
            ByteArrayOutputStream().also { scaled.compress(Bitmap.CompressFormat.JPEG, 85, it) }.toByteArray()
        }
        return upload(ownerUid, bytes, "image/jpeg")
    }

    /** Sube una nota de voz ya grabada. */
    suspend fun uploadAudio(ownerUid: String, file: File): String {
        val bytes = withContext(Dispatchers.IO) { file.readBytes() }
        require(bytes.size <= MAX_BYTES) { "La nota de voz es demasiado larga" }
        return upload(ownerUid, bytes, "audio/mp4")
    }

    private suspend fun upload(ownerUid: String, bytes: ByteArray, mime: String): String {
        val doc = media.document()
        doc.set(
            mapOf(
                "ownerId" to ownerUid,
                "mime" to mime,
                "size" to bytes.size,
                "data" to Blob.fromBytes(bytes),
                "createdAt" to FieldValue.serverTimestamp()
            )
        ).await()
        withContext(Dispatchers.IO) { File(dir, doc.id).writeBytes(bytes) } // quien lo sube ya lo tiene
        return doc.id
    }

    /** Devuelve el archivo en el móvil, descargándolo si aún no está. */
    suspend fun localFile(mediaId: String): File? {
        if (mediaId.isBlank()) return null
        val file = File(dir, mediaId)
        if (file.exists() && file.length() > 0) return file
        return downloadLock.withLock {
            if (file.exists() && file.length() > 0) return@withLock file
            runCatching {
                val snap = media.document(mediaId).get().await()
                val blob = snap.getBlob("data") ?: return@runCatching null
                withContext(Dispatchers.IO) { file.writeBytes(blob.toBytes()) }
                file
            }.getOrNull()
        }
    }

    fun isDownloaded(mediaId: String): Boolean = File(dir, mediaId).exists()

    suspend fun delete(mediaId: String) {
        if (mediaId.isBlank()) return
        runCatching { media.document(mediaId).delete().await() }
        File(dir, mediaId).delete()
    }

    /** Espacio que ocupan las descargas, en bytes. */
    fun downloadedBytes(): Long = dir.listFiles()?.sumOf { it.length() } ?: 0L

    /** Borra las descargas. Se volverán a bajar si se abren con conexión. */
    fun clearDownloads() {
        dir.listFiles()?.forEach { it.delete() }
    }

    /** Copia una foto descargada a la galería del móvil (carpeta Imágenes/BarCraft). */
    suspend fun saveToGallery(mediaId: String): Boolean = withContext(Dispatchers.IO) {
        val file = localFile(mediaId) ?: return@withContext false
        runCatching {
            val name = "BarCraft_${System.currentTimeMillis()}.jpg"
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val values = ContentValues().apply {
                    put(MediaStore.Images.Media.DISPLAY_NAME, name)
                    put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
                    put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/BarCraft")
                }
                val uri = appContext.contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
                    ?: return@runCatching false
                appContext.contentResolver.openOutputStream(uri)?.use { out -> file.inputStream().use { it.copyTo(out) } }
                true
            } else {
                // Android 8 y 9: se guarda en la carpeta de imágenes de la app (no pide permisos).
                val target = File(appContext.getExternalFilesDir(Environment.DIRECTORY_PICTURES), name)
                file.copyTo(target, overwrite = true)
                true
            }
        }.getOrDefault(false)
    }

    private fun compressImage(source: Uri): ByteArray {
        var bitmap = decode(source)
        var quality = 82
        while (true) {
            val out = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.JPEG, quality, out)
            val bytes = out.toByteArray()
            if (bytes.size <= TARGET_IMAGE_BYTES) return bytes
            if (quality > 50) {
                quality -= 10
            } else {
                bitmap = Bitmap.createScaledBitmap(bitmap, (bitmap.width * 0.75f).toInt(), (bitmap.height * 0.75f).toInt(), true)
                if (bitmap.width < 200) return bytes.takeIf { it.size <= MAX_BYTES } ?: error("Imagen demasiado grande")
            }
        }
    }

    private fun decode(source: Uri): Bitmap {
        val resolver = appContext.contentResolver
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            ImageDecoder.decodeBitmap(ImageDecoder.createSource(resolver, source)) { decoder, info, _ ->
                val size = info.size
                val scale = MAX_SIDE.toFloat() / maxOf(size.width, size.height)
                if (scale < 1f) decoder.setTargetSize((size.width * scale).toInt(), (size.height * scale).toInt())
                decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
            }
        } else {
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            resolver.openInputStream(source)?.use { BitmapFactory.decodeStream(it, null, bounds) }
            var sample = 1
            while (maxOf(bounds.outWidth, bounds.outHeight) / (sample * 2) >= MAX_SIDE) sample *= 2
            val opts = BitmapFactory.Options().apply { inSampleSize = sample }
            resolver.openInputStream(source)?.use { BitmapFactory.decodeStream(it, null, opts) }
                ?: error("No se pudo leer la imagen")
        }
    }

    companion object {
        /** Límite de un documento de Firestore (1 MiB) con margen. */
        const val MAX_BYTES = 900 * 1024
        private const val TARGET_IMAGE_BYTES = 450 * 1024
        private const val MAX_SIDE = 1280
        private const val AVATAR_SIDE = 512
    }
}
