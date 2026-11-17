package com.barcraft.app.utils

import com.barcraft.app.R
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
            minutes < 1 -> Txt.get(R.string.time_now)
            minutes < 60 -> Txt.plural(R.plurals.time_minutes_ago, minutes.toInt())
            hours < 24 -> Txt.plural(R.plurals.time_hours_ago, hours.toInt())
            days < 2 -> Txt.get(R.string.time_yesterday)
            days < 7 -> Txt.plural(R.plurals.time_days_ago, days.toInt())
            else -> SimpleDateFormat("d MMM", spanish).format(Date(millis))
        }
    }

    fun time(millis: Long): String = SimpleDateFormat("HH:mm", spanish).format(Date(millis))

    fun date(millis: Long): String = SimpleDateFormat("d MMM yyyy", spanish).format(Date(millis))
}
