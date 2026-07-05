package com.ascend.lifeos.data

import android.content.Context
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Versioned local backups into a user-chosen SAF folder. Runs opportunistically
 * on app start (max once per 6 days), keeps the last 8 files. A pm clear or a
 * new phone must never cost data again.
 */
object Backup {
    private const val PREF = "backup"
    private const val KEY_URI = "tree_uri"
    private const val KEY_LAST = "last_ms"
    private const val KEEP = 8

    private fun prefs(ctx: Context) = ctx.getSharedPreferences(PREF, Context.MODE_PRIVATE)

    fun folder(ctx: Context): Uri? = prefs(ctx).getString(KEY_URI, null)?.let(Uri::parse)

    fun setFolder(ctx: Context, uri: Uri) {
        runCatching {
            ctx.contentResolver.takePersistableUriPermission(
                uri,
                android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION or
                    android.content.Intent.FLAG_GRANT_WRITE_URI_PERMISSION,
            )
        }
        prefs(ctx).edit().putString(KEY_URI, uri.toString()).apply()
    }

    fun lastBackupMs(ctx: Context): Long = prefs(ctx).getLong(KEY_LAST, 0L)

    /** Write a backup if a folder is set and the last one is ≥6 days old. */
    fun maybeRun(ctx: Context) {
        val uri = folder(ctx) ?: return
        if (System.currentTimeMillis() - lastBackupMs(ctx) < 6L * 86_400_000) return
        runNow(ctx, uri)
    }

    fun runNow(ctx: Context, uri: Uri? = folder(ctx)): Boolean {
        uri ?: return false
        return runCatching {
            val dir = DocumentFile.fromTreeUri(ctx, uri) ?: return false
            val stamp = SimpleDateFormat("yyyy-MM-dd-HHmm", Locale.US).format(Date())
            val file = dir.createFile("application/json", "jarvis-backup-$stamp.json") ?: return false
            ctx.contentResolver.openOutputStream(file.uri)?.use { out ->
                out.write(Repo.exportJson().toByteArray(Charsets.UTF_8))
            }
            // rotate: keep the newest KEEP backups
            dir.listFiles()
                .filter { it.name?.startsWith("jarvis-backup-") == true }
                .sortedByDescending { it.name }
                .drop(KEEP)
                .forEach { runCatching { it.delete() } }
            prefs(ctx).edit().putLong(KEY_LAST, System.currentTimeMillis()).apply()
            true
        }.getOrDefault(false)
    }

    /** Restore the newest backup found in the folder. */
    fun restoreLatest(ctx: Context): Boolean {
        val uri = folder(ctx) ?: return false
        return runCatching {
            val dir = DocumentFile.fromTreeUri(ctx, uri) ?: return false
            val latest = dir.listFiles()
                .filter { it.name?.startsWith("jarvis-backup-") == true }
                .maxByOrNull { it.name ?: "" } ?: return false
            val json = ctx.contentResolver.openInputStream(latest.uri)?.use {
                it.readBytes().toString(Charsets.UTF_8)
            } ?: return false
            Repo.importJson(json)
        }.getOrDefault(false)
    }
}
