package com.ascend.lifeos.data

import android.content.ContentUris
import android.content.Context
import android.provider.CalendarContract
import androidx.core.content.ContextCompat
import java.util.Calendar

data class CalEvent(val title: String, val start: Long, val end: Long, val allDay: Boolean)

/** Reads today's events from the device calendar (read-only) via CalendarContract. */
object CalendarSync {
    const val PERMISSION = android.Manifest.permission.READ_CALENDAR

    fun granted(ctx: Context): Boolean =
        ContextCompat.checkSelfPermission(ctx, PERMISSION) == android.content.pm.PackageManager.PERMISSION_GRANTED

    fun readToday(ctx: Context): List<CalEvent> {
        val start = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
        }.timeInMillis
        val end = start + 24L * 60 * 60 * 1000

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
                    val title = c.getString(0)?.takeIf { it.isNotBlank() } ?: "(ohne Titel)"
                    out.add(CalEvent(title, c.getLong(1), c.getLong(2), c.getInt(3) == 1))
                }
            }
        }
        return out
    }
}
