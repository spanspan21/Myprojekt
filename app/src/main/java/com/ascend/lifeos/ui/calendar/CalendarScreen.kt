package com.ascend.lifeos.ui.calendar

import android.app.Application
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.GenericShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.Checklist
import androidx.compose.material.icons.rounded.ChevronLeft
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Link
import androidx.compose.material.icons.rounded.RssFeed
import androidx.compose.material.icons.rounded.Sync
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ascend.lifeos.data.CalendarSync
import com.ascend.lifeos.data.calendar.*
import com.ascend.lifeos.ui.kit.*
import com.ascend.lifeos.ui.motion.Motion
import com.ascend.lifeos.ui.motion.pressScale
import com.ascend.lifeos.ui.theme.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

// ─── CALENDAR — the timeline JARVIS plans around ─────────────────────────────

class CalendarViewModel(app: Application) : AndroidViewModel(app) {
    private val dao = CalendarRepo.dao(app)

    var selectedDay by mutableStateOf(LocalDate.now())

    private val rangeFrom = LocalDate.now().minusDays(120).toEpochDay()
    private val rangeTo = LocalDate.now().plusDays(400).toEpochDay()

    val entities = dao.eventsInRange(rangeFrom, rangeTo)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun add(
        title: String, type: EventType, day: LocalDate, endDay: LocalDate,
        startMin: Int, endMin: Int, allDay: Boolean, repeatMask: Int,
    ) = viewModelScope.launch(Dispatchers.IO) {
        CalendarRepo.upsert(getApplication(), title, type, day, endDay, startMin, endMin, allDay, repeatMask)
    }

    fun delete(id: String) = viewModelScope.launch(Dispatchers.IO) { dao.delete(id) }
}

// type → colour, one place
fun eventColor(t: EventType): Color = when (t) {
    EventType.SCHOOL -> Color(0xFF5B9DFF)
    EventType.WORK -> Color(0xFF9BA7B8)
    EventType.HOCKEY -> Color(0xFF4CD4FF)
    EventType.TRAINING -> Mod.Train
    EventType.EXAM -> Crit
    EventType.HOLIDAY -> Good
    EventType.PERSONAL -> Mod.Calendar
}

fun eventLabel(t: EventType): String = when (t) {
    EventType.SCHOOL -> "School"; EventType.WORK -> "Work"; EventType.HOCKEY -> "Hockey"
    EventType.TRAINING -> "Training"; EventType.EXAM -> "Exam"; EventType.HOLIDAY -> "Holiday"
    EventType.PERSONAL -> "Personal"
}

@Composable
fun CalendarScreen(vm: CalendarViewModel = viewModel()) {
    val ctx = LocalContext.current
    val entities by vm.entities.collectAsState()
    val day = vm.selectedDay

    var addOpen by remember { mutableStateOf(false) }
    var prefillStart by remember { mutableStateOf<Int?>(null) }
    var detailBlock by remember { mutableStateOf<TimelineBlock?>(null) }
    var permTick by remember { mutableIntStateOf(0) }
    var monthOpen by remember { mutableStateOf(false) }
    var settingsOpen by remember { mutableStateOf(false) }
    var tasksOpen by remember { mutableStateOf(false) }

    // timeline: own events are instant; device events load off the main thread
    var timeline by remember { mutableStateOf<DayTimeline?>(null) }
    LaunchedEffect(day, entities, permTick) {
        timeline = withContext(Dispatchers.IO) {
            CalendarRepo.timelineFor(ctx, day, entities)
        }
    }

    // "imports itself": throttled feed refresh on open (Room flows update the UI)
    LaunchedEffect(Unit) {
        runCatching { com.ascend.lifeos.data.calendar.CalendarAutoSync.maybe(ctx) }
    }

    Box(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize().statusBarsPadding().padding(horizontal = 20.dp)) {
            Spacer(Modifier.height(14.dp))

            val tl = timeline
            val contextLine = when {
                tl == null -> "reading timeline…"
                tl.isHoliday -> "Holiday — school is off the books"
                tl.blocks.isEmpty() -> "Clear day · all yours"
                else -> {
                    val free = tl.freeSlots.sumOf { it.durationMin } / 60f
                    "${tl.blocks.size} blocks · %.1fh free".format(free)
                }
            }
            JarvisHeader("Calendar", contextLine, Mod.Calendar) {
                IconOrb(Icons.Rounded.CalendarMonth, tint = Mod.Calendar, size = 36.dp) { monthOpen = true }
                Spacer(Modifier.width(8.dp))
                IconOrb(Icons.Rounded.Checklist, tint = Mod.Calendar, size = 36.dp) { tasksOpen = true }
                Spacer(Modifier.width(8.dp))
                IconOrb(Icons.Rounded.Tune, size = 36.dp) { settingsOpen = true }
            }

            Spacer(Modifier.height(16.dp))

            // ── week strip ───────────────────────────────────────────
            WeekStrip(day, entities, onSelect = { vm.selectedDay = it })

            Spacer(Modifier.height(12.dp))

            // ── all-day chips (holidays etc.) ────────────────────────
            timeline?.allDays?.takeIf { it.isNotEmpty() }?.let { allDays ->
                Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    allDays.forEach { b ->
                        val c = eventColor(b.type)
                        Box(
                            Modifier.clip(RoundedCornerShape(9.dp)).background(c.copy(alpha = 0.12f))
                                .border(0.5.dp, c.copy(alpha = 0.35f), RoundedCornerShape(9.dp))
                                .clickable { if (!b.fromDevice) detailBlock = b }
                                .padding(horizontal = 10.dp, vertical = 5.dp),
                        ) {
                            Text(b.title, color = c, fontSize = 11.sp, fontFamily = Body, fontWeight = FontWeight.Bold)
                        }
                    }
                }
                Spacer(Modifier.height(10.dp))
            }

            // ── timeline ─────────────────────────────────────────────
            val t = timeline
            if (t == null) {
                Box(Modifier.fillMaxWidth().padding(vertical = 80.dp), contentAlignment = Alignment.Center) {
                    Text("Reading timeline…", color = TextDim, fontSize = 13.sp, fontFamily = Body)
                }
            } else {
                DayTimelineView(
                    t,
                    onBlockTap = { if (!it.fromDevice) detailBlock = it },
                    onSlotTap = { slot -> prefillStart = slot.startMin; addOpen = true },
                )
            }
        }

        // FAB
        Box(
            Modifier.align(Alignment.BottomEnd).padding(end = 20.dp, bottom = 110.dp)
                .size(54.dp).clip(CircleShape)
                .background(Mod.Calendar)
                .clickable { prefillStart = null; addOpen = true },
            contentAlignment = Alignment.Center,
        ) { Icon(Icons.Rounded.Add, "Add event", tint = Void, modifier = Modifier.size(24.dp)) }

        // month overlay — above everything incl. FAB
        if (monthOpen) {
            MonthOverlay(
                selected = day,
                entities = entities,
                onPick = { vm.selectedDay = it; monthOpen = false },
                onClose = { monthOpen = false },
            )
        }
    }

    if (settingsOpen) {
        CalendarSettingsSheet(onDismiss = { settingsOpen = false; permTick++ })
    }

    if (tasksOpen) {
        TaskBlocksSheet(onDismiss = { tasksOpen = false; permTick++ })
    }

    if (addOpen) {
        QuickAddSheet(
            day = day,
            prefillStart = prefillStart,
            onSave = { title, type, d, endD, s, e, allDay, mask ->
                vm.add(title, type, d, endD, s, e, allDay, mask)
                addOpen = false
            },
            onDismiss = { addOpen = false },
        )
    }

    detailBlock?.let { b ->
        EventDetailSheet(b, onDelete = { vm.delete(b.id); detailBlock = null }, onDismiss = { detailBlock = null })
    }
}

// ─── Week strip ──────────────────────────────────────────────────────────────

@Composable
private fun WeekStrip(selected: LocalDate, entities: List<CalEventEntity>, onSelect: (LocalDate) -> Unit) {
    val weekStart = selected.with(DayOfWeek.MONDAY)
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Icon(
            Icons.Rounded.ChevronLeft, null, tint = TextDim,
            modifier = Modifier.size(22.dp).clickable { onSelect(selected.minusWeeks(1)) },
        )
        Row(Modifier.weight(1f), horizontalArrangement = Arrangement.SpaceEvenly) {
            for (i in 0..6) {
                val d = weekStart.plusDays(i.toLong())
                DayChip(d, d == selected, d == LocalDate.now(), entities) { onSelect(d) }
            }
        }
        Icon(
            Icons.Rounded.ChevronRight, null, tint = TextDim,
            modifier = Modifier.size(22.dp).clickable { onSelect(selected.plusWeeks(1)) },
        )
    }
}

@Composable
private fun DayChip(d: LocalDate, selected: Boolean, isToday: Boolean, entities: List<CalEventEntity>, onClick: () -> Unit) {
    val types = remember(d, entities) {
        entities.filter { CalendarRepo.occursOn(it, d) }
            .map { runCatching { EventType.valueOf(it.type) }.getOrDefault(EventType.PERSONAL) }
            .distinct().take(3)
    }
    val bg by animateColorAsState(if (selected) Mod.Calendar.copy(alpha = 0.14f) else Color.Transparent, tween(Motion.quick), label = "dcB")
    val edge by animateColorAsState(
        when {
            selected -> Mod.Calendar.copy(alpha = 0.5f)
            isToday -> com.ascend.lifeos.ui.theme.Ivory.copy(alpha = 0.18f)
            else -> Color.Transparent
        },
        tween(Motion.quick), label = "dcE",
    )
    val over by animateColorAsState(if (selected) Mod.Calendar else TextDim, tween(Motion.quick), label = "dcO")
    val num by animateColorAsState(if (selected) TextPrimary else TextMuted, tween(Motion.quick), label = "dcN")
    Column(
        Modifier.pressScale(onClick)
            .clip(RoundedCornerShape(13.dp))
            .background(bg)
            .border(0.5.dp, edge, RoundedCornerShape(13.dp))
            .padding(horizontal = 9.dp, vertical = 7.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            d.format(DateTimeFormatter.ofPattern("EEE", Locale.ENGLISH)).uppercase().take(2),
            color = over,
            fontFamily = Display, fontSize = 9.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 1.sp,
        )
        Spacer(Modifier.height(2.dp))
        Text(
            "${d.dayOfMonth}", color = num,
            style = metricStyle(15, FontWeight.SemiBold),
        )
        Spacer(Modifier.height(3.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
            if (types.isEmpty()) Box(Modifier.size(3.dp))
            types.forEach { t ->
                Box(Modifier.size(3.dp).clip(CircleShape).background(eventColor(t)))
            }
        }
    }
}

// ─── Month overlay ───────────────────────────────────────────────────────────

/** Top-right corner triangle — the hockey marker on month cells. */
private val CornerTriangle = GenericShape { size, _ ->
    moveTo(0f, 0f); lineTo(size.width, 0f); lineTo(size.width, size.height); close()
}

@Composable
private fun MonthOverlay(
    selected: LocalDate,
    entities: List<CalEventEntity>,
    onPick: (LocalDate) -> Unit,
    onClose: () -> Unit,
) {
    BackHandler(onBack = onClose)
    var month by remember { mutableStateOf(YearMonth.from(selected)) }

    Box(
        Modifier.fillMaxSize()
            .background(Void.copy(alpha = 0.97f))
            // consume taps so nothing beneath the overlay reacts
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {},
    ) {
        Column(Modifier.fillMaxSize().statusBarsPadding().padding(horizontal = 20.dp)) {
            Spacer(Modifier.height(14.dp))
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "MONTH", color = Mod.Calendar, fontFamily = Display,
                    fontSize = 10.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 2.5.sp,
                    modifier = Modifier.weight(1f),
                )
                IconOrb(Icons.Rounded.Close, size = 34.dp) { onClose() }
            }
            Spacer(Modifier.height(16.dp))

            // ‹ month › navigation
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                IconOrb(Icons.Rounded.ChevronLeft, size = 36.dp) { month = month.minusMonths(1) }
                Text(
                    month.format(DateTimeFormatter.ofPattern("MMMM uuuu", Locale.ENGLISH)).uppercase(),
                    color = TextPrimary, fontFamily = Display, fontSize = 19.sp,
                    fontWeight = FontWeight.Bold, letterSpacing = 3.sp,
                    textAlign = TextAlign.Center, modifier = Modifier.weight(1f),
                )
                IconOrb(Icons.Rounded.ChevronRight, size = 36.dp) { month = month.plusMonths(1) }
            }
            Spacer(Modifier.height(18.dp))

            // weekday header, Mon-first
            Row(Modifier.fillMaxWidth()) {
                listOf("MO", "TU", "WE", "TH", "FR", "SA", "SU").forEach {
                    Text(
                        it, color = TextDim, fontFamily = Display, fontSize = 9.sp,
                        fontWeight = FontWeight.SemiBold, letterSpacing = 1.sp,
                        textAlign = TextAlign.Center, modifier = Modifier.weight(1f),
                    )
                }
            }
            Spacer(Modifier.height(8.dp))

            // 6 × 7 grid
            val gridStart = remember(month) {
                month.atDay(1).let { it.minusDays((it.dayOfWeek.value - 1).toLong()) }
            }
            val today = LocalDate.now()
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                for (r in 0 until 6) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        for (c in 0 until 7) {
                            val d = gridStart.plusDays((r * 7 + c).toLong())
                            MonthDayCell(
                                d = d,
                                inMonth = YearMonth.from(d) == month,
                                isToday = d == today,
                                isSelected = d == selected,
                                entities = entities,
                                modifier = Modifier.weight(1f),
                            ) { onPick(d) }
                        }
                    }
                }
            }

            Spacer(Modifier.height(14.dp))
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    "CELL TINT = DAY LOAD", color = TextDim, fontFamily = Display,
                    fontSize = 8.5.sp, fontWeight = FontWeight.Medium, letterSpacing = 1.5.sp,
                )
                Spacer(Modifier.width(14.dp))
                Box(Modifier.size(7.dp).clip(CornerTriangle).background(eventColor(EventType.HOCKEY)))
                Spacer(Modifier.width(5.dp))
                Text(
                    "HOCKEY", color = TextDim, fontFamily = Display,
                    fontSize = 8.5.sp, fontWeight = FontWeight.Medium, letterSpacing = 1.5.sp,
                )
            }
        }
    }
}

@Composable
private fun MonthDayCell(
    d: LocalDate,
    inMonth: Boolean,
    isToday: Boolean,
    isSelected: Boolean,
    entities: List<CalEventEntity>,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    val occurring = remember(d, entities) { entities.filter { CalendarRepo.occursOn(it, d) } }
    val isHoliday = occurring.any { it.type == EventType.HOLIDAY.name }
    // load = timed blocks; school doesn't count during holidays (the timeline suppresses it too)
    val load = occurring.count { !it.allDay && !(isHoliday && it.type == EventType.SCHOOL.name) }
    val types = occurring
        .map { runCatching { EventType.valueOf(it.type) }.getOrDefault(EventType.PERSONAL) }
        .distinct().take(3)
    val hasHockey = occurring.any { it.type == EventType.HOCKEY.name }

    val tint = when {
        load <= 0 -> Color.Transparent
        load <= 2 -> Mod.Calendar.copy(alpha = 0.06f)
        load <= 4 -> Mod.Calendar.copy(alpha = 0.12f)
        else -> Mod.Calendar.copy(alpha = 0.20f)
    }

    Box(
        modifier.height(52.dp).clip(RoundedCornerShape(11.dp))
            .background(tint)
            .border(
                0.5.dp,
                when {
                    isSelected -> Mod.Calendar.copy(alpha = 0.55f)
                    isToday -> com.ascend.lifeos.ui.theme.Ivory.copy(alpha = 0.30f)
                    else -> Color.Transparent
                },
                RoundedCornerShape(11.dp),
            )
            .clickable(onClick = onClick),
    ) {
        if (hasHockey) {
            Box(
                Modifier.align(Alignment.TopEnd).size(7.dp)
                    .clip(CornerTriangle)
                    .background(eventColor(EventType.HOCKEY).copy(alpha = if (inMonth) 0.9f else 0.35f)),
            )
        }
        Column(Modifier.align(Alignment.Center), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                "${d.dayOfMonth}",
                color = when {
                    !inMonth -> TextDim.copy(alpha = 0.45f)
                    isToday -> TextPrimary
                    else -> TextMuted
                },
                style = metricStyle(13, FontWeight.SemiBold),
            )
            Spacer(Modifier.height(3.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(2.5.dp)) {
                if (types.isEmpty()) Box(Modifier.size(3.dp))
                types.forEach { t ->
                    Box(
                        Modifier.size(3.dp).clip(CircleShape)
                            .background(eventColor(t).copy(alpha = if (inMonth) 1f else 0.4f)),
                    )
                }
            }
        }
    }
}

// ─── ICS timetable feed row ──────────────────────────────────────────────────

@Composable
private fun IcsFeedRow() {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()

    var tick by remember { mutableIntStateOf(0) }
    val feed = remember(tick) { IcsSync.feedUrl(ctx) }
    val last = remember(tick) { IcsSync.lastSync(ctx) }

    var expanded by remember { mutableStateOf(false) }
    var input by remember { mutableStateOf("") }
    var syncing by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    fun runSync(firstAttempt: Boolean = false) {
        if (syncing) return
        error = null
        syncing = true
        scope.launch {
            IcsSync.sync(ctx) // hops to Dispatchers.IO internally
                .onSuccess { expanded = false; input = "" }
                .onFailure {
                    error = shortIcsError(it)
                    // bad first paste → drop the feed, stay in the editor to correct it
                    if (firstAttempt) IcsSync.removeFeed(ctx)
                }
            syncing = false
            tick++
        }
    }

    when {
        // feed configured → status row: last sync + re-sync + remove
        feed != null -> Row(
            Modifier.fillMaxWidth().padding(horizontal = 2.dp, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(Icons.Rounded.RssFeed, null, tint = Mod.Calendar.copy(alpha = 0.75f), modifier = Modifier.size(13.dp))
            Spacer(Modifier.width(8.dp))
            val lastTxt = if (last > 0L) {
                val t = Instant.ofEpochMilli(last).atZone(ZoneId.systemDefault()).toLocalTime()
                "last sync %02d:%02d".format(t.hour, t.minute)
            } else "not synced yet"
            Text(
                "Timetable feed · $lastTxt", color = TextMuted, fontSize = 11.5.sp,
                fontFamily = Body, fontWeight = FontWeight.Medium,
                maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f),
            )
            if (syncing) {
                CircularProgressIndicator(Modifier.size(13.dp), color = Mod.Calendar, strokeWidth = 1.5.dp)
            } else {
                Icon(
                    Icons.Rounded.Sync, null, tint = TextMuted,
                    modifier = Modifier.clip(CircleShape).clickable { runSync() }.padding(5.dp).size(15.dp),
                )
            }
            Spacer(Modifier.width(6.dp))
            Icon(
                Icons.Rounded.Close, null, tint = TextDim,
                modifier = Modifier.clip(CircleShape).clickable {
                    scope.launch {
                        IcsSync.removeFeed(ctx) // clears the feed + deletes its imported events
                        expanded = false; input = ""; error = null; tick++
                    }
                }.padding(5.dp).size(14.dp),
            )
        }

        // no feed, editor open → paste URL + sync
        expanded -> Panel(Modifier.fillMaxWidth(), corner = 14.dp) {
            Column(Modifier.padding(horizontal = 14.dp, vertical = 12.dp)) {
                Text(
                    "TIMETABLE FEED", color = Mod.Calendar, fontFamily = Display,
                    fontSize = 9.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 2.sp,
                )
                Spacer(Modifier.height(9.dp))
                Box(
                    Modifier.fillMaxWidth().clip(RoundedCornerShape(11.dp))
                        .background(com.ascend.lifeos.ui.theme.Ivory.copy(alpha = 0.05f))
                        .border(0.5.dp, com.ascend.lifeos.ui.theme.Ivory.copy(alpha = 0.10f), RoundedCornerShape(11.dp))
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                ) {
                    if (input.isEmpty()) Text(
                        "Paste ICS URL (https:// or webcal://)",
                        color = TextDim, fontSize = 12.sp, fontFamily = Body,
                    )
                    BasicTextField(
                        input, { input = it }, singleLine = true,
                        textStyle = TextStyle(color = TextPrimary, fontSize = 12.sp, fontFamily = Body, fontWeight = FontWeight.Medium),
                        cursorBrush = SolidColor(Mod.Calendar), modifier = Modifier.fillMaxWidth(),
                    )
                }
                Spacer(Modifier.height(10.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        Modifier.clip(RoundedCornerShape(11.dp))
                            .background(Mod.Calendar.copy(alpha = if (syncing) 0.55f else 1f))
                            .clickable(enabled = !syncing) {
                                val url = input.trim()
                                when {
                                    url.isBlank() -> error = "paste a feed URL first"
                                    !Regex("^(https?|webcal)://", RegexOption.IGNORE_CASE).containsMatchIn(url) ->
                                        error = "URL must start with http(s):// or webcal://"
                                    else -> {
                                        IcsSync.setFeed(ctx, url)
                                        runSync(firstAttempt = true)
                                    }
                                }
                            }
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                    ) {
                        Text(
                            if (syncing) "Syncing…" else "Sync",
                            color = Void, fontSize = 12.sp, fontFamily = Body, fontWeight = FontWeight.ExtraBold,
                        )
                    }
                    Spacer(Modifier.width(12.dp))
                    if (syncing) {
                        CircularProgressIndicator(Modifier.size(13.dp), color = Mod.Calendar, strokeWidth = 1.5.dp)
                    } else {
                        Text(
                            "Cancel", color = TextDim, fontSize = 11.5.sp, fontFamily = Body, fontWeight = FontWeight.Medium,
                            modifier = Modifier.clip(RoundedCornerShape(8.dp))
                                .clickable { expanded = false; error = null }
                                .padding(horizontal = 6.dp, vertical = 4.dp),
                        )
                    }
                }
            }
        }

        // no feed → subtle invite
        else -> Row(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp))
                .clickable { expanded = true }
                .padding(horizontal = 2.dp, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(Icons.Rounded.RssFeed, null, tint = TextDim, modifier = Modifier.size(13.dp))
            Spacer(Modifier.width(8.dp))
            Text(
                "Subscribe to a timetable feed (ICS)", color = TextDim, fontSize = 11.5.sp,
                fontFamily = Body, fontWeight = FontWeight.Medium,
            )
        }
    }

    error?.let {
        Spacer(Modifier.height(4.dp))
        Text(
            "Sync failed · $it", color = Crit.copy(alpha = 0.9f),
            fontSize = 10.5.sp, fontFamily = Body, fontWeight = FontWeight.Medium,
        )
    }
    Spacer(Modifier.height(12.dp))
}

private fun shortIcsError(e: Throwable): String = when (e) {
    is java.net.SocketTimeoutException -> "timed out — check the URL"
    is java.net.UnknownHostException -> "host not found — offline?"
    else -> e.message?.take(60) ?: "unknown error"
}

// ─── Day timeline ────────────────────────────────────────────────────────────

private const val HOUR_START = 6
private const val HOUR_END = 23
private val HOUR_DP = 46.dp

@Composable
private fun DayTimelineView(
    t: DayTimeline,
    onBlockTap: (TimelineBlock) -> Unit,
    onSlotTap: (FreeSlot) -> Unit,
) {
    val totalHours = HOUR_END - HOUR_START
    val scroll = rememberScrollState()

    // auto-scroll near "now" on first show of today
    LaunchedEffect(t.day) {
        if (t.day == LocalDate.now()) {
            val nowMin = LocalTime.now().hour * 60 + LocalTime.now().minute
            val frac = ((nowMin - HOUR_START * 60).toFloat() / (totalHours * 60)).coerceIn(0f, 1f)
            scroll.scrollTo((scroll.maxValue * (frac - 0.18f).coerceAtLeast(0f)).toInt())
        }
    }

    Box(Modifier.fillMaxSize().verticalScroll(scroll).padding(bottom = 110.dp)) {
        Column {
            for (h in HOUR_START until HOUR_END) {
                Row(Modifier.height(HOUR_DP)) {
                    Text(
                        "%02d".format(h), color = TextDim.copy(alpha = 0.6f),
                        fontFamily = Display, fontSize = 9.5.sp, fontWeight = FontWeight.Medium,
                        modifier = Modifier.width(26.dp).padding(top = 0.dp),
                    )
                    Column(Modifier.weight(1f)) {
                        Box(Modifier.fillMaxWidth().height(0.5.dp).background(com.ascend.lifeos.ui.theme.Ivory.copy(alpha = 0.05f)))
                    }
                }
            }
        }

        // free slots (ghost, tappable) — optionally tagged with the hour's weather
        val ctx = androidx.compose.ui.platform.LocalContext.current
        val weatherOn = com.ascend.lifeos.data.Prefs.bool(ctx, com.ascend.lifeos.data.Prefs.WEATHER_SLOTS, true)
        LaunchedEffect(weatherOn, t.day) {
            if (weatherOn && t.day == LocalDate.now()) {
                runCatching { com.ascend.lifeos.data.WeatherRepo.refresh(ctx) }
            }
        }
        t.freeSlots.forEach { slot ->
            val topMin = slot.startMin - HOUR_START * 60
            if (topMin >= 0) {
                val weather = if (weatherOn && t.day == LocalDate.now()) {
                    com.ascend.lifeos.data.WeatherRepo.slotTag(slot.startMin)
                } else null
                Box(
                    Modifier.padding(start = 30.dp, end = 2.dp)
                        .offset(y = HOUR_DP * (topMin / 60f) + 2.dp)
                        .fillMaxWidth()
                        .height(HOUR_DP * (slot.durationMin / 60f) - 4.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .border(0.5.dp, Mod.Calendar.copy(alpha = 0.18f), RoundedCornerShape(10.dp))
                        .clickable { onSlotTap(slot) },
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        "+ ${slot.durationMin} min free" + (weather?.let { " · $it" } ?: ""),
                        color = Mod.Calendar.copy(alpha = 0.4f),
                        fontFamily = Display, fontSize = 9.5.sp, fontWeight = FontWeight.Medium, letterSpacing = 1.sp,
                    )
                }
            }
        }

        // event blocks — cancelled lessons render ghosted: visible, but free
        t.blocks.forEach { b ->
            val topMin = (b.startMin - HOUR_START * 60).coerceAtLeast(0)
            val durMin = (b.endMin - b.startMin).coerceAtLeast(20)
            val c = if (b.cancelled) TextDim else eventColor(b.type)
            Box(
                Modifier.padding(start = 30.dp, end = 2.dp)
                    .offset(y = HOUR_DP * (topMin / 60f) + 1.dp)
                    .fillMaxWidth()
                    .height(HOUR_DP * (durMin / 60f) - 2.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(c.copy(alpha = if (b.cancelled) 0.05f else 0.13f))
                    .border(
                        0.5.dp, c.copy(alpha = if (b.cancelled) 0.2f else 0.35f),
                        RoundedCornerShape(10.dp),
                    )
                    .clickable { onBlockTap(b) },
            ) {
                Row(Modifier.fillMaxSize()) {
                    Box(Modifier.width(3.dp).fillMaxHeight().background(c.copy(alpha = if (b.cancelled) 0.35f else 1f)))
                    Column(Modifier.padding(horizontal = 9.dp, vertical = 5.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                b.title,
                                color = if (b.cancelled) TextDim else TextPrimary,
                                fontFamily = Body,
                                fontSize = 12.5.sp, fontWeight = FontWeight.Bold,
                                maxLines = 1, overflow = TextOverflow.Ellipsis,
                                textDecoration = if (b.cancelled) androidx.compose.ui.text.style.TextDecoration.LineThrough else null,
                                modifier = Modifier.weight(1f, fill = false),
                            )
                            if (b.cancelled) {
                                Spacer(Modifier.width(6.dp))
                                Text(
                                    "CANCELLED · FREE", color = Good, fontFamily = Display,
                                    fontSize = 8.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 1.sp,
                                )
                            }
                            if (b.fromDevice) {
                                Spacer(Modifier.width(5.dp))
                                Icon(Icons.Rounded.Link, null, tint = c.copy(alpha = 0.6f), modifier = Modifier.size(11.dp))
                            }
                        }
                        if (durMin >= 40) {
                            Text(
                                "${CalendarRepo.fmtMin(b.startMin)}–${CalendarRepo.fmtMin(b.endMin)} · ${eventLabel(b.type)}",
                                color = TextDim, fontSize = 10.sp, fontFamily = Body, maxLines = 1,
                            )
                        }
                    }
                }
            }
        }

        // now line
        if (t.day == LocalDate.now()) {
            val now = LocalTime.now()
            val nowMin = now.hour * 60 + now.minute - HOUR_START * 60
            if (nowMin in 0..((HOUR_END - HOUR_START) * 60)) {
                Row(
                    Modifier.offset(y = HOUR_DP * (nowMin / 60f)).fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        CalendarRepo.fmtMin(now.hour * 60 + now.minute),
                        color = Crit, fontFamily = Display, fontSize = 8.5.sp, fontWeight = FontWeight.Bold,
                        modifier = Modifier.width(26.dp),
                    )
                    Box(Modifier.size(5.dp).clip(CircleShape).background(Crit))
                    Box(Modifier.weight(1f).height(1.dp).background(Crit.copy(alpha = 0.55f)))
                }
            }
        }
    }
}

// ─── Quick add ───────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun QuickAddSheet(
    day: LocalDate,
    prefillStart: Int?,
    onSave: (String, EventType, LocalDate, LocalDate, Int, Int, Boolean, Int) -> Unit,
    onDismiss: () -> Unit,
) {
    var title by remember { mutableStateOf("") }
    var type by remember { mutableStateOf(EventType.PERSONAL) }
    var startMin by remember { mutableIntStateOf(prefillStart ?: 15 * 60) }
    var endMin by remember { mutableIntStateOf((prefillStart ?: (15 * 60)) + 60) }
    var repeatMask by remember { mutableIntStateOf(0) }
    var holidayDays by remember { mutableIntStateOf(7) }

    val isHoliday = type == EventType.HOLIDAY

    com.ascend.lifeos.ui.kit.JarvisSheet(onDismiss = onDismiss) {
        Column(Modifier.fillMaxWidth().padding(22.dp).navigationBarsPadding()) {
            Text(
                "NEW BLOCK", color = Mod.Calendar, fontFamily = Display,
                fontSize = 10.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 2.5.sp,
            )
            Spacer(Modifier.height(6.dp))
            Text(
                day.format(DateTimeFormatter.ofPattern("EEEE, d MMM", Locale.ENGLISH)),
                color = TextPrimary, fontFamily = Display, fontSize = 19.sp, fontWeight = FontWeight.Bold,
            )
            Spacer(Modifier.height(16.dp))

            // title
            Box(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(13.dp))
                    .background(com.ascend.lifeos.ui.theme.Ivory.copy(alpha = 0.05f))
                    .border(0.5.dp, com.ascend.lifeos.ui.theme.Ivory.copy(alpha = 0.10f), RoundedCornerShape(13.dp))
                    .padding(horizontal = 14.dp, vertical = 13.dp),
            ) {
                if (title.isEmpty()) Text(
                    if (isHoliday) "Holiday name (e.g. Summer break)" else "Title (e.g. School, Shift, Practice)",
                    color = TextDim, fontSize = 14.sp, fontFamily = Body,
                )
                BasicTextField(
                    title, { title = it }, singleLine = true,
                    textStyle = TextStyle(color = TextPrimary, fontSize = 14.sp, fontFamily = Body, fontWeight = FontWeight.SemiBold),
                    cursorBrush = SolidColor(Mod.Calendar), modifier = Modifier.fillMaxWidth(),
                )
            }
            Spacer(Modifier.height(12.dp))

            // type chips
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                EventType.entries.forEach { t ->
                    val c = eventColor(t)
                    val on = t == type
                    Box(
                        Modifier.clip(RoundedCornerShape(10.dp))
                            .background(if (on) c.copy(alpha = 0.15f) else com.ascend.lifeos.ui.theme.Ivory.copy(alpha = 0.04f))
                            .border(0.5.dp, if (on) c.copy(alpha = 0.5f) else com.ascend.lifeos.ui.theme.Ivory.copy(alpha = 0.10f), RoundedCornerShape(10.dp))
                            .clickable { type = t }
                            .padding(horizontal = 11.dp, vertical = 7.dp),
                    ) {
                        Text(eventLabel(t), color = if (on) c else TextMuted, fontSize = 12.sp, fontFamily = Body, fontWeight = FontWeight.Bold)
                    }
                }
            }
            Spacer(Modifier.height(14.dp))

            if (!isHoliday) {
                // time steppers
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    TimeStepper("From", startMin, Modifier.weight(1f)) {
                        startMin = it.coerceIn(0, 23 * 60 + 45)
                        if (endMin <= startMin) endMin = startMin + 30
                    }
                    TimeStepper("To", endMin, Modifier.weight(1f)) {
                        endMin = it.coerceIn(startMin + 15, 24 * 60)
                    }
                }
                Spacer(Modifier.height(14.dp))

                // weekly repeat
                Text(
                    "REPEATS WEEKLY ON", color = TextDim, fontFamily = Display,
                    fontSize = 9.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 1.5.sp,
                )
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                    val letters = listOf("M", "T", "W", "T", "F", "S", "S")
                    for (i in 0..6) {
                        val on = repeatMask and (1 shl i) != 0
                        Box(
                            Modifier.size(34.dp).clip(CircleShape)
                                .background(if (on) Mod.Calendar.copy(alpha = 0.16f) else com.ascend.lifeos.ui.theme.Ivory.copy(alpha = 0.04f))
                                .border(0.5.dp, if (on) Mod.Calendar.copy(alpha = 0.5f) else com.ascend.lifeos.ui.theme.Ivory.copy(alpha = 0.10f), CircleShape)
                                .clickable { repeatMask = repeatMask xor (1 shl i) },
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(letters[i], color = if (on) Mod.Calendar else TextDim, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            } else {
                // holiday length
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Length", color = TextMuted, fontSize = 14.sp, fontFamily = Body, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                    Box(
                        Modifier.size(38.dp).clip(CircleShape).background(com.ascend.lifeos.ui.theme.Ivory.copy(alpha = 0.05f))
                            .border(0.5.dp, com.ascend.lifeos.ui.theme.Ivory.copy(alpha = 0.10f), CircleShape)
                            .clickable { holidayDays = (holidayDays - 1).coerceAtLeast(1) },
                        contentAlignment = Alignment.Center,
                    ) { Text("−", color = TextPrimary, fontSize = 17.sp, fontWeight = FontWeight.Bold) }
                    Text(
                        "$holidayDays days", color = TextPrimary, style = metricStyle(16),
                        modifier = Modifier.widthIn(min = 78.dp), textAlign = TextAlign.Center,
                    )
                    Box(
                        Modifier.size(38.dp).clip(CircleShape).background(com.ascend.lifeos.ui.theme.Ivory.copy(alpha = 0.05f))
                            .border(0.5.dp, com.ascend.lifeos.ui.theme.Ivory.copy(alpha = 0.10f), CircleShape)
                            .clickable { holidayDays += 1 },
                        contentAlignment = Alignment.Center,
                    ) { Text("+", color = TextPrimary, fontSize = 17.sp, fontWeight = FontWeight.Bold) }
                }
            }

            Spacer(Modifier.height(20.dp))
            Box(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(15.dp))
                    .background(Mod.Calendar)
                    .clickable {
                        onSave(
                            title, type, day,
                            if (isHoliday) day.plusDays((holidayDays - 1).toLong()) else day,
                            startMin, endMin, isHoliday, if (isHoliday) 0 else repeatMask,
                        )
                    }
                    .padding(vertical = 14.dp),
                contentAlignment = Alignment.Center,
            ) { Text("Add to timeline", color = Void, fontSize = 14.5.sp, fontFamily = Body, fontWeight = FontWeight.ExtraBold) }
            Spacer(Modifier.height(18.dp))
        }
    }
}

@Composable
private fun TimeStepper(label: String, value: Int, modifier: Modifier = Modifier, onValue: (Int) -> Unit) {
    Column(modifier) {
        Text(
            label.uppercase(), color = TextDim, fontFamily = Display,
            fontSize = 9.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 1.5.sp,
        )
        Spacer(Modifier.height(6.dp))
        Row(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(13.dp))
                .background(com.ascend.lifeos.ui.theme.Ivory.copy(alpha = 0.04f))
                .border(0.5.dp, com.ascend.lifeos.ui.theme.Ivory.copy(alpha = 0.10f), RoundedCornerShape(13.dp))
                .padding(horizontal = 8.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                "−", color = TextMuted, fontSize = 17.sp, fontWeight = FontWeight.Bold,
                modifier = Modifier.clip(CircleShape).clickable { onValue(value - 15) }.padding(horizontal = 10.dp, vertical = 2.dp),
            )
            Text(
                CalendarRepo.fmtMin(value), color = TextPrimary, style = metricStyle(16),
                modifier = Modifier.weight(1f), textAlign = TextAlign.Center,
            )
            Text(
                "+", color = TextMuted, fontSize = 17.sp, fontWeight = FontWeight.Bold,
                modifier = Modifier.clip(CircleShape).clickable { onValue(value + 15) }.padding(horizontal = 10.dp, vertical = 2.dp),
            )
        }
    }
}

// ─── Detail / delete ─────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EventDetailSheet(b: TimelineBlock, onDelete: () -> Unit, onDismiss: () -> Unit) {
    com.ascend.lifeos.ui.kit.JarvisSheet(onDismiss = onDismiss) {
        Column(Modifier.fillMaxWidth().padding(22.dp).navigationBarsPadding()) {
            val c = eventColor(b.type)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(10.dp).clip(CircleShape).background(c))
                Spacer(Modifier.width(10.dp))
                Text(b.title, color = TextPrimary, fontFamily = Display, fontSize = 19.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
            }
            Spacer(Modifier.height(6.dp))
            Text(
                if (b.allDay) "All day · ${eventLabel(b.type)}"
                else "${CalendarRepo.fmtMin(b.startMin)}–${CalendarRepo.fmtMin(b.endMin)} · ${eventLabel(b.type)}",
                color = TextMuted, fontSize = 13.sp, fontFamily = Body,
            )
            Spacer(Modifier.height(14.dp))

            // Weather-dependent flag — learned per title: set once, every
            // same-named/recurring instance inherits it (Ideensammlung).
            val ctx = LocalContext.current
            var outdoorTick by remember { mutableIntStateOf(0) }
            val outdoor = remember(outdoorTick) { com.ascend.lifeos.data.WeatherRepo.isOutdoor(ctx, b.title) }
            Row(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp))
                    .background(if (outdoor) Mod.Calendar.copy(alpha = 0.10f) else com.ascend.lifeos.ui.theme.Ivory.copy(alpha = 0.04f))
                    .border(0.5.dp, if (outdoor) Mod.Calendar.copy(alpha = 0.4f) else com.ascend.lifeos.ui.theme.Ivory.copy(alpha = 0.1f), RoundedCornerShape(12.dp))
                    .clickable { com.ascend.lifeos.data.WeatherRepo.toggleOutdoor(ctx, b.title); outdoorTick++ }
                    .padding(horizontal = 14.dp, vertical = 11.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f)) {
                    Text("Weather-dependent", color = TextPrimary, fontSize = 13.sp, fontFamily = Body, fontWeight = FontWeight.SemiBold)
                    Text(
                        "Applies to every \"${b.title}\" — Jarvis checks the forecast and suggests, never moves.",
                        color = TextDim, fontSize = 10.5.sp, fontFamily = Body, lineHeight = 14.sp,
                    )
                }
                Text(
                    if (outdoor) "ON" else "OFF",
                    color = if (outdoor) Mod.Calendar else TextDim,
                    fontFamily = Display, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.5.sp,
                )
            }
            Spacer(Modifier.height(12.dp))
            Box(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp))
                    .background(Crit.copy(alpha = 0.10f))
                    .border(0.5.dp, Crit.copy(alpha = 0.35f), RoundedCornerShape(14.dp))
                    .clickable(onClick = onDelete)
                    .padding(vertical = 13.dp),
                contentAlignment = Alignment.Center,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.Delete, null, tint = Crit, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Remove from timeline", color = Crit, fontSize = 13.5.sp, fontFamily = Body, fontWeight = FontWeight.Bold)
                }
            }
            Spacer(Modifier.height(18.dp))
        }
    }
}

// ─── WebUntis login sync — for schools that block ICS publishing ────────────

@Composable
private fun UntisRow() {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    var tick by remember { mutableIntStateOf(0) }
    val configured = remember(tick) { UntisSync.configured(ctx) }

    var expanded by remember { mutableStateOf(false) }
    var host by remember { mutableStateOf(UntisSync.host(ctx)) }
    var school by remember { mutableStateOf(UntisSync.school(ctx)) }
    var user by remember { mutableStateOf(UntisSync.user(ctx)) }
    var pass by remember { mutableStateOf("") }
    var syncing by remember { mutableStateOf(false) }
    var status by remember { mutableStateOf<String?>(null) }

    fun runSync() {
        syncing = true; status = null
        scope.launch {
            val r = UntisSync.sync(ctx)
            syncing = false
            status = r.fold(
                onSuccess = { n -> expanded = false; "Imported $n lessons ✓" },
                onFailure = { e -> e.message ?: "Sync failed" },
            )
            tick++
        }
    }

    Panel(Modifier.fillMaxWidth(), corner = 14.dp) {
        Column(Modifier.padding(horizontal = 14.dp, vertical = 11.dp)) {
            Row(
                Modifier.fillMaxWidth().clickable { if (!configured) expanded = !expanded },
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(Icons.Rounded.Sync, null, tint = Mod.Calendar, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        if (configured) "WebUntis · ${UntisSync.user(ctx)}" else "Connect WebUntis login (no ICS needed)",
                        color = if (configured) TextPrimary else TextMuted,
                        fontSize = 12.5.sp, fontFamily = Body, fontWeight = FontWeight.Bold,
                    )
                    val sub = status ?: if (configured) {
                        val last = UntisSync.lastSync(ctx)
                        if (last > 0) "last sync " + java.text.SimpleDateFormat("HH:mm", Locale.getDefault()).format(java.util.Date(last))
                        else "not synced yet"
                    } else "Your timetable imports itself — cancelled lessons excluded"
                    Text(sub, color = TextDim, fontSize = 10.5.sp, fontFamily = Body)
                }
                if (syncing) {
                    CircularProgressIndicator(Modifier.size(14.dp), color = Mod.Calendar, strokeWidth = 1.5.dp)
                } else if (configured) {
                    Text(
                        "SYNC", color = Mod.Calendar, fontFamily = Display, fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold, letterSpacing = 1.5.sp,
                        modifier = Modifier.clip(RoundedCornerShape(8.dp)).clickable { runSync() }.padding(6.dp),
                    )
                    Spacer(Modifier.width(4.dp))
                    Icon(
                        Icons.Rounded.Close, null, tint = TextDim,
                        modifier = Modifier.size(16.dp).clickable {
                            scope.launch { UntisSync.remove(ctx); tick++; status = "Removed" }
                        },
                    )
                }
            }

            if (expanded && !configured) {
                Spacer(Modifier.height(10.dp))
                UntisField("Server", host, "fos-bos-kempten.webuntis.com") { host = it }
                Spacer(Modifier.height(7.dp))
                UntisField("School", school, "fos-bos-kempten") { school = it }
                Spacer(Modifier.height(7.dp))
                UntisField("Username", user, "your Untis login") { user = it }
                Spacer(Modifier.height(7.dp))
                UntisField("Password", pass, "stays on this device", password = true) { pass = it }
                Spacer(Modifier.height(10.dp))
                Box(
                    Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp))
                        .background(if (user.isNotBlank() && pass.isNotBlank()) Mod.Calendar else Mod.Calendar.copy(alpha = 0.25f))
                        .clickable(enabled = user.isNotBlank() && pass.isNotBlank() && !syncing) {
                            UntisSync.save(ctx, host, school, user, pass)
                            runSync()
                        }
                        .padding(vertical = 11.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        if (syncing) "Connecting…" else "Connect & sync",
                        color = Void, fontSize = 13.sp, fontFamily = Body, fontWeight = FontWeight.ExtraBold,
                    )
                }
            }
        }
    }
    Spacer(Modifier.height(12.dp))
}

@Composable
private fun UntisField(label: String, value: String, hint: String, password: Boolean = false, onValue: (String) -> Unit) {
    Column {
        Text(
            label.uppercase(), color = TextDim, fontFamily = Display,
            fontSize = 8.5.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 1.5.sp,
        )
        Spacer(Modifier.height(3.dp))
        Box(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(11.dp))
                .background(com.ascend.lifeos.ui.theme.Ivory.copy(alpha = 0.05f))
                .border(0.5.dp, com.ascend.lifeos.ui.theme.Ivory.copy(alpha = 0.10f), RoundedCornerShape(11.dp))
                .padding(horizontal = 12.dp, vertical = 10.dp),
        ) {
            if (value.isEmpty()) Text(hint, color = TextDim, fontSize = 12.5.sp, fontFamily = Body)
            BasicTextField(
                value = value, onValueChange = onValue, singleLine = true,
                textStyle = TextStyle(color = TextPrimary, fontSize = 12.5.sp, fontFamily = Body, fontWeight = FontWeight.SemiBold),
                cursorBrush = SolidColor(Mod.Calendar),
                visualTransformation = if (password) androidx.compose.ui.text.input.PasswordVisualTransformation()
                else androidx.compose.ui.text.input.VisualTransformation.None,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

// ─── Calendar settings — every data feed in one place, off the main screen ──

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CalendarSettingsSheet(onDismiss: () -> Unit) {
    val ctx = LocalContext.current
    var tick by remember { mutableIntStateOf(0) }
    val calPermission = remember(tick) { CalendarSync.granted(ctx) }
    val permLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { tick++ }

    com.ascend.lifeos.ui.kit.JarvisSheet(onDismiss = onDismiss) {
        Column(
            Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 22.dp)
                .navigationBarsPadding().verticalScroll(rememberScrollState()),
        ) {
            Text(
                "CALENDAR FEEDS", color = Mod.Calendar, fontFamily = Display,
                fontSize = 10.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 3.sp,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                "Every source can be connected or removed here — the timeline itself stays clean.",
                color = TextDim, fontSize = 11.5.sp, fontFamily = Body, lineHeight = 16.sp,
            )
            Spacer(Modifier.height(16.dp))

            // ── device calendar ──────────────────────────────────────
            Panel(Modifier.fillMaxWidth(), corner = 14.dp) {
                Row(Modifier.padding(horizontal = 14.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        Modifier.size(8.dp).clip(CircleShape)
                            .background(if (calPermission) Good else com.ascend.lifeos.ui.theme.Ivory.copy(alpha = 0.15f)),
                    )
                    Spacer(Modifier.width(10.dp))
                    Icon(Icons.Rounded.Link, null, tint = Mod.Calendar, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(10.dp))
                    Column(Modifier.weight(1f)) {
                        Text("Device calendar", color = TextPrimary, fontSize = 12.5.sp, fontFamily = Body, fontWeight = FontWeight.Bold)
                        Text(
                            if (calPermission) "Connected — hockey games merge automatically"
                            else "Your hockey games appear automatically",
                            color = TextDim, fontSize = 10.5.sp, fontFamily = Body,
                        )
                    }
                    if (!calPermission) {
                        Text(
                            "CONNECT", color = Mod.Calendar, fontFamily = Display, fontSize = 10.sp,
                            fontWeight = FontWeight.SemiBold, letterSpacing = 1.5.sp,
                            modifier = Modifier.clip(RoundedCornerShape(8.dp))
                                .clickable { permLauncher.launch(android.Manifest.permission.READ_CALENDAR) }
                                .padding(6.dp),
                        )
                    }
                }
            }
            Spacer(Modifier.height(12.dp))

            // ── WebUntis login (primary for FOS/BOS Kempten) ─────────
            UntisRow()

            // ── ICS feed (fallback for schools that publish one) ─────
            IcsFeedRow()

            Spacer(Modifier.height(10.dp))
        }
    }
}

// ─── Auto time-blocking: tasks → free slots (replaces Motion/Reclaim) ────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TaskBlocksSheet(onDismiss: () -> Unit) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    @Suppress("UNUSED_EXPRESSION") com.ascend.lifeos.data.calendar.TaskBlocks.rev
    val tasks = com.ascend.lifeos.data.calendar.TaskBlocks.tasks(ctx)
    var title by remember { mutableStateOf("") }
    var prio by remember { mutableIntStateOf(2) }
    var durMin by remember { mutableIntStateOf(45) }
    var deadlineDays by remember { mutableIntStateOf(3) }
    var planNote by remember { mutableStateOf<String?>(null) }

    com.ascend.lifeos.ui.kit.JarvisSheet(onDismiss = onDismiss) {
        Column(
            Modifier.fillMaxWidth().navigationBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 22.dp).padding(top = 20.dp, bottom = 22.dp),
        ) {
            Text(
                "TIME BLOCKING", color = Mod.Calendar, fontFamily = Display,
                fontSize = 10.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 3.sp,
            )
            Spacer(Modifier.height(4.dp))
            Text("Tasks find their own slot", color = TextPrimary, fontFamily = Display, fontSize = 20.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(4.dp))
            Text(
                "Priority beats deadline beats duration — transparent rules, fixed events never move.",
                color = TextDim, fontSize = 11.5.sp, fontFamily = Body,
            )
            Spacer(Modifier.height(14.dp))

            // add form
            Box(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(13.dp))
                    .background(com.ascend.lifeos.ui.theme.Ivory.copy(alpha = 0.05f))
                    .border(0.5.dp, com.ascend.lifeos.ui.theme.Ivory.copy(alpha = 0.1f), RoundedCornerShape(13.dp))
                    .padding(horizontal = 14.dp, vertical = 12.dp),
            ) {
                if (title.isEmpty()) Text("Task title…", color = TextDim, fontSize = 14.sp, fontFamily = Body)
                androidx.compose.foundation.text.BasicTextField(
                    value = title, onValueChange = { title = it.take(60) }, singleLine = true,
                    textStyle = androidx.compose.ui.text.TextStyle(color = TextPrimary, fontSize = 14.sp),
                    cursorBrush = androidx.compose.ui.graphics.SolidColor(Mod.Calendar),
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            Spacer(Modifier.height(10.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                listOf(1 to "Low", 2 to "Normal", 3 to "High").forEach { (p, label) ->
                    val sel = prio == p
                    Text(
                        label, color = if (sel) Mod.Calendar else TextMuted,
                        fontSize = 11.5.sp, fontFamily = Body, fontWeight = FontWeight.Bold,
                        modifier = Modifier.clip(RoundedCornerShape(10.dp))
                            .background(if (sel) Mod.Calendar.copy(alpha = 0.14f) else com.ascend.lifeos.ui.theme.Ivory.copy(alpha = 0.04f))
                            .clickable { prio = p }
                            .padding(horizontal = 11.dp, vertical = 6.dp),
                    )
                    Spacer(Modifier.width(7.dp))
                }
            }
            Spacer(Modifier.height(10.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Duration", color = TextMuted, fontSize = 12.sp, fontFamily = Body, modifier = Modifier.weight(1f))
                Text(
                    "−", color = TextMuted, fontSize = 16.sp, fontWeight = FontWeight.Bold,
                    modifier = Modifier.clip(CircleShape).clickable { durMin = (durMin - 15).coerceAtLeast(15) }.padding(horizontal = 10.dp, vertical = 2.dp),
                )
                Text(
                    "$durMin min", color = TextPrimary, fontFamily = Display, fontSize = 14.sp, fontWeight = FontWeight.Bold,
                    modifier = Modifier.width(72.dp), textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                )
                Text(
                    "+", color = TextMuted, fontSize = 16.sp, fontWeight = FontWeight.Bold,
                    modifier = Modifier.clip(CircleShape).clickable { durMin = (durMin + 15).coerceAtMost(240) }.padding(horizontal = 10.dp, vertical = 2.dp),
                )
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Deadline", color = TextMuted, fontSize = 12.sp, fontFamily = Body, modifier = Modifier.weight(1f))
                Text(
                    "−", color = TextMuted, fontSize = 16.sp, fontWeight = FontWeight.Bold,
                    modifier = Modifier.clip(CircleShape).clickable { deadlineDays = (deadlineDays - 1).coerceAtLeast(0) }.padding(horizontal = 10.dp, vertical = 2.dp),
                )
                Text(
                    if (deadlineDays == 0) "today" else "+$deadlineDays d",
                    color = TextPrimary, fontFamily = Display, fontSize = 14.sp, fontWeight = FontWeight.Bold,
                    modifier = Modifier.width(72.dp), textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                )
                Text(
                    "+", color = TextMuted, fontSize = 16.sp, fontWeight = FontWeight.Bold,
                    modifier = Modifier.clip(CircleShape).clickable { deadlineDays = (deadlineDays + 1).coerceAtMost(21) }.padding(horizontal = 10.dp, vertical = 2.dp),
                )
            }
            Spacer(Modifier.height(10.dp))
            Box(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(13.dp))
                    .background(if (title.isBlank()) Mod.Calendar.copy(alpha = 0.25f) else Mod.Calendar)
                    .clickable(enabled = title.isNotBlank()) {
                        com.ascend.lifeos.data.calendar.TaskBlocks.add(
                            ctx, title, prio,
                            java.time.LocalDate.now().plusDays(deadlineDays.toLong()).toEpochDay(), durMin,
                        )
                        title = ""
                    }
                    .padding(vertical = 12.dp),
                contentAlignment = Alignment.Center,
            ) { Text("Add task", color = Void, fontSize = 13.sp, fontFamily = Body, fontWeight = FontWeight.ExtraBold) }

            Spacer(Modifier.height(16.dp))

            tasks.forEach { t ->
                Row(
                    Modifier.fillMaxWidth().padding(vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(
                        Modifier.size(19.dp).clip(CircleShape)
                            .background(if (t.done) Mod.Calendar else Color.Transparent)
                            .border(1.dp, if (t.done) Mod.Calendar else com.ascend.lifeos.ui.theme.Ivory.copy(alpha = 0.25f), CircleShape)
                            .clickable { com.ascend.lifeos.data.calendar.TaskBlocks.setDone(ctx, t.id, !t.done) },
                        contentAlignment = Alignment.Center,
                    ) { if (t.done) Text("✓", color = Void, fontSize = 10.sp, fontWeight = FontWeight.Bold) }
                    Spacer(Modifier.width(11.dp))
                    Column(Modifier.weight(1f)) {
                        Text(
                            t.title,
                            color = if (t.done) TextDim else TextPrimary,
                            fontSize = 13.5.sp, fontFamily = Body, fontWeight = FontWeight.SemiBold,
                        )
                        val due = java.time.LocalDate.ofEpochDay(t.deadlineEpochDay)
                        val sched = if (t.scheduledDay >= 0) {
                            val d = java.time.LocalDate.ofEpochDay(t.scheduledDay)
                            "${d.dayOfMonth}.${d.monthValue}. ${CalendarRepo.fmtMin(t.scheduledStart)}"
                        } else "not placed yet"
                        Text(
                            "P${t.priority} · due ${due.dayOfMonth}.${due.monthValue}. · ${t.durationMin}m · $sched",
                            color = if (t.scheduledDay >= 0 || t.done) TextDim else Warn,
                            fontSize = 10.5.sp, fontFamily = Body,
                        )
                    }
                    Text(
                        "✕", color = TextDim, fontSize = 13.sp,
                        modifier = Modifier.clip(CircleShape)
                            .clickable { scope.launch { com.ascend.lifeos.data.calendar.TaskBlocks.delete(ctx, t.id) } }
                            .padding(6.dp),
                    )
                }
            }
            if (tasks.isEmpty()) {
                Text("No tasks yet — add one above.", color = TextDim, fontSize = 12.sp, fontFamily = Body)
            }

            Spacer(Modifier.height(14.dp))
            Box(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(13.dp))
                    .background(com.ascend.lifeos.ui.theme.Ivory.copy(alpha = 0.06f))
                    .border(0.5.dp, Mod.Calendar.copy(alpha = 0.4f), RoundedCornerShape(13.dp))
                    .clickable(enabled = tasks.any { !it.done }) {
                        planNote = "Planning…"
                        scope.launch {
                            val placed = runCatching {
                                com.ascend.lifeos.data.calendar.TaskBlocks.plan(ctx)
                            }.getOrDefault(0)
                            planNote = "$placed placed into free slots ✓"
                        }
                    }
                    .padding(vertical = 12.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    planNote ?: "Plan now → free slots",
                    color = Mod.Calendar, fontSize = 13.sp, fontFamily = Body, fontWeight = FontWeight.Bold,
                )
            }
            Spacer(Modifier.height(10.dp))
        }
    }
}
