package com.ascend.lifeos.ui.hud

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ascend.lifeos.core.prevKey
import com.ascend.lifeos.core.todayKey
import com.ascend.lifeos.data.Prefs
import com.ascend.lifeos.data.Repo
import com.ascend.lifeos.ui.kit.TickerNumber
import com.ascend.lifeos.ui.theme.*
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import kotlin.math.abs
import kotlin.math.roundToInt

// ─── Der Wochen-Rückblick (FUEL-Masterplan Kap. 43) ──────────────────────────
// Sonntagabend & montags: die abgeschlossene Woche als ehrliches Zeugnis —
// Fuel-Score aus Konstanz (40), Protein (30) und kcal-Präzision (30).

private class WeekDay(val kcal: Int, val protein: Int, val logged: Boolean)

@Composable
fun WeeklyFuelReview(isToday: Boolean) {
    if (!isToday) return
    val now = LocalDate.now()
    val show = now.dayOfWeek == DayOfWeek.MONDAY ||
        (now.dayOfWeek == DayOfWeek.SUNDAY && LocalTime.now().hour >= 17)
    if (!show) return

    val p = Repo.profile()
    val days = remember(Repo.data.days) {
        var k = prevKey(todayKey())
        val out = ArrayList<WeekDay>(7)
        repeat(7) {
            val d = Repo.dayFor(k)
            val t = d?.let { Repo.nutritionTotals(it) }
            out.add(WeekDay(t?.kcal ?: 0, t?.protein ?: 0, d?.meals?.isNotEmpty() == true))
            k = prevKey(k)
        }
        out.reversed()
    }
    val logged = days.count { it.logged }
    if (logged == 0) return

    val avgK = days.filter { it.logged }.map { it.kcal }.average().roundToInt()
    val avgP = days.filter { it.logged }.map { it.protein }.average().roundToInt()
    val ctx = androidx.compose.ui.platform.LocalContext.current
    val protPct = Prefs.int(ctx, Prefs.PROTEIN_HIT_PCT, 90) / 100.0
    val kcalPct = Prefs.int(ctx, Prefs.KCAL_ADHERENCE_PCT, 10) / 100.0
    val protHit = days.count { it.logged && it.protein >= p.proteinGoal * protPct }
    val kcalHit = days.count { it.logged && p.kcalGoal > 0 && abs(it.kcal - p.kcalGoal) <= p.kcalGoal * kcalPct }
    val score = (40.0 * logged / 7 + 30.0 * protHit / logged + 30.0 * kcalHit / logged).roundToInt()
    val scoreColor = when { score >= 80 -> Good; score >= 55 -> Amber; else -> Warn }

    Spacer(Modifier.height(14.dp))
    GlassPanel(Modifier.fillMaxWidth()) {
        Column(Modifier.fillMaxWidth().padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("WEEKLY REVIEW", color = TextDim, fontSize = FS.s10, fontFamily = Display, fontWeight = FontWeight.Bold, letterSpacing = 2.sp, modifier = Modifier.weight(1f))
                TickerNumber(score, 22, scoreColor)
                Text(" / 100", color = TextDim, fontSize = FS.s11, fontFamily = Body, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.height(12.dp))
            // 7 Balken — grün: ±10 % ums Ziel, amber: drunter, warn: drüber
            Row(Modifier.fillMaxWidth().height(56.dp), horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.Bottom) {
                days.forEach { d ->
                    val frac = if (p.kcalGoal > 0) (d.kcal.toFloat() / (p.kcalGoal * 1.3f)).coerceIn(0.06f, 1f) else 0.06f
                    val c = when {
                        !d.logged -> Ivory.copy(alpha = 0.06f)
                        abs(d.kcal - p.kcalGoal) <= p.kcalGoal * kcalPct -> Good
                        d.kcal > p.kcalGoal -> Warn.copy(alpha = 0.8f)
                        else -> Amber.copy(alpha = 0.8f)
                    }
                    Box(
                        Modifier.weight(1f).height((56 * frac).dp)
                            .clip(RoundedCornerShape(topStart = 5.dp, topEnd = 5.dp)).background(c),
                    )
                }
            }
            Spacer(Modifier.height(10.dp))
            Text(
                "Ø $avgK kcal · Ø $avgP g protein · $logged/7 days logged",
                color = TextPrimary, fontSize = FS.s12_5, fontFamily = Body, fontWeight = FontWeight.Bold,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                when {
                    score >= 80 -> "Razor-sharp week — consistency is your superpower."
                    score >= 55 -> "Solid base. Protein came up short on ${(logged - protHit).coerceAtLeast(0)} logged days — the gap filler helps in the evening."
                    else -> "Reset week: today counts, not yesterday. One logged day is a good day."
                },
                color = TextMuted, fontSize = FS.s11_5, fontFamily = Body, lineHeight = FS.s16,
            )
        }
    }
}
