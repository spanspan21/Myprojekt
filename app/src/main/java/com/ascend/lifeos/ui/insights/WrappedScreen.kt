package com.ascend.lifeos.ui.insights

import android.content.Context
import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ascend.lifeos.core.todayKey
import com.ascend.lifeos.data.Repo
import com.ascend.lifeos.data.training.TrainingDatabase
import com.ascend.lifeos.ui.theme.Body
import com.ascend.lifeos.ui.theme.Display
import com.ascend.lifeos.ui.theme.Mod
import com.ascend.lifeos.ui.theme.TextDim
import com.ascend.lifeos.ui.theme.TextMuted
import com.ascend.lifeos.ui.theme.TextPrimary
import com.ascend.lifeos.ui.theme.metricStyle
import com.ascend.lifeos.wellbeing.WellbeingStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.withContext
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.Locale
import kotlin.random.Random

// ─── JARVIS Wrapped — the whole story, from real logs only ──────────────────

private data class Sub(val value: String, val label: String)

private data class WrapSlide(
    val tag: String,
    val title: String?,
    val huge: String,
    val hugeLabel: String,
    val subs: List<Sub>,
    val accent: Color,
    val footer: String? = null,
)

private data class WrapModel(val slides: List<WrapSlide>)

private fun dayKeyOf(epochMs: Long): String =
    todayKey(LocalDateTime.ofInstant(Instant.ofEpochMilli(epochMs), ZoneId.systemDefault()))

private fun num(v: Int): String = String.format(Locale.US, "%,d", v)

private fun hm(min: Int): String = "${min / 60}h ${(min % 60).toString().padStart(2, '0')}m"

private suspend fun buildWrap(ctx: Context): WrapModel {
    val dao = TrainingDatabase.get(ctx).dao()
    val body = Repo.data.bodyDays

    val sessions = runCatching { dao.sessionsSince(0L) }.getOrDefault(emptyList())
    val firstSessionKey = sessions.minByOrNull { it.session.startedAt }?.let { dayKeyOf(it.session.startedAt) }
    val firstKey = listOfNotNull(body.keys.minOrNull(), firstSessionKey).minOrNull()
    val today = todayKey()
    val days = if (firstKey == null) 0
    else (ChronoUnit.DAYS.between(LocalDate.parse(firstKey), LocalDate.parse(today)) + 1).toInt()

    if (days < 14) {
        return WrapModel(
            listOf(
                WrapSlide(
                    tag = "JARVIS WRAPPED",
                    title = "Come back after two weeks of data.",
                    huge = "$days", hugeLabel = "DAYS TRACKED SO FAR",
                    subs = listOf(Sub("14", "DAYS NEEDED")),
                    accent = Mod.Home,
                    footer = "Wrapped tells your story from real logs only — no filler, no demo numbers.",
                ),
            ),
        )
    }

    // ---- train ----
    val totalReps = runCatching { dao.totalRepsSince(0L) }.getOrDefault(0)
    val sessionCount = runCatching { dao.sessionCountSince(0L) }.getOrDefault(0)
    val prCount = runCatching { dao.recentPrs(500).firstOrNull()?.size ?: 0 }.getOrDefault(0)

    // ---- body ----
    val nights = body.values.count { (it.sleepMin ?: 0) > 0 }
    val sleepTotalMin = body.values.sumOf { it.sleepMin ?: 0 }
    val recVals = body.values.mapNotNull { d ->
        d.sleepMin?.takeIf { it > 0 }?.let { sm ->
            val perf = (sm / 480.0).coerceIn(0.0, 1.0)
            val rest = ((d.rem + d.deep).toDouble() / sm).coerceIn(0.0, 0.45) / 0.45
            (0.65 * perf + 0.35 * rest) * 100
        }
    }
    val avgRec = recVals.takeIf { it.isNotEmpty() }?.average()?.toInt()
    // best calendar week (Mon-anchored) with at least 4 recorded nights
    val byWeek = HashMap<LocalDate, MutableList<Int>>()
    body.forEach { (k, d) ->
        val sm = d.sleepMin ?: return@forEach
        if (sm <= 0) return@forEach
        val date = runCatching { LocalDate.parse(k) }.getOrNull() ?: return@forEach
        val monday = date.minusDays((date.dayOfWeek.value - 1).toLong())
        byWeek.getOrPut(monday) { mutableListOf() }.add(sm)
    }
    val bestWeek = byWeek.filterValues { it.size >= 4 }.maxByOrNull { it.value.average() }
    val bestWeekLabel = bestWeek?.key?.format(DateTimeFormatter.ofPattern("d MMM", Locale.ENGLISH))
    val bestWeekAvg = bestWeek?.value?.average()?.toInt()

    // ---- fuel ----
    val mealKcals = Repo.data.days.values.map { d -> d.meals.sumOf { it.kcal } }.filter { it > 0 }
    val kcalDays = mealKcals.size
    val avgKcal = mealKcals.takeIf { it.isNotEmpty() }?.average()?.toInt()
    val proteins = Repo.data.days.values.map { d -> d.meals.sumOf { it.protein } }.filter { it > 0 }
    val avgProtein = proteins.takeIf { it.isNotEmpty() }?.average()?.toInt()

    // ---- guard (WellbeingStore keeps a 60-day window — labelled honestly) ----
    val hist = WellbeingStore.history(ctx)
    val screenDays = hist.size
    val screenTotalMin = hist.values.sumOf { it.first }
    val screenAvg = if (screenDays > 0) screenTotalMin / screenDays else null
    val unlocksTotal = hist.values.sumOf { it.second }
    val reclaimedMin = WellbeingStore.interceptCount(ctx) * 9

    val p = Repo.data.profile
    val highlight = when {
        prCount > 0 -> "$prCount personal records broken. The bar keeps moving."
        totalReps >= 1000 -> "${num(totalReps)} reps in the bank. Strength is compound interest."
        bestWeekLabel != null && bestWeekAvg != null -> "Best sleep week: ${hm(bestWeekAvg)} a night around $bestWeekLabel."
        kcalDays >= 30 -> "$kcalDays days of honest fuel logging. The mirror doesn't lie."
        else -> "The flywheel is turning. Keep feeding it real days."
    }
    val firstLabel = LocalDate.parse(firstKey!!).format(DateTimeFormatter.ofPattern("d MMM yy", Locale.ENGLISH))

    val slides = listOf(
        WrapSlide(
            "JARVIS WRAPPED", "YOUR STORY SO FAR",
            num(days), "DAYS TRACKED",
            listOf(Sub(firstLabel, "FIRST LOG"), Sub(num(nights), "NIGHTS OF SLEEP")),
            Mod.Home, footer = "Swipe →",
        ),
        WrapSlide(
            "TRAIN", null,
            num(totalReps), "TOTAL REPS",
            listOf(Sub(num(sessionCount), "SESSIONS"), Sub("$prCount", "PERSONAL RECORDS")),
            Mod.Train,
        ),
        WrapSlide(
            "BODY", null,
            num(sleepTotalMin / 60), "HOURS SLEPT",
            listOf(
                Sub(avgRec?.toString() ?: "—", "Ø RECOVERY"),
                Sub(
                    bestWeekAvg?.let { hm(it) } ?: "—",
                    bestWeekLabel?.let { "BEST WEEK · ${it.uppercase(Locale.ENGLISH)}" } ?: "BEST WEEK",
                ),
            ),
            Mod.Body,
        ),
        WrapSlide(
            "FUEL", null,
            num(kcalDays), "DAYS LOGGED",
            listOf(
                Sub(avgKcal?.let { num(it) } ?: "—", "Ø KCAL"),
                Sub(avgProtein?.let { "${it}g" } ?: "—", "Ø PROTEIN"),
            ),
            Mod.Fuel,
        ),
        WrapSlide(
            "GUARD", null,
            if (screenDays > 0) num(screenTotalMin / 60) else "—", "SCREEN HOURS · 60 DAYS",
            listOf(
                Sub(screenAvg?.let { hm(it) } ?: "—", "Ø PER DAY"),
                Sub("≈${num(reclaimedMin)}m", "RECLAIMED"),
            ),
            Mod.Guard,
        ),
        WrapSlide(
            "KEEP CLIMBING", null,
            "${p.streak}", "DAY STREAK",
            listOf(Sub("${p.longest}", "LONGEST"), Sub(num(unlocksTotal), "UNLOCKS · 60D")),
            Mod.Home, footer = highlight,
        ),
    )
    return WrapModel(slides)
}

@Composable
fun WrappedScreen(onClose: () -> Unit) {
    val ctx = LocalContext.current
    val model by produceState<WrapModel?>(null) {
        value = withContext(Dispatchers.IO) { buildWrap(ctx) }
    }

    BackHandler { onClose() }

    Box(Modifier.fillMaxSize()) {
        val m = model
        if (m == null) {
            Text(
                "Rewinding your data…", color = TextDim, fontSize = 13.sp, fontFamily = Body,
                modifier = Modifier.align(Alignment.Center),
            )
        } else {
            val pagerState = rememberPagerState(pageCount = { m.slides.size })
            HorizontalPager(state = pagerState, modifier = Modifier.fillMaxSize()) { page ->
                SlidePage(m.slides[page], page)
            }
            if (m.slides.size > 1) {
                Row(
                    Modifier.align(Alignment.BottomCenter).navigationBarsPadding().padding(bottom = 26.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    repeat(m.slides.size) { i ->
                        val active = pagerState.currentPage == i
                        val w by animateDpAsState(if (active) 18.dp else 6.dp, label = "dot")
                        Box(
                            Modifier.height(6.dp).width(w).clip(CircleShape)
                                .background(
                                    if (active) m.slides[pagerState.currentPage].accent
                                    else Color.White.copy(alpha = 0.18f),
                                ),
                        )
                    }
                }
            }
        }
        CloseOrb(onClose)
    }
}

// ─── slide ───────────────────────────────────────────────────────────────────

@Composable
private fun SlidePage(s: WrapSlide, seed: Int) {
    Box(Modifier.fillMaxSize()) {
        SlideDecor(s.accent, seed)
        Column(
            Modifier.fillMaxSize().statusBarsPadding().padding(horizontal = 34.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                s.tag, color = s.accent, fontFamily = Display,
                fontSize = 11.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 4.sp,
            )
            s.title?.let {
                Spacer(Modifier.height(10.dp))
                Text(
                    it, color = TextPrimary, fontFamily = Display, fontSize = 24.sp,
                    fontWeight = FontWeight.Bold, textAlign = TextAlign.Center, lineHeight = 30.sp,
                )
            }
            Spacer(Modifier.height(30.dp))
            Text(s.huge, style = metricStyle(60), color = s.accent)
            Spacer(Modifier.height(6.dp))
            Text(
                s.hugeLabel, color = TextDim, fontFamily = Display,
                fontSize = 10.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 2.sp,
            )
            if (s.subs.isNotEmpty()) {
                Spacer(Modifier.height(34.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(40.dp)) {
                    s.subs.forEach { sub ->
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(sub.value, style = metricStyle(20), color = TextPrimary)
                            Spacer(Modifier.height(3.dp))
                            Text(
                                sub.label, color = TextDim, fontFamily = Display,
                                fontSize = 8.5.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 1.2.sp,
                            )
                        }
                    }
                }
            }
            s.footer?.let {
                Spacer(Modifier.height(30.dp))
                Text(
                    it, color = TextMuted, fontFamily = Body, fontSize = 13.sp,
                    lineHeight = 19.sp, textAlign = TextAlign.Center,
                )
            }
        }
    }
}

/** Subtle Canvas atmosphere: concentric rings, an arc, a sparse dot field. */
@Composable
private fun SlideDecor(accent: Color, seed: Int) {
    Canvas(Modifier.fillMaxSize()) {
        val rnd = Random(seed * 31 + 7)
        val c = Offset(size.width * 0.82f, size.height * 0.16f)
        drawCircle(accent.copy(alpha = 0.10f), size.minDimension * 0.28f, c, style = Stroke(1.dp.toPx()))
        drawCircle(accent.copy(alpha = 0.06f), size.minDimension * 0.40f, c, style = Stroke(0.75.dp.toPx()))
        drawCircle(accent.copy(alpha = 0.04f), size.minDimension * 0.54f, c, style = Stroke(0.5.dp.toPx()))
        drawArc(
            accent.copy(alpha = 0.08f), 180f, 120f, false,
            topLeft = Offset(-size.width * 0.25f, size.height * 0.72f),
            size = Size(size.width * 0.7f, size.width * 0.7f),
            style = Stroke(1.dp.toPx()),
        )
        repeat(26) {
            val p = Offset(rnd.nextFloat() * size.width, rnd.nextFloat() * size.height)
            drawCircle(
                accent.copy(alpha = 0.04f + rnd.nextFloat() * 0.07f),
                0.8.dp.toPx() + rnd.nextFloat() * 1.6.dp.toPx(), p,
            )
        }
    }
}

/** Guard-overlay close pattern: floating glass orb, top-right. */
@Composable
private fun BoxScope.CloseOrb(onClose: () -> Unit) {
    Box(
        Modifier.align(Alignment.TopEnd).statusBarsPadding().padding(16.dp)
            .size(40.dp).clip(RoundedCornerShape(13.dp))
            .background(Color.White.copy(alpha = 0.06f))
            .border(0.5.dp, Color.White.copy(alpha = 0.12f), RoundedCornerShape(13.dp))
            .clickable(onClick = onClose),
        contentAlignment = Alignment.Center,
    ) { Icon(Icons.Rounded.Close, null, tint = TextPrimary, modifier = Modifier.size(19.dp)) }
}
