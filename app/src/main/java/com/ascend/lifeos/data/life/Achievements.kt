package com.ascend.lifeos.data.life

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import com.ascend.lifeos.data.Repo
import com.ascend.lifeos.data.finance.FinanceStore
import com.ascend.lifeos.data.training.TrainingDatabase
import kotlinx.coroutines.flow.first
import org.json.JSONArray
import org.json.JSONObject
import java.util.Locale
import kotlin.math.abs

// ─── Life changelog ──────────────────────────────────────────────────────────
// Automatic milestone detection over stores that already exist — training PRs,
// day-streaks, bodyweight marks and savings goals. Nothing is entered by hand:
// [scan] derives milestones and appends the new ones. Every milestone carries a
// stable natural id, so re-scanning is idempotent and never duplicates.

/** One detected milestone. [module] ∈ train | streak | money | body | school. */
data class Achievement(
    val id: String,
    val ts: Long,
    val module: String,
    val title: String,
    val detail: String,
)

object Achievements {
    private const val PREF = "achievements"
    private const val MAX = 300
    private val STREAK_MARKS = intArrayOf(7, 30, 60, 100, 180, 365)
    private const val WEIGHT_STEP_KG = 2.5

    /** Bump-on-write revision — read it in composition to subscribe to changes. */
    var rev by mutableIntStateOf(0)
        private set

    private fun touch() { rev++ }

    private fun prefs(ctx: Context): SharedPreferences =
        ctx.applicationContext.getSharedPreferences(PREF, Context.MODE_PRIVATE)

    private fun Achievement.toJson() = JSONObject()
        .put("id", id).put("ts", ts).put("module", module)
        .put("title", title).put("detail", detail)

    private fun from(o: JSONObject) = Achievement(
        id = o.optString("id"),
        ts = o.optLong("ts"),
        module = o.optString("module", "train"),
        title = o.optString("title"),
        detail = o.optString("detail", ""),
    )

    /** All milestones, newest first (store capped at [MAX]). */
    fun list(ctx: Context): List<Achievement> {
        val arr = runCatching { JSONArray(prefs(ctx).getString("entries", "[]") ?: "[]") }
            .getOrDefault(JSONArray())
        val out = ArrayList<Achievement>(arr.length())
        for (i in 0 until arr.length()) out.add(from(arr.getJSONObject(i)))
        return out.sortedByDescending { it.ts }.take(MAX)
    }

    /** Appends once per natural [id]; returns true only when the milestone is new. */
    private fun add(
        ctx: Context,
        id: String,
        module: String,
        title: String,
        detail: String,
        ts: Long = System.currentTimeMillis(),
    ): Boolean {
        if (id.isBlank()) return false
        val cur = list(ctx)
        if (cur.any { it.id == id }) return false
        val arr = JSONArray()
        (cur + Achievement(id, ts, module, title, detail))
            .sortedByDescending { it.ts }.take(MAX)
            .forEach { arr.put(it.toJson()) }
        prefs(ctx).edit().putString("entries", arr.toString()).apply()
        touch()
        return true
    }

    /** "82.5" / "80" — trims trailing .0 so ids stay stable and titles terse. */
    private fun num(v: Double): String =
        if (v % 1.0 == 0.0) "${v.toLong()}" else String.format(Locale.US, "%.1f", v)

    /**
     * Runs every detector and appends whatever is new. Each detector is wrapped
     * in [runCatching] so one broken store never kills the whole scan.
     * @return the number of NEW achievements added.
     */
    suspend fun scan(ctx: Context): Int {
        var added = 0

        // Training PRs — the newest 50 from the Room store. The PR type is part
        // of the id: max-reps 15 and max-weight 15 are different milestones.
        runCatching {
            for (pr in TrainingDatabase.get(ctx).dao().recentPrs(50).first()) {
                val v = num(pr.value.toDouble())
                val type = pr.type.name.lowercase(Locale.US)
                val newHit = add(
                    ctx, "pr_${pr.exerciseId}_${type}_$v", "train",
                    "${pr.exerciseName} PR", "${type.replace('_', ' ')}: $v", ts = pr.date,
                )
                if (newHit) added++
            }
        }

        // Day-streak marks — current or longest, whichever reached further.
        runCatching {
            val p = Repo.profile()
            val reached = maxOf(p.streak, p.longest)
            for (n in STREAK_MARKS) {
                if (reached < n) break
                if (add(ctx, "streak_$n", "streak", "$n-day streak", "Kept the chain for $n days")) added++
            }
        }

        // Bodyweight — every full 2.5 kg moved from the FIRST logged weight
        // toward the most recent one, direction-agnostic.
        runCatching {
            val log = Repo.weightLog()
            val first = log.firstOrNull() ?: return@runCatching
            val last = log.last()
            val dir = if (last.kg >= first.kg) 1.0 else -1.0
            val steps = ((abs(last.kg - first.kg) + 1e-9) / WEIGHT_STEP_KG).toInt()
            for (k in 1..steps) {
                val mark = first.kg + dir * WEIGHT_STEP_KG * k
                val hitTs = log.firstOrNull {
                    if (dir > 0) it.kg >= mark - 1e-9 else it.kg <= mark + 1e-9
                }?.ts ?: System.currentTimeMillis()
                val newHit = add(
                    ctx, "weight_${num(mark)}", "body",
                    "Weight ${num(mark)} kg", "Moved ${num(WEIGHT_STEP_KG * k)} kg since first log (${num(first.kg)} kg)",
                    ts = hitTs,
                )
                if (newHit) added++
            }
        }

        // Savings goals reached. Finance v2 goals include the migrated legacy
        // LifeStores goal; the direct LifeStores read covers a not-yet-migrated
        // one — the natural key dedupes the overlap.
        runCatching {
            val goals = runCatching {
                FinanceStore.saveGoals(ctx).map { Triple(it.title, it.savedCents, it.targetCents) }
            }.getOrDefault(emptyList()) + listOfNotNull(LifeStores.savingsGoal(ctx))
            for ((title, saved, target) in goals) {
                if (title.isBlank() || target <= 0 || saved < target) continue
                val key = title.trim().lowercase(Locale.US).replace(Regex("\\s+"), "_")
                val newHit = add(
                    ctx, "save_$key", "money",
                    "${title.trim()} saved", "Goal reached: ${String.format(Locale.ENGLISH, "%.2f", target / 100.0)} €",
                )
                if (newHit) added++
            }
        }

        return added
    }
}
