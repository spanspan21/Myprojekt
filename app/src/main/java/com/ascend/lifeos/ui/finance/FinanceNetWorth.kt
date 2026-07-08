package com.ascend.lifeos.ui.finance

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.TrendingDown
import androidx.compose.material.icons.automirrored.rounded.TrendingUp
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ascend.lifeos.data.finance.FinanceStore
import com.ascend.lifeos.data.finance.FinanceStore.HoldingKind
import com.ascend.lifeos.ui.kit.Panel
import com.ascend.lifeos.ui.kit.SectionLabel
import com.ascend.lifeos.ui.theme.Body
import com.ascend.lifeos.ui.theme.Crit
import com.ascend.lifeos.ui.theme.Good
import com.ascend.lifeos.ui.theme.Ivory
import com.ascend.lifeos.ui.theme.MicroLabel
import com.ascend.lifeos.ui.theme.Mod
import com.ascend.lifeos.ui.theme.TextDim
import com.ascend.lifeos.ui.theme.TextMuted
import com.ascend.lifeos.ui.theme.TextPrimary
import com.ascend.lifeos.ui.theme.metricStyle
import com.ascend.lifeos.ui.theme.themeSpec
import java.time.LocalDate
import kotlin.math.abs
import kotlin.math.roundToInt

// ─── FINANCE · net worth ─────────────────────────────────────────────────────
// The balance-sheet head of the money module, in the editorial language: a
// glowing net-worth area chart with range toggles, an all-time stat row, an
// allocation donut, and manual panels for investments, crypto, other assets and
// debt. Prices are manual — the app stays offline and free; the chart grows from
// real daily snapshots (FinanceStore), never fabricated history.

private enum class NwRange(val label: String, val days: Long?) {
    M1("1M", 30), M3("3M", 91), Y1("1Y", 365), ALL("All", null)
}

/** Allocation-class hues — distinct module jewels so classes read apart. */
private val allocColors: List<Color>
    get() = listOf(Mod.Finance, Mod.School, Mod.Guard, Mod.Skills)

// ─── 1 · net-worth hero ──────────────────────────────────────────────────────

@Composable
internal fun NetWorthSection() {
    val ctx = LocalContext.current
    val rev = FinanceStore.rev
    val snaps = remember(rev) { FinanceStore.snapshots(ctx) }
    val nw = remember(rev) { FinanceStore.netWorthCents(ctx) }
    val extremes = remember(rev) { FinanceStore.netWorthExtremes(ctx) }
    var range by remember { mutableStateOf(NwRange.ALL) }
    val today = LocalDate.now().toEpochDay()

    val pts = remember(snaps, range) {
        val from = range.days?.let { today - it }
        (if (from == null) snaps else snaps.filter { it.first >= from }).ifEmpty { snaps.takeLast(1) }
    }
    val startVal = pts.firstOrNull()?.second ?: nw
    val delta = nw - startVal
    val pct = if (startVal != 0L) delta * 100.0 / abs(startVal) else 0.0
    val up = delta >= 0

    Panel(
        Modifier.fillMaxWidth(), corner = 22.dp,
        fill = FinAccent.copy(alpha = 0.05f), line = FinAccent.copy(alpha = 0.22f), lux = true,
    ) {
        Column(Modifier.padding(18.dp)) {
            Overline("Net worth")
            Spacer(Modifier.height(5.dp))
            Text(euros(nw), color = TextPrimary, style = metricStyle(36))
            if (pts.size >= 2 && delta != 0L) {
                Spacer(Modifier.height(5.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        if (up) Icons.AutoMirrored.Rounded.TrendingUp else Icons.AutoMirrored.Rounded.TrendingDown,
                        null, tint = if (up) Good else Crit, modifier = Modifier.size(15.dp),
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        "${signedEuros(delta)}  ·  ${if (up) "+" else "−"}${"%.1f".format(abs(pct))}%",
                        color = if (up) Good else Crit,
                        fontSize = 12.sp, fontFamily = Body, fontWeight = FontWeight.Bold,
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(range.label, color = TextDim, fontFamily = MicroLabel, fontSize = 10.sp, letterSpacing = 1.sp)
                }
            }

            Spacer(Modifier.height(16.dp))
            NetWorthChart(pts, FinAccent, Modifier.fillMaxWidth().height(140.dp))
            Spacer(Modifier.height(12.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                NwRange.values().forEach { r -> FinChip(r.label, r == range) { range = r } }
            }

            Spacer(Modifier.height(14.dp))
            Box(Modifier.fillMaxWidth().height(0.5.dp).background(Ivory.copy(alpha = 0.08f)))
            Spacer(Modifier.height(12.dp))
            Row(Modifier.fillMaxWidth()) {
                NwStat("1% of NW", euros(nw / 100), Modifier.weight(1f))
                NwStat("All-time high", euros(extremes.first), Modifier.weight(1f))
                NwStat("All-time low", euros(extremes.second), Modifier.weight(1f))
                NwStat("Snapshots", snaps.size.toString(), Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun NwStat(label: String, value: String, modifier: Modifier = Modifier) {
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            value, color = TextPrimary, style = metricStyle(13),
            maxLines = 1, overflow = TextOverflow.Ellipsis,
        )
        Spacer(Modifier.height(3.dp))
        Text(
            label.uppercase(), color = TextDim, fontFamily = MicroLabel,
            fontSize = 8.sp, fontWeight = FontWeight.Medium, letterSpacing = 1.sp,
            maxLines = 1, overflow = TextOverflow.Ellipsis,
        )
    }
}

/** Glowing net-worth area chart: gradient fill, soft glow line, endpoint dot,
 *  a faint baseline grid. Handles a single snapshot as a flat honest line. */
@Composable
private fun NetWorthChart(points: List<Pair<Long, Long>>, color: Color, modifier: Modifier) {
    val glowF = themeSpec.value.glow
    Canvas(modifier) {
        if (points.isEmpty()) return@Canvas
        val ys = points.map { it.second }
        val minV = ys.min(); val maxV = ys.max()
        val span = (maxV - minV).toFloat().takeIf { it > 0f } ?: 1f
        val days = points.map { it.first }
        val minD = days.min(); val maxD = days.max()
        val dSpan = (maxD - minD).toFloat().takeIf { it > 0f } ?: 1f
        val padY = size.height * 0.14f
        fun y(v: Long) = size.height - padY - ((v - minV) / span) * (size.height - padY * 2)

        // faint baseline grid
        val grid = Ivory.copy(alpha = 0.05f)
        for (g in 1..3) {
            val gy = size.height / 4f * g
            drawLine(grid, Offset(0f, gy), Offset(size.width, gy), 1f)
        }

        val screen = if (points.size == 1) {
            val yy = y(ys[0])
            listOf(Offset(0f, yy), Offset(size.width, yy))
        } else {
            points.map { Offset((it.first - minD) / dSpan * size.width, y(it.second)) }
        }

        val line = Path().apply {
            screen.forEachIndexed { i, p -> if (i == 0) moveTo(p.x, p.y) else lineTo(p.x, p.y) }
        }
        val area = Path().apply {
            addPath(line)
            lineTo(screen.last().x, size.height)
            lineTo(screen.first().x, size.height)
            close()
        }
        drawPath(area, Brush.verticalGradient(listOf(color.copy(alpha = 0.24f), Color.Transparent)))
        if (glowF > 0f) drawPath(line, color.copy(alpha = 0.20f * glowF), style = Stroke(6.dp.toPx(), cap = StrokeCap.Round))
        drawPath(line, color, style = Stroke(2.2.dp.toPx(), cap = StrokeCap.Round))
        val end = screen.last()
        drawCircle(color.copy(alpha = 0.22f), 7.dp.toPx(), end)
        drawCircle(color, 3.5.dp.toPx(), end)
    }
}

// ─── 2 · allocation donut ────────────────────────────────────────────────────

@Composable
internal fun AllocationSection() {
    val ctx = LocalContext.current
    val rev = FinanceStore.rev
    val alloc = remember(rev) { FinanceStore.allocation(ctx) }
    val total = alloc.sumOf { it.second }
    if (alloc.isEmpty() || total <= 0L) return

    Panel(Modifier.fillMaxWidth(), corner = 20.dp) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            CategoryDonut(
                alloc.mapIndexed { i, (_, v) -> allocColors[i % allocColors.size] to v },
                Modifier.size(130.dp), ringWidth = 18.dp,
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(euros(total), color = TextPrimary, style = metricStyle(14))
                    Text(
                        "TOTAL", color = TextDim, fontFamily = MicroLabel,
                        fontSize = 7.5.sp, fontWeight = FontWeight.Medium, letterSpacing = 1.5.sp,
                    )
                }
            }
            Spacer(Modifier.width(18.dp))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(9.dp)) {
                alloc.forEachIndexed { i, (name, v) ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(8.dp).clip(CircleShape).background(allocColors[i % allocColors.size]))
                        Spacer(Modifier.width(8.dp))
                        Text(
                            name, color = TextMuted, fontSize = 12.sp,
                            fontFamily = Body, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f),
                        )
                        Text("${(v * 100.0 / total).roundToInt()}%", color = TextDim, style = metricStyle(11))
                    }
                }
            }
        }
    }
}

// ─── 3 · holdings panels (investments / crypto / other / debt) ───────────────

@Composable
internal fun HoldingsSection(kind: HoldingKind, title: String, number: Int) {
    val ctx = LocalContext.current
    val rev = FinanceStore.rev
    val items = remember(rev) { FinanceStore.holdingsOf(ctx, kind) }
    val total = items.sumOf { it.valueCents }
    var showAdd by remember { mutableStateOf(false) }
    var edit by remember { mutableStateOf<FinanceStore.Holding?>(null) }

    val subtotal = when {
        total <= 0L -> null
        kind == HoldingKind.DEBT -> "−${euros(total)}"
        else -> euros(total)
    }
    SectionLabel(if (subtotal != null) "$title · $subtotal" else title, number = number, accent = FinAccent)
    Spacer(Modifier.height(8.dp))

    if (items.isEmpty()) {
        // compact — a single ghost row per empty class (reference "+ add" idiom)
        AddRowButton("Add ${singular(kind)}") { showAdd = true }
    } else {
        Panel(Modifier.fillMaxWidth(), corner = 20.dp) {
            Column(Modifier.padding(horizontal = 14.dp, vertical = 6.dp)) {
                items.forEachIndexed { i, h ->
                    if (i > 0) Box(Modifier.fillMaxWidth().height(0.5.dp).background(Ivory.copy(alpha = 0.06f)))
                    HoldingRow(h, kind, onEdit = { edit = h }, onDelete = { FinanceStore.deleteHolding(ctx, h.id) })
                }
            }
        }
        Spacer(Modifier.height(8.dp))
        AddRowButton("Add ${singular(kind)}") { showAdd = true }
    }

    if (showAdd) HoldingSheet(kind, null) { showAdd = false }
    edit?.let { HoldingSheet(kind, it) { edit = null } }
}

@Composable
private fun HoldingRow(
    h: FinanceStore.Holding,
    kind: HoldingKind,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
) {
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).clickable(onClick = onEdit)
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // ticker / initial chip
        Box(
            Modifier.size(34.dp).clip(RoundedCornerShape(10.dp)).background(FinAccent.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                h.name.take(4).uppercase(), color = FinAccent, fontFamily = MicroLabel,
                fontSize = 8.5.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.4.sp,
                maxLines = 1,
            )
        }
        Spacer(Modifier.width(11.dp))
        Column(Modifier.weight(1f)) {
            Text(
                h.name, color = TextPrimary, fontSize = 13.5.sp,
                fontFamily = Body, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis,
            )
            val sub = when (kind) {
                HoldingKind.STOCK -> "${trimNum(h.units)} sh · ${euros(h.priceCents)}"
                HoldingKind.CRYPTO -> "${trimNum(h.units)} · ${euros(h.priceCents)}"
                else -> null
            }
            if (sub != null) {
                Text(sub, color = TextDim, fontFamily = MicroLabel, fontSize = 10.sp, letterSpacing = 0.3.sp)
            }
        }
        Spacer(Modifier.width(10.dp))
        Text(
            (if (kind == HoldingKind.DEBT) "−" else "") + euros(h.valueCents),
            color = if (kind == HoldingKind.DEBT) Crit else TextPrimary, style = metricStyle(13),
        )
        Spacer(Modifier.width(10.dp))
        ArmedDelete(onDelete = onDelete)
    }
}

// ─── add / edit sheet ────────────────────────────────────────────────────────

@Composable
internal fun HoldingSheet(kind: HoldingKind, existing: FinanceStore.Holding?, onDismiss: () -> Unit) {
    val ctx = LocalContext.current
    val priced = kind == HoldingKind.STOCK || kind == HoldingKind.CRYPTO
    var name by remember { mutableStateOf(existing?.name ?: "") }
    var units by remember { mutableStateOf(existing?.let { trimNum(it.units) } ?: "") }
    // priced: price per unit; lump: whole value
    var amount by remember {
        mutableStateOf(
            existing?.let { if (priced) centsToInput(it.priceCents) else centsToInput(it.valueCents) } ?: "",
        )
    }

    val unitsVal = parseUnits(units) ?: if (priced) null else 1.0
    val amountCents = parseCentsLoose(amount)
    val valueCents = if (priced) {
        if (unitsVal != null && amountCents != null) Math.round(unitsVal * amountCents) else null
    } else amountCents
    val valid = name.isNotBlank() && valueCents != null && (!priced || unitsVal != null)

    SheetShell(if (existing != null) "Edit ${singular(kind)}" else "Add ${singular(kind)}", onDismiss) {
        GlassField(name, { name = it }, nameHint(kind))
        if (priced) {
            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Box(Modifier.weight(1f)) {
                    GlassField(units, { units = it }, if (kind == HoldingKind.STOCK) "Shares" else "Amount", keyboard = androidx.compose.ui.text.input.KeyboardType.Decimal)
                }
                Box(Modifier.weight(1f)) {
                    GlassField(amount, { amount = it }, "Price / unit €", keyboard = androidx.compose.ui.text.input.KeyboardType.Decimal)
                }
            }
        } else {
            Spacer(Modifier.height(10.dp))
            GlassField(amount, { amount = it }, "Value €", keyboard = androidx.compose.ui.text.input.KeyboardType.Decimal)
        }

        Spacer(Modifier.height(8.dp))
        Text(
            if (valueCents != null) "= ${if (kind == HoldingKind.DEBT) "−" else ""}${euros(valueCents)}" else "Manual value — no live prices",
            color = if (valueCents != null) FinAccent else TextDim,
            fontSize = 11.5.sp, fontFamily = Body, fontWeight = FontWeight.Bold,
        )

        Spacer(Modifier.height(16.dp))
        ActionButton(if (existing != null) "Save" else "Add", enabled = valid) {
            val u = if (priced) (unitsVal ?: 0.0) else 1.0
            val price = if (priced) (amountCents ?: 0L) else (valueCents ?: 0L)
            if (existing != null) FinanceStore.updateHolding(ctx, existing.id, name, u, price)
            else FinanceStore.addHolding(ctx, kind, name, u, price)
            onDismiss()
        }
        existing?.let {
            Spacer(Modifier.height(8.dp))
            Box(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(13.dp)).clickable {
                    FinanceStore.deleteHolding(ctx, it.id); onDismiss()
                }.padding(vertical = 11.dp),
                contentAlignment = Alignment.Center,
            ) { Text("Delete", color = Crit, fontSize = 13.sp, fontFamily = Body, fontWeight = FontWeight.Bold) }
        }
    }
}

// ─── small helpers ───────────────────────────────────────────────────────────

private fun singular(kind: HoldingKind) = when (kind) {
    HoldingKind.STOCK -> "investment"
    HoldingKind.CRYPTO -> "coin"
    HoldingKind.OTHER -> "asset"
    HoldingKind.DEBT -> "debt"
}

private fun nameHint(kind: HoldingKind) = when (kind) {
    HoldingKind.STOCK -> "Ticker (VWCE, VOO …)"
    HoldingKind.CRYPTO -> "Coin (BTC, ETH …)"
    HoldingKind.OTHER -> "Asset name (Home, Car …)"
    HoldingKind.DEBT -> "Liability (Student loan …)"
}

/** "12", "0.05", "1.5" — trims trailing zeros. */
private fun trimNum(v: Double): String =
    if (v == v.toLong().toDouble()) v.toLong().toString()
    else v.toString().trimEnd('0').trimEnd('.')

private fun parseUnits(raw: String): Double? =
    raw.trim().replace(',', '.').toDoubleOrNull()?.takeIf { it > 0.0 }

private fun centsToInput(cents: Long): String =
    if (cents % 100 == 0L) (cents / 100).toString() else "%.2f".format(cents / 100.0)
