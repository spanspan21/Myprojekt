package com.ascend.lifeos.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Bedtime
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.SelfImprovement
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ascend.lifeos.data.Haptics
import com.ascend.lifeos.data.Prefs
import com.ascend.lifeos.data.Repo
import com.ascend.lifeos.data.calendar.CalendarRepo
import com.ascend.lifeos.ui.kit.JarvisHeader
import com.ascend.lifeos.ui.kit.ModuleBackground
import com.ascend.lifeos.ui.kit.Panel
import com.ascend.lifeos.ui.motion.pressScale
import com.ascend.lifeos.ui.theme.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.LocalDate

@Composable
fun WindDownScreen(onClose: () -> Unit, onOpenBreathe: () -> Unit) {
    val WindDownAccent = Mod.Mind
    val ctx = LocalContext.current
    val d = Repo.bodyDay()
    val sleepNeed = Repo.sleepNeedMin()
    val sleepDebt = Repo.sleepDebtMin()

    var bedtime by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(Unit) {
        bedtime = withContext(Dispatchers.IO) {
            runCatching {
                val tomorrow = LocalDate.now().plusDays(1)
                val dao = CalendarRepo.dao(ctx)
                val entities = dao.eventsInRangeOnce(tomorrow.toEpochDay(), tomorrow.toEpochDay())
                val tl = CalendarRepo.timelineFor(ctx, tomorrow, entities)
                val first = tl.blocks.minByOrNull { it.startMin } ?: return@runCatching null
                val wakeMin = first.startMin - 75
                val target = wakeMin - sleepNeed
                val bedMin = ((target % (24 * 60)) + 24 * 60) % (24 * 60)
                "%02d:%02d".format(bedMin / 60, bedMin % 60)
            }.getOrNull()
        }
    }

    Box(Modifier.fillMaxSize()) {
        ModuleBackground(WindDownAccent)
        Column(
            Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp).padding(top = 8.dp, bottom = 100.dp),
        ) {
            JarvisHeader("Wind Down", "Evening routine", WindDownAccent) {
                Icon(Icons.Rounded.Close, "Close", tint = TextDim, modifier = Modifier.size(20.dp).clip(CircleShape).clickable(onClick = onClose))
            }
            Spacer(Modifier.height(20.dp))

            // Bedtime card
            Panel(Modifier.fillMaxWidth(), corner = 18.dp) {
                Row(Modifier.padding(18.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.Bedtime, null, tint = WindDownAccent, modifier = Modifier.size(28.dp))
                    Spacer(Modifier.width(14.dp))
                    Column {
                        Text(
                            bedtime?.let { "Lights out by $it" } ?: "Target: ${sleepNeed / 60}h ${sleepNeed % 60}m sleep",
                            color = TextPrimary, fontFamily = Body, fontSize = FS.s15, fontWeight = FontWeight.Bold,
                        )
                        if (sleepDebt > Prefs.int(ctx, Prefs.SLEEP_DEBT_WARN, 60)) {
                            Spacer(Modifier.height(3.dp))
                            Text(
                                "Sleep debt: ${sleepDebt / 60}h ${sleepDebt % 60}m — aim for extra tonight",
                                color = Warn, fontFamily = Body, fontSize = FS.s11, fontWeight = FontWeight.SemiBold,
                            )
                        }
                    }
                }
            }
            Spacer(Modifier.height(16.dp))

            // Evening check-in
            Panel(Modifier.fillMaxWidth(), corner = 16.dp) {
                Column(Modifier.padding(16.dp)) {
                    Text("How was your day?", color = TextPrimary, fontFamily = Body, fontSize = FS.s14, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(4.dp))
                    Text("Your evening check-in feeds tomorrow's readiness score", color = TextDim, fontFamily = Body, fontSize = FS.s11)
                    Spacer(Modifier.height(12.dp))

                    Text("Stress level", color = TextMuted, fontFamily = Body, fontSize = FS.s12, fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.height(6.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        WindDownChip("Calm", d?.eveningStress == 1, Good) { Repo.setCheckIn(eveningStress = 1) }
                        WindDownChip("OK", d?.eveningStress == 2, Warn) { Repo.setCheckIn(eveningStress = 2) }
                        WindDownChip("Fried", d?.eveningStress == 3, Crit) { Repo.setCheckIn(eveningStress = 3) }
                    }
                    Spacer(Modifier.height(14.dp))

                    Text("Tonight's factors", color = TextMuted, fontFamily = Body, fontSize = FS.s12, fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.height(6.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                        WindDownFactor("Late caffeine", d?.fCaffeineLate == true) { Repo.setJournalFactor(caffeineLate = it) }
                        WindDownFactor("Alcohol", d?.fAlcohol == true) { Repo.setJournalFactor(alcohol = it) }
                    }
                    Spacer(Modifier.height(7.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                        WindDownFactor("Late meal", d?.fLateMeal == true) { Repo.setJournalFactor(lateMeal = it) }
                        WindDownFactor("Screen in bed", d?.fScreenLate == true) { Repo.setJournalFactor(screenLate = it) }
                    }
                    Spacer(Modifier.height(7.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                        WindDownFactor("Meditation", d?.fMeditation == true) { Repo.setJournalFactor(meditation = it) }
                        WindDownFactor("Supplements", d?.fSupplements == true) { Repo.setJournalFactor(supplements = it) }
                    }
                }
            }
            Spacer(Modifier.height(16.dp))

            // Breathing shortcut
            Panel(
                Modifier.fillMaxWidth().pressScale(onClick = onOpenBreathe),
                corner = 16.dp,
            ) {
                Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.SelfImprovement, "Open breathing exercise", tint = Good, modifier = Modifier.size(24.dp))
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text("Breathe before bed", color = TextPrimary, fontFamily = Body, fontSize = FS.s13_5, fontWeight = FontWeight.Bold)
                        Text("4-7-8 pattern recommended for sleep", color = TextDim, fontFamily = Body, fontSize = FS.s11)
                    }
                    Text("→", color = Good, fontFamily = Display, fontSize = FS.s17, fontWeight = FontWeight.Bold)
                }
            }
            Spacer(Modifier.height(16.dp))

            // Sleep hygiene checklist
            Panel(Modifier.fillMaxWidth(), corner = 16.dp) {
                Column(Modifier.padding(16.dp)) {
                    Text("Sleep hygiene", color = TextPrimary, fontFamily = Body, fontSize = FS.s14, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(10.dp))
                    val rules = listOf(
                        "Room cool — 18–20°C is optimal",
                        "Dark room — block all light sources",
                        "No screens 30 min before bed",
                        "Caffeine cutoff 6–8h before sleep",
                        "Consistent bed/wake time (±30 min)",
                        "Avoid heavy meals 2–3h before bed",
                    )
                    rules.forEach { rule ->
                        Row(
                            Modifier.padding(vertical = 4.dp),
                            verticalAlignment = Alignment.Top,
                        ) {
                            Box(Modifier.padding(top = 6.dp).size(5.dp).clip(CircleShape).background(WindDownAccent.copy(alpha = 0.6f)))
                            Spacer(Modifier.width(10.dp))
                            Text(rule, color = TextMuted, fontFamily = Body, fontSize = FS.s12, lineHeight = 17.sp)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun WindDownChip(label: String, on: Boolean, color: Color, onClick: () -> Unit) {
    val ctx = LocalContext.current
    Box(
        Modifier.clip(RoundedCornerShape(10.dp))
            .background(if (on) color.copy(alpha = 0.15f) else Ivory.copy(alpha = 0.04f))
            .border(0.5.dp, if (on) color.copy(alpha = 0.5f) else Ivory.copy(alpha = 0.10f), RoundedCornerShape(10.dp))
            .pressScale { onClick(); Haptics.tick(ctx) }
            .padding(horizontal = 14.dp, vertical = 8.dp),
    ) { Text(label, color = if (on) color else TextMuted, fontSize = FS.s12, fontFamily = Body, fontWeight = FontWeight.Bold) }
}

@Composable
private fun WindDownFactor(label: String, on: Boolean, onToggle: (Boolean) -> Unit) {
    val ctx = LocalContext.current
    val accent = Mod.Mind
    Box(
        Modifier.clip(RoundedCornerShape(10.dp))
            .background(if (on) accent.copy(alpha = 0.14f) else Ivory.copy(alpha = 0.04f))
            .border(0.5.dp, if (on) accent.copy(alpha = 0.5f) else Ivory.copy(alpha = 0.10f), RoundedCornerShape(10.dp))
            .pressScale { onToggle(!on); Haptics.tick(ctx) }
            .padding(horizontal = 12.dp, vertical = 8.dp),
    ) { Text(label, color = if (on) accent else TextMuted, fontSize = FS.s12, fontFamily = Body, fontWeight = FontWeight.Bold) }
}
