package com.ascend.lifeos.data

import android.content.Context
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import com.ascend.lifeos.data.calendar.CalendarDatabase
import com.ascend.lifeos.data.masterplan.MasterPlanDatabase
import com.ascend.lifeos.data.training.TrainingDatabase
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

/**
 * Versioned FULL backups (v2): one zip per run containing every SharedPreferences
 * store and all Room databases — a pm clear or a new phone must never cost data.
 * Runs opportunistically on app start (max once per 6 days, off the main thread),
 * keeps the last 8 archives. Legacy jarvis-backup-*.json (Repo-only) still restores.
 */
object Backup {
    private const val PREF = "backup"
    private const val KEY_URI = "tree_uri"
    private const val KEY_LAST = "last_ms"
    private const val KEEP = 8
    private val DBS = listOf("ascend_training.db", "jarvis_calendar.db", "ascend_masterplan.db")

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
        // SAF enumeration + zip writing must never block the cold start.
        Thread { runCatching { runNow(ctx.applicationContext, uri) } }.start()
    }

    fun runNow(ctx: Context, uri: Uri? = folder(ctx)): Boolean {
        uri ?: return false
        return runCatching {
            Repo.flush()
            checkpointDbs(ctx)
            val dir = DocumentFile.fromTreeUri(ctx, uri) ?: return false
            val stamp = SimpleDateFormat("yyyy-MM-dd-HHmm", Locale.US).format(Date())
            val file = dir.createFile("application/zip", "jarvis-backup-$stamp.zip") ?: return false
            ctx.contentResolver.openOutputStream(file.uri)?.use { raw ->
                ZipOutputStream(raw).use { zip ->
                    zip.putNextEntry(ZipEntry("manifest.json"))
                    zip.write(
                        JSONObject().put("version", 2)
                            .put("createdMs", System.currentTimeMillis()).toString().toByteArray()
                    )
                    zip.closeEntry()
                    prefsNames(ctx).forEach { name ->
                        zip.putNextEntry(ZipEntry("prefs/$name.json"))
                        zip.write(prefsToJson(ctx, name).toString().toByteArray())
                        zip.closeEntry()
                    }
                    DBS.forEach { db ->
                        val f = ctx.getDatabasePath(db)
                        if (f.exists()) {
                            zip.putNextEntry(ZipEntry("db/$db"))
                            f.inputStream().use { it.copyTo(zip) }
                            zip.closeEntry()
                        }
                    }
                }
            }
            // rotate: keep the newest KEEP backups (zip and legacy json alike)
            dir.listFiles()
                .filter { it.name?.startsWith("jarvis-backup-") == true }
                .sortedByDescending { it.name }
                .drop(KEEP)
                .forEach { runCatching { it.delete() } }
            prefs(ctx).edit().putLong(KEY_LAST, System.currentTimeMillis()).apply()
            true
        }.getOrDefault(false)
    }

    /** Flush the WAL into the main db file so a plain file copy is consistent. */
    private fun checkpointDbs(ctx: Context) {
        runCatching {
            TrainingDatabase.get(ctx).openHelper.writableDatabase
                .query("PRAGMA wal_checkpoint(TRUNCATE)").use { it.moveToFirst() }
        }
        runCatching {
            CalendarDatabase.get(ctx).openHelper.writableDatabase
                .query("PRAGMA wal_checkpoint(TRUNCATE)").use { it.moveToFirst() }
        }
        runCatching {
            MasterPlanDatabase.get(ctx).openHelper.writableDatabase
                .query("PRAGMA wal_checkpoint(TRUNCATE)").use { it.moveToFirst() }
        }
    }

    /** Every SharedPreferences file that exists on disk right now. */
    private fun prefsNames(ctx: Context): List<String> =
        File(ctx.applicationInfo.dataDir, "shared_prefs")
            .listFiles { f -> f.name.endsWith(".xml") }
            ?.map { it.name.removeSuffix(".xml") }
            ?: emptyList()

    private fun prefsToJson(ctx: Context, name: String): JSONObject {
        val o = JSONObject()
        ctx.getSharedPreferences(name, Context.MODE_PRIVATE).all.forEach { (k, v) ->
            val e = JSONObject()
            when (v) {
                is String -> e.put("t", "s").put("v", v)
                is Boolean -> e.put("t", "b").put("v", v)
                is Int -> e.put("t", "i").put("v", v)
                is Long -> e.put("t", "l").put("v", v)
                is Float -> e.put("t", "f").put("v", v.toDouble())
                is Set<*> -> e.put("t", "ss").put("v", JSONArray(v.mapNotNull { s -> s?.toString() }))
                else -> return@forEach
            }
            o.put(k, e)
        }
        return o
    }

    /**
     * Restore the newest backup in the folder. Full (zip) restores swap prefs and
     * database files — the process MUST be restarted afterwards (`needsRestart`).
     */
    data class RestoreResult(val ok: Boolean, val needsRestart: Boolean)

    fun restoreLatest(ctx: Context): RestoreResult {
        val uri = folder(ctx) ?: return RestoreResult(false, false)
        return runCatching {
            val dir = DocumentFile.fromTreeUri(ctx, uri) ?: return RestoreResult(false, false)
            val latest = dir.listFiles()
                .filter { it.name?.startsWith("jarvis-backup-") == true }
                .maxByOrNull { it.name ?: "" } ?: return RestoreResult(false, false)
            if (latest.name?.endsWith(".zip") == true) {
                RestoreResult(restoreZip(ctx, latest.uri), needsRestart = true)
            } else {
                val json = ctx.contentResolver.openInputStream(latest.uri)?.use {
                    it.readBytes().toString(Charsets.UTF_8)
                } ?: return RestoreResult(false, false)
                RestoreResult(Repo.importJson(json), needsRestart = false)
            }
        }.getOrDefault(RestoreResult(false, false))
    }

    private fun restoreZip(ctx: Context, file: Uri): Boolean {
        val entries = HashMap<String, ByteArray>()
        ctx.contentResolver.openInputStream(file)?.use { raw ->
            ZipInputStream(raw).use { zip ->
                var e = zip.nextEntry
                while (e != null) {
                    if (!e.isDirectory) entries[e.name] = zip.readBytes()
                    zip.closeEntry()
                    e = zip.nextEntry
                }
            }
        }
        if (entries.none { it.key.startsWith("prefs/") || it.key.startsWith("db/") }) return false

        // 1) Swap database files while no connection is open.
        TrainingDatabase.close()
        CalendarDatabase.close()
        MasterPlanDatabase.close()
        DBS.forEach { db ->
            entries["db/$db"]?.let { bytes ->
                val f = ctx.getDatabasePath(db)
                f.parentFile?.mkdirs()
                File(f.path + "-wal").delete()
                File(f.path + "-shm").delete()
                f.writeBytes(bytes)
            }
        }
        // 2) Restore every prefs store except our own (folder uri is device-specific).
        entries.keys.filter { it.startsWith("prefs/") }.forEach { key ->
            val name = key.removePrefix("prefs/").removeSuffix(".json")
            if (name == PREF) return@forEach
            val o = runCatching { JSONObject(String(entries[key]!!)) }.getOrNull() ?: return@forEach
            val ed = ctx.getSharedPreferences(name, Context.MODE_PRIVATE).edit().clear()
            for (k in o.keys()) {
                val e = o.optJSONObject(k) ?: continue
                when (e.optString("t")) {
                    "s" -> ed.putString(k, e.optString("v"))
                    "b" -> ed.putBoolean(k, e.optBoolean("v"))
                    "i" -> ed.putInt(k, e.optInt("v"))
                    "l" -> ed.putLong(k, e.optLong("v"))
                    "f" -> ed.putFloat(k, e.optDouble("v").toFloat())
                    "ss" -> {
                        val a = e.optJSONArray("v") ?: JSONArray()
                        ed.putStringSet(k, (0 until a.length()).map { a.getString(it) }.toSet())
                    }
                }
            }
            ed.commit()
        }
        return true
    }
}
