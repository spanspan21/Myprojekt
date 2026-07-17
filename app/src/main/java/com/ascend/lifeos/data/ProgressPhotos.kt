package com.ascend.lifeos.data

import android.content.Context
import android.net.Uri
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File

@Serializable
data class ProgressPhoto(
    val id: String,
    val ts: Long,
    val dayKey: String,
    val pose: String,
    val note: String = "",
)

object ProgressPhotos {
    private val json = Json { ignoreUnknownKeys = true }
    private const val DIR = "progress_photos"
    private const val META = "progress_photos_meta.json"
    val POSES = listOf("Front", "Side", "Back", "Flex")

    private fun dir(ctx: Context) = File(ctx.filesDir, DIR).also { it.mkdirs() }
    private fun metaFile(ctx: Context) = File(ctx.filesDir, META)

    fun list(ctx: Context): List<ProgressPhoto> {
        val f = metaFile(ctx)
        if (!f.exists()) return emptyList()
        return runCatching { json.decodeFromString<List<ProgressPhoto>>(f.readText()) }
            .onFailure { android.util.Log.e("ProgressPhotos", "Metadata corrupt", it) }
            .getOrDefault(emptyList())
    }

    fun photoFile(ctx: Context, id: String): File = File(dir(ctx), "$id.jpg")

    fun save(ctx: Context, uri: Uri, pose: String, note: String): ProgressPhoto? {
        val id = Repo.newId("pp")
        val dayKey = com.ascend.lifeos.core.todayKey()
        val dest = photoFile(ctx, id)
        runCatching {
            ctx.contentResolver.openInputStream(uri)?.use { input ->
                dest.outputStream().use { out -> input.copyTo(out) }
            }
        }.onFailure { return null }

        val entry = ProgressPhoto(id, System.currentTimeMillis(), dayKey, pose, note)
        val entries = list(ctx) + entry
        writeMeta(ctx, entries) ?: run { dest.delete(); return null }
        return entry
    }

    fun delete(ctx: Context, id: String) {
        val entries = list(ctx).filter { it.id != id }
        writeMeta(ctx, entries) ?: return
        photoFile(ctx, id).delete()
    }

    fun byDay(ctx: Context): Map<String, List<ProgressPhoto>> =
        list(ctx).groupBy { it.dayKey }.toSortedMap(compareByDescending { it })

    fun latestByPose(ctx: Context): Map<String, ProgressPhoto> =
        list(ctx).groupBy { it.pose }.mapValues { (_, v) -> v.maxBy { it.ts } }

    private fun writeMeta(ctx: Context, entries: List<ProgressPhoto>): Unit? = runCatching {
        val tmp = java.io.File(metaFile(ctx).parent, "progress_photos_meta.tmp")
        tmp.writeText(json.encodeToString(entries))
        tmp.renameTo(metaFile(ctx))
        Unit
    }.getOrNull()

    fun count(ctx: Context): Int = list(ctx).size
}
