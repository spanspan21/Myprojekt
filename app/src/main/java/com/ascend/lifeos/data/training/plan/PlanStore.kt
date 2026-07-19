package com.ascend.lifeos.data.training.plan

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import org.json.JSONArray

/**
 * User-built plan templates — prefs-JSON store in the established rev pattern
 * (small data, no query needs; a template is 2–8 KB JSON). PlanStore.rev is
 * registered in the JarvisApp snapshot warmup (K1 invariant — the
 * WarmupCompletenessTest enforces it mechanically).
 */
object PlanStore {

    private const val PREF = "plan_templates"

    var rev by mutableIntStateOf(0)
        private set

    private fun touch() { rev++ }

    private fun prefs(ctx: Context) = ctx.applicationContext.getSharedPreferences(PREF, Context.MODE_PRIVATE)

    fun all(ctx: Context): List<PlanTemplate> = runCatching {
        val arr = JSONArray(prefs(ctx).getString("templates", "[]") ?: "[]")
        (0 until arr.length()).mapNotNull { PlanCodec.decode(arr.getJSONObject(it)) }
    }.getOrDefault(emptyList())

    fun byId(ctx: Context, id: String): PlanTemplate? = all(ctx).firstOrNull { it.id == id }

    @Synchronized
    fun save(ctx: Context, t: PlanTemplate) {
        val now = System.currentTimeMillis()
        val stamped = t.copy(
            createdAt = if (t.createdAt == 0L) now else t.createdAt,
            updatedAt = now,
        )
        val rest = all(ctx).filterNot { it.id == t.id }
        write(ctx, rest + stamped)
    }

    @Synchronized
    fun delete(ctx: Context, id: String) = write(ctx, all(ctx).filterNot { it.id == id })

    private fun write(ctx: Context, list: List<PlanTemplate>) {
        val arr = JSONArray()
        list.forEach { arr.put(PlanCodec.encode(it)) }
        prefs(ctx).edit().putString("templates", arr.toString()).apply()
        touch()
    }
}
