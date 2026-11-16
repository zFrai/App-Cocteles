package com.barcraft.app.utils

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object DateUtils {
    private val spanish = Locale("es", "ES")

    /** "ahora", "hace 5 min", "hace 3 h", "ayer", "12 mar". */
    fun relative(millis: Long, now: Long = System.currentTimeMillis()): String {
        val diff = (now - millis).coerceAtLeast(0)
        val minutes = diff / 60_000
        val hours = minutes / 60
        val days = hours / 24
        return when {
            minutes < 1 -> "ahora"
            minutes < 60 -> "hace $minutes min"
            hours < 24 -> "hace $hours h"
            days < 2 -> "ayer"
            days < 7 -> "hace $days días"
            else -> SimpleDateFormat("d MMM", spanish).format(Date(millis))
        }
    }

    fun time(millis: Long): String = SimpleDateFormat("HH:mm", spanish).format(Date(millis))

    fun date(millis: Long): String = SimpleDateFormat("d MMM yyyy", spanish).format(Date(millis))
}
