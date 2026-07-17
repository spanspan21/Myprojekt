package com.ascend.lifeos.data

import android.content.ContentUris
import android.content.Context
import android.provider.CalendarContract
import androidx.core.content.ContextCompat

data class CalEvent(val title: String, val start: Long, val end: Long, val allDay: Boolean)

/** Reads today's events from the device calendar (read-only) via CalendarContract. */
object CalendarSync {
    const val PERMISSION = android.Manifest.permission.READ_CALENDAR

    fun granted(ctx: Context): Boolean =
        ContextCompat.checkSelfPermission(ctx, PERMISSION) == android.content.pm.PackageManager.PERMISSION_GRANTED

    fun readToday(ctx: Context): List<CalEvent> = readDay(ctx, com.ascend.lifeos.core.todayDate())

    fun readDay(ctx: Context, day: java.time.LocalDate): List<CalEvent> {
        val zone = java.time.ZoneId.systemDefault()
        val start = day.atStartOfDay(zone).toInstant().toEpochMilli()
        val end = day.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli()
        return readRange(ctx, start, end)
    }

    fun readRange(ctx: Context, start: Long, end: Long): List<CalEvent> {
        val uri = CalendarContract.Instances.CONTENT_URI.buildUpon().let {
            ContentUris.appendId(it, start); ContentUris.appendId(it, end); it.build()
        }
        val projection = arrayOf(
            CalendarContract.Instances.TITLE,
            CalendarContract.Instances.BEGIN,
            CalendarContract.Instances.END,
            CalendarContract.Instances.ALL_DAY,
        )
        val out = ArrayList<CalEvent>()
        runCatching {
            ctx.contentResolver.query(uri, projection, null, null, "${CalendarContract.Instances.BEGIN} ASC")?.use { c ->
                while (c.moveToNext()) {
                    val title = c.getString(0)?.takeIf { it.isNotBlank() } ?: "(untitled)"
                    out.add(CalEvent(title, c.getLong(1), c.getLong(2), c.getInt(3) == 1))
                }
            }
        }
        return out
    }
}
