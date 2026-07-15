package com.ascend.lifeos.data

import android.content.Context
import com.ascend.lifeos.data.calendar.CalendarRepo
import com.ascend.lifeos.data.calendar.EventType
import java.time.LocalDate
import java.time.LocalTime

/**
 * Protocols — WHEN → THEN rules across every module. No other app can run
 * "WHEN recovery is low THEN reschedule training and lock socials", because
 * no other app sees all of it. Each protocol is a pure check that may fire a
 * directive (shown as a Home card); heavier actions reuse existing systems.
 */
data class Protocol(
    val id: String,
    val title: String,
    val description: String,
    val check: suspend (Context) -> String?,   // directive text when firing
)

object Protocols {

    fun enabled(ctx: Context, id: String): Boolean = Prefs.bool(ctx, "proto_$id", true)
    fun setEnabled(ctx: Context, id: String, on: Boolean) = Prefs.setBool(ctx, "proto_$id", on)

    val ALL: List<Protocol> = listOf(
        Protocol(
            "game_day", "Game day",
            "Sport block today → carbs at lunch, hydrate early, legs stay fresh",
        ) { ctx ->
            val today = LocalDate.now()
            val dao = CalendarRepo.dao(ctx)
            val entities = dao.eventsInRangeOnce(today.toEpochDay(), today.toEpochDay())
            val tl = CalendarRepo.timelineFor(ctx, today, entities)
            val game = tl.blocks.firstOrNull { it.type == EventType.HOCKEY }
            game?.let {
                // the directive speaks the athlete's sport (hockey keeps face-off)
                val sport = com.ascend.lifeos.data.training.SportCatalog
                    .byId(com.ascend.lifeos.data.Repo.data.profile.sport)
                val start = if (sport.id == "hockey") "face-off" else "start"
                val arena = if (sport.id == "hockey") "the ice" else "the game"
                "Game day: $start ${CalendarRepo.fmtMin(it.startMin)}. Carbs at lunch, " +
                    "hydrate now, no heavy legs before $arena."
            }
        },
        Protocol(
            "exam_focus", "Exam focus",
            "Exam within 3 days → study reminder, training already trimmed",
        ) { ctx ->
            val today = LocalDate.now()
            val dao = CalendarRepo.dao(ctx)
            val exams = dao.eventsInRangeOnce(today.toEpochDay(), today.plusDays(3).toEpochDay())
                .filter { it.type == EventType.EXAM.name && CalendarRepo.run { true } }
            exams.minByOrNull { it.dayEpoch }?.let { e ->
                val days = (e.dayEpoch - today.toEpochDay()).toInt()
                "${e.title} in ${if (days == 0) "TODAY" else "$days day${if (days > 1) "s" else ""}"} — " +
                    "one focused study block beats three distracted ones."
            }
        },
        Protocol(
            "sickness_watch", "Illness early warning",
            "Resting HR +5 over baseline for 2 days → back off before it hits",
        ) { ctx ->
            if (!Prefs.bool(ctx, Prefs.SICKNESS_ALERT, true)) return@Protocol null
            Repo.sicknessSignal()?.let { d ->
                "Resting HR +$d bpm over baseline for 2 days — early illness sign. " +
                    "Train light, sleep long. Sick mode is one tap away in Body."
            }
        },
        Protocol(
            "low_recovery", "Low-recovery guard",
            "Recovery < 50 → intensity gets pulled automatically",
        ) { _ ->
            Repo.recoveryScore()?.takeIf { it < 50 }?.let {
                "Recovery $it — today's plan runs at reduced intensity. The gains happen when you rest."
            }
        },
        Protocol(
            "hydration_catchup", "Hydration catch-up",
            "Trained but under half the water goal → catch up before evening",
        ) { _ ->
            val day = Repo.today()
            val p = Repo.profile()
            if (day.workoutDone || day.cali.values.any { it.isNotEmpty() } || Repo.workoutSets(day) > 0) {
                // *2 instead of /2: integer division truncated (goal 9 → 4), so a
                // user at exactly 4/9 (44 %, genuinely under half) missed the nudge.
                // Count logged drinks too (hydrationMl), matching completion()/Home —
                // otherwise a 1.5 L bottle day still nags "water is at 0".
                val hydrationMl = Repo.hydrationMl(day)
                if (hydrationMl * 2 < p.waterGoal * WaterCalc.GLASS_ML && LocalTime.now().hour >= 15) {
                    "You trained but water is at ${hydrationMl / WaterCalc.GLASS_ML}/${p.waterGoal} — two glasses now."
                } else null
            } else null
        },
        Protocol(
            "streak_guard", "Streak guard",
            "Evening with open missions → one warning before the chain breaks",
        ) { _ ->
            if (LocalTime.now().hour >= 20) {
                val c = Repo.completion()
                val p = Repo.profile()
                if (c.done < c.total && p.streak >= 3) {
                    "Streak ${p.streak} on the line — ${c.total - c.done} mission${if (c.total - c.done > 1) "s" else ""} still open."
                } else null
            } else null
        },
        // creatine_guard removed: the supplement check-off UI died in the v2
        // rework, so day.supps was never written and the warning fired daily.
        Protocol(
            "holiday_slot", "Holiday bonus slot",
            "Holiday + nothing planned → claim the free morning",
        ) { ctx ->
            val today = LocalDate.now()
            val dao = CalendarRepo.dao(ctx)
            val entities = dao.eventsInRangeOnce(today.toEpochDay(), today.toEpochDay())
            val tl = CalendarRepo.timelineFor(ctx, today, entities)
            if (tl.isHoliday && tl.blocks.none { it.type == EventType.TRAINING }) {
                "Holiday and the calendar is open — a morning session hits different."
            } else null
        },
        Protocol(
            "bedtime_tomorrow", "Early-start warning",
            "First block before 08:00 tomorrow → bedtime heads-up at night",
        ) { ctx ->
            if (LocalTime.now().hour < 21) return@Protocol null
            val tomorrow = LocalDate.now().plusDays(1)
            val dao = CalendarRepo.dao(ctx)
            val entities = dao.eventsInRangeOnce(tomorrow.toEpochDay(), tomorrow.toEpochDay())
            val tl = CalendarRepo.timelineFor(ctx, tomorrow, entities)
            val first = tl.blocks.minByOrNull { it.startMin } ?: return@Protocol null
            if (first.startMin <= 8 * 60) {
                val need = Repo.sleepNeedMin()
                val bed = ((first.startMin - 75 - need) % 1440 + 1440) % 1440
                "${first.title} at ${CalendarRepo.fmtMin(first.startMin)} tomorrow — lights out by ${CalendarRepo.fmtMin(bed)}."
            } else null
        },
        Protocol(
            "growth_sleep", "Growth-spurt sleep",
            "Growing >0.5 cm/month → sleep need raised automatically",
        ) { ctx ->
            if (!Prefs.bool(ctx, Prefs.GROWTH_TRACKING, false)) return@Protocol null
            val heights = Repo.data.profile.measurements["height"].orEmpty()
            if (heights.size < 2) return@Protocol null
            val monthAgo = System.currentTimeMillis() - 35L * 86_400_000
            val old = heights.lastOrNull { it.ts < monthAgo } ?: return@Protocol null
            val delta = heights.last().cm - old.cm
            if (delta > 0.5) "Growth spurt: +%.1f cm this month — sleep target raised 20 min.".format(delta) else null
        },
        Protocol(
            "weather_watch", "Weather watch",
            "Outdoor-flagged plans get a forecast check — one suggestion, never auto-moved",
        ) { ctx ->
            val titles = WeatherRepo.outdoorTitles(ctx)
            if (titles.isEmpty()) return@Protocol null
            runCatching { WeatherRepo.refresh(ctx) }
            val codes = WeatherRepo.hourlyCode ?: return@Protocol null
            val temps = WeatherRepo.hourlyTemp
            val today = LocalDate.now()
            val nowMin = LocalTime.now().let { it.hour * 60 + it.minute }
            val ev = CalendarRepo.dao(ctx)
                .eventsInRangeOnce(today.toEpochDay(), today.toEpochDay())
                .filter { !it.allDay && it.endMin > nowMin && it.title.trim().lowercase() in titles }
                .minByOrNull { it.startMin } ?: return@Protocol null
            val hour = (ev.startMin / 60).coerceIn(0, 23)
            val temp = temps?.getOrNull(hour)
            if (!WeatherRepo.badWeather(codes.getOrNull(hour) ?: 0, temp)) return@Protocol null
            val tempTxt = temp?.let { " · ${it.toInt()}°" } ?: ""
            "${ev.title} ${CalendarRepo.fmtMin(ev.startMin)} is outdoor — forecast looks rough$tempTxt. " +
                "Move it or plan the indoor alternative? Your call."
        },
    )

    /** Evaluate all enabled protocols; max one directive per protocol per day. */
    suspend fun fire(ctx: Context): List<Pair<Protocol, String>> {
        if (!Prefs.bool(ctx, Prefs.PROTOCOLS_ON, true)) return emptyList()
        val today = com.ascend.lifeos.core.todayKey()
        val out = ArrayList<Pair<Protocol, String>>()
        for (p in ALL) {
            if (!enabled(ctx, p.id)) continue
            val seenKey = "proto_seen_${p.id}"
            if (Prefs.string(ctx, seenKey, "") == today) continue   // dismissed today
            runCatching { p.check(ctx) }.getOrNull()?.let { out.add(p to it) }
        }
        return out.take(2)
    }

    fun dismissToday(ctx: Context, id: String) =
        Prefs.setString(ctx, "proto_seen_$id", com.ascend.lifeos.core.todayKey())
}
