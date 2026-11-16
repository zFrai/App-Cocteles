package com.barcraft.app.utils

import android.content.Context
import android.media.MediaRecorder
import android.os.Build
import java.io.File

/** Graba notas de voz en AAC a baja calidad (≈ 4 KB por segundo) para que quepan en el servidor. */
class AudioRecorder(private val context: Context) {

    private var recorder: MediaRecorder? = null
    private var file: File? = null
    private var startedAt = 0L

    val isRecording: Boolean get() = recorder != null

    fun start(): Boolean = runCatching {
        val out = File(context.cacheDir, "voice_${System.currentTimeMillis()}.m4a")
        val r = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) MediaRecorder(context) else @Suppress("DEPRECATION") MediaRecorder()
        r.setAudioSource(MediaRecorder.AudioSource.MIC)
        r.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
        r.setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
        r.setAudioEncodingBitRate(32_000)
        r.setAudioSamplingRate(22_050)
        r.setAudioChannels(1)
        r.setMaxDuration(MAX_DURATION_MS)
        r.setOutputFile(out.absolutePath)
        r.prepare()
        r.start()
        recorder = r
        file = out
        startedAt = System.currentTimeMillis()
        true
    }.getOrElse { release(); false }

    /** Para la grabación y devuelve el archivo y su duración, o null si fue demasiado corta. */
    fun stop(): Pair<File, Long>? {
        val r = recorder ?: return null
        val duration = System.currentTimeMillis() - startedAt
        val ok = runCatching { r.stop() }.isSuccess
        release()
        val f = file
        return if (ok && f != null && duration >= 700) f to duration.coerceAtMost(MAX_DURATION_MS.toLong()) else {
            f?.delete(); null
        }
    }

    fun cancel() {
        runCatching { recorder?.stop() }
        release()
        file?.delete()
    }

    fun elapsedMs(): Long = if (isRecording) System.currentTimeMillis() - startedAt else 0L

    private fun release() {
        runCatching { recorder?.release() }
        recorder = null
    }

    companion object {
        /** 3 minutos a 32 kbps ≈ 720 KB, por debajo del límite de 900 KB. */
        const val MAX_DURATION_MS = 180_000
    }
}
