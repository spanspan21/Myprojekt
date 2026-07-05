package com.ascend.lifeos.data

import android.app.Application
import android.content.Context
import android.os.Build
import android.util.Log
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// ─── Crash black box ─────────────────────────────────────────────────────────
// Sideloaded app, no Play Console: when JARVIS dies on the device, this file is
// the only witness. Writes the stack trace to filesDir/crashlog before handing
// the crash back to the system; Settings → Diagnostics shares the latest report.

object CrashLog {
    private const val DIR = "crashlog"
    private const val KEEP = 5

    fun install(app: Application) {
        val system = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, e ->
            runCatching { write(app, thread, e) }
            system?.uncaughtException(thread, e) ?: Runtime.getRuntime().exit(2)
        }
    }

    fun latestStamp(ctx: Context): String? = newest(ctx)?.let {
        SimpleDateFormat("dd.MM HH:mm", Locale.getDefault()).format(Date(it.lastModified()))
    }

    fun read(ctx: Context): String? = newest(ctx)?.takeIf { it.exists() }?.readText()

    fun clear(ctx: Context) {
        dir(ctx).listFiles()?.forEach { it.delete() }
    }

    private fun dir(ctx: Context) = File(ctx.filesDir, DIR)

    // file names sort chronologically (yyyy-MM-dd_HH-mm-ss), so max = newest
    private fun newest(ctx: Context): File? =
        dir(ctx).listFiles()?.filter { it.name.startsWith("crash_") }?.maxByOrNull { it.name }

    private fun write(ctx: Context, thread: Thread, e: Throwable) {
        val d = dir(ctx).apply { mkdirs() }
        val ts = SimpleDateFormat("yyyy-MM-dd_HH-mm-ss", Locale.US).format(Date())
        val installed = runCatching {
            val pi = ctx.packageManager.getPackageInfo(ctx.packageName, 0)
            SimpleDateFormat("dd.MM HH:mm", Locale.US).format(Date(pi.lastUpdateTime))
        }.getOrDefault("?")
        File(d, "crash_$ts.txt").writeText(
            buildString {
                appendLine("JARVIS crash report")
                appendLine("Time:    ${ts.replace('_', ' ')}")
                appendLine("Thread:  ${thread.name}")
                appendLine("Build:   installed $installed")
                appendLine("Device:  ${Build.MANUFACTURER} ${Build.MODEL} · Android ${Build.VERSION.RELEASE}")
                appendLine()
                append(Log.getStackTraceString(e))
            },
        )
        d.listFiles()?.sortedByDescending { it.name }?.drop(KEEP)?.forEach { it.delete() }
    }
}
