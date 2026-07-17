package com.ascend.lifeos.ui.home

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.RadioButtonUnchecked
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.ascend.lifeos.data.Repo
import com.ascend.lifeos.data.calendar.CalendarRepo
import com.ascend.lifeos.ui.kit.Panel
import com.ascend.lifeos.ui.kit.SectionLabel
import com.ascend.lifeos.ui.theme.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.LocalDate

/**
 * Game-day mode (idea #4). On days with a hockey event, a pre-game readiness card:
 * puck-drop time + a Sleep / Fueled / Hydrated check derived from existing data,
 * and a one-line cue. Read-only — no new data model; hydration already gets its
 * +0.5 L game-day bump from WaterCalc.
 */
@Composable
fun GameDayCard(modifier: Modifier = Modifier) {
    val ctx = LocalContext.current
    val game by produceState<Pair<Int, Int>?>(null) {
        value = withContext(Dispatchers.IO) {
            val today = com.ascend.lifeos.core.todayDate().toEpochDay()
            runCatching {
                CalendarRepo.dao(ctx).eventsInRangeOnce(today, today)
                    .filter { it.type == "HOCKEY" && !it.allDay }
                    .minByOrNull { it.startMin }
                    ?.let { it.startMin to it.endMin }
            }.getOrNull()
        }
    }
    val g = game ?: return

    val p = Repo.profile()
    val sleepMin = Repo.data.health?.sleepMin ?: 0
    val kcal = Repo.today().meals.sumOf { it.kcal }
    val hydration = Repo.hydrationMl(Repo.today())
    val waterGoalMl = (p.waterGoal * 250).coerceAtLeast(1)

    // learned personal need (Rise-style, strain/growth-aware) with the same
    // 30-min grace the old fixed 450 gave an 8h default — personalizes as
    // the app learns instead of judging everyone by 7h30
    val sleptOk = sleepMin >= (Repo.sleepNeedMin() - 30).coerceAtLeast(330)
    val fueledOk = kcal >= p.kcalGoal * 0.4             // eaten a meaningful share pre-game
    val hydratedOk = hydration >= waterGoalMl * 0.6
    val greens = listOf(sleptOk, fueledOk, hydratedOk).count { it }

    fun fmt(m: Int) = "%02d:%02d".format(m / 60, m % 60)
    // the card speaks the athlete's sport: hockey keeps its puck drop
    val sport = com.ascend.lifeos.data.training.SportCatalog.byId(p.sport)
    val startWord = if (sport.id == "hockey") "puck drop" else "start"
    val cue = when {
        greens == 3 -> "Locked in — go win it."
        !sleptOk -> "Short on sleep — long warm-up, carbs by 2 h before, hydrate now."
        !fueledOk -> "Top up carbs before the $startWord — pasta/rice ~2–3 h out."
        else -> "Sip water steadily until warm-up, then taper."
    }

    Panel(modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            SectionLabel("${sport.emoji} ${sport.dayWord} · $startWord ${fmt(g.first)}", accent = Mod.Body)
            Spacer(Modifier.height(10.dp))
            Check("Sleep", sleptOk, "${sleepMin / 60}h ${sleepMin % 60}m")
            Check("Fueled", fueledOk, "$kcal kcal")
            Check("Hydrated", hydratedOk, "%.1f L".format(hydration / 1000.0))
            Spacer(Modifier.height(10.dp))
            Text(cue, color = TextDim, fontFamily = Body, fontSize = FS.s12, fontWeight = FontWeight.Medium)
        }
    }
}

@Composable
private fun Check(label: String, ok: Boolean, value: String) {
    Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(
            if (ok) Icons.Rounded.CheckCircle else Icons.Rounded.RadioButtonUnchecked,
            "$label ${if (ok) "met" else "not met"}", tint = if (ok) Good else Crit, modifier = Modifier.size(17.dp),
        )
        Spacer(Modifier.width(10.dp))
        Text(label, color = TextPrimary, fontFamily = Body, fontSize = FS.s13_5, fontWeight = FontWeight.Medium, modifier = Modifier.weight(1f))
        Text(value, color = TextDim, fontFamily = Body, fontSize = FS.s12_5)
    }
}
