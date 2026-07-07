package com.ascend.lifeos.ui.home

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import com.ascend.lifeos.ui.motion.Motion
import com.ascend.lifeos.ui.motion.pressScale
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.MonitorWeight
import androidx.compose.material.icons.rounded.Restaurant
import androidx.compose.material.icons.rounded.ShoppingBag
import androidx.compose.material.icons.rounded.WaterDrop
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ascend.lifeos.core.todayKey
import com.ascend.lifeos.data.Prefs
import com.ascend.lifeos.data.Repo
import com.ascend.lifeos.data.life.LifeStores
import com.ascend.lifeos.ui.theme.*
import kotlinx.coroutines.delay
import kotlin.math.roundToLong

// ─── QUICK LOG — the fastest path from real life into the system ────────────
// One orb on Home opens this sheet. Purchase: type 4.50 → tap Food → Save.
// Water: one tap per glass, sheet stays open. Meal jumps to Fuel. Weight is a
// two-tap stepper. Everything haptic, everything under three seconds.

private val FinAccent = Color(0xFF9CC24A)

private enum class QlMode { ACTIONS, PURCHASE, WEIGHT, DONE }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuickLogSheet(onDismiss: () -> Unit, onOpenModule: (String) -> Unit) {
    val ctx = LocalContext.current
    var mode by remember { mutableStateOf(QlMode.ACTIONS) }

    com.ascend.lifeos.ui.kit.JarvisSheet(onDismiss = onDismiss) {
        Column(
            Modifier.fillMaxWidth()
                .padding(horizontal = 20.dp)
                .navigationBarsPadding()
                .imePadding()
                .padding(bottom = 18.dp),
        ) {
            Spacer(Modifier.height(18.dp))
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    when (mode) {
                        QlMode.PURCHASE -> "LOG PURCHASE"
                        QlMode.WEIGHT -> "LOG WEIGHT"
                        else -> "QUICK LOG"
                    },
                    color = Mod.Home, fontFamily = Display, fontSize = 10.sp,
                    fontWeight = FontWeight.SemiBold, letterSpacing = 2.5.sp,
                )
                Spacer(Modifier.weight(1f))
                if (mode == QlMode.PURCHASE || mode == QlMode.WEIGHT) {
                    Text(
                        "BACK", color = TextDim, fontFamily = Display, fontSize = 9.5.sp,
                        fontWeight = FontWeight.SemiBold, letterSpacing = 1.5.sp,
                        modifier = Modifier.clip(RoundedCornerShape(8.dp))
                            .clickable { mode = QlMode.ACTIONS }
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                    )
                }
            }
            Spacer(Modifier.height(14.dp))

            AnimatedContent(
                mode, label = "ql",
                transitionSpec = { fadeIn(tween(180)) togetherWith fadeOut(tween(120)) },
            ) { m ->
                when (m) {
                    QlMode.ACTIONS -> ActionsPane(
                        ctx = ctx,
                        onPurchase = { mode = QlMode.PURCHASE },
                        onWeight = { mode = QlMode.WEIGHT },
                        onMeal = { onDismiss(); onOpenModule("fuel") },
                    )
                    QlMode.PURCHASE -> PurchasePane(ctx) { mode = QlMode.DONE }
                    QlMode.WEIGHT -> WeightPane(ctx) { mode = QlMode.DONE }
                    QlMode.DONE -> DonePane(onDismiss)
                }
            }
            Spacer(Modifier.height(8.dp))
        }
    }
}

// ─── actions row ─────────────────────────────────────────────────────────────

@Composable
private fun ActionsPane(
    ctx: Context,
    onPurchase: () -> Unit,
    onWeight: () -> Unit,
    onMeal: () -> Unit,
) {
    val water = Repo.data.days[todayKey()]?.water ?: 0
    val waterGoal = Repo.data.profile.waterGoal
    val lastKg = remember { Repo.weightLog().lastOrNull()?.kg ?: Repo.data.profile.weightKg.toDouble() }

    // satisfying tick on every water tap
    var waterTick by remember { mutableIntStateOf(0) }
    val waterScale = remember { Animatable(1f) }
    LaunchedEffect(waterTick) {
        if (waterTick > 0) {
            waterScale.snapTo(1.3f)
            waterScale.animateTo(1f, spring(dampingRatio = Spring.DampingRatioMediumBouncy))
        }
    }

    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(9.dp)) {
        QlTile(
            Icons.Rounded.ShoppingBag, "Purchase", "€", FinAccent,
            onClick = onPurchase,
        )
        QlTile(
            Icons.Rounded.WaterDrop, "Water +1", "$water/$waterGoal", Mod.Body,
            iconScale = waterScale.value,
            statColor = if (water >= waterGoal) Good else Mod.Body,
            onClick = {
                Repo.addWater(1)
                haptic(ctx, 18)
                waterTick++
            },
        )
        QlTile(
            Icons.Rounded.Restaurant, "Meal", "kcal", Mod.Fuel,
            onClick = onMeal,
        )
        QlTile(
            Icons.Rounded.MonitorWeight, "Weight", "%.1f kg".format(lastKg), Mod.Body,
            onClick = onWeight,
        )
    }
}

@Composable
private fun RowScope.QlTile(
    icon: ImageVector,
    label: String,
    stat: String,
    accent: Color,
    iconScale: Float = 1f,
    statColor: Color = TextDim,
    onClick: () -> Unit,
) {
    Column(
        Modifier.weight(1f)
            .clip(RoundedCornerShape(16.dp))
            .background(com.ascend.lifeos.ui.theme.Ivory.copy(alpha = 0.04f))
            .border(0.5.dp, com.ascend.lifeos.ui.theme.Ivory.copy(alpha = 0.10f), RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 15.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(icon, null, tint = accent, modifier = Modifier.size(22.dp).scale(iconScale))
        Spacer(Modifier.height(9.dp))
        Text(
            label, color = TextPrimary, fontFamily = Body,
            fontSize = 11.5.sp, fontWeight = FontWeight.Bold, maxLines = 1,
        )
        Spacer(Modifier.height(2.dp))
        Text(stat, color = statColor, style = metricStyle(10, FontWeight.SemiBold), maxLines = 1)
    }
}

// ─── purchase — the fastest path ─────────────────────────────────────────────

@Composable
private fun PurchasePane(ctx: Context, onSaved: () -> Unit) {
    var amount by remember { mutableStateOf("") }
    var cat by remember { mutableStateOf<String?>(null) }
    var note by remember { mutableStateOf("") }
    val focus = remember { FocusRequester() }
    LaunchedEffect(Unit) { delay(250); runCatching { focus.requestFocus() } }

    val cents = amount.toDoubleOrNull()?.let { (it * 100).roundToLong() } ?: 0L
    val canSave = cents > 0 && cat != null

    Column {
        // big tabular amount
        Box(
            Modifier.fillMaxWidth()
                .clip(RoundedCornerShape(18.dp))
                .background(com.ascend.lifeos.ui.theme.Ivory.copy(alpha = 0.04f))
                .border(0.5.dp, if (cents > 0) FinAccent.copy(alpha = 0.45f) else com.ascend.lifeos.ui.theme.Ivory.copy(alpha = 0.10f), RoundedCornerShape(18.dp))
                .padding(vertical = 18.dp),
            contentAlignment = Alignment.Center,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(contentAlignment = Alignment.Center) {
                    if (amount.isEmpty()) {
                        Text("0.00", color = TextDim, style = metricStyle(34))
                    }
                    BasicTextField(
                        value = amount,
                        onValueChange = { raw ->
                            val s = raw.replace(',', '.').filter { it.isDigit() || it == '.' }
                            if (s.count { it == '.' } <= 1 && s.length <= 8) amount = s
                        },
                        singleLine = true,
                        textStyle = metricStyle(34).copy(color = TextPrimary, textAlign = TextAlign.Center),
                        cursorBrush = SolidColor(FinAccent),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.widthIn(min = 96.dp, max = 200.dp).focusRequester(focus),
                    )
                }
                Spacer(Modifier.width(6.dp))
                Text("€", color = TextMuted, style = metricStyle(22))
            }
        }

        Spacer(Modifier.height(12.dp))

        // category chips — Income excluded, this is the spend lane
        LifeStores.CATEGORIES.filter { it != "Income" }.chunked(3).forEach { rowCats ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                rowCats.forEach { c ->
                    val on = cat == c
                    val bg by animateColorAsState(if (on) FinAccent.copy(alpha = 0.14f) else com.ascend.lifeos.ui.theme.Ivory.copy(alpha = 0.04f), tween(Motion.quick), label = "qcB")
                    val edge by animateColorAsState(if (on) FinAccent.copy(alpha = 0.5f) else com.ascend.lifeos.ui.theme.Ivory.copy(alpha = 0.10f), tween(Motion.quick), label = "qcE")
                    val fg by animateColorAsState(if (on) FinAccent else TextMuted, tween(Motion.quick), label = "qcF")
                    Box(
                        Modifier.weight(1f)
                            .pressScale { cat = c }
                            .clip(RoundedCornerShape(12.dp))
                            .background(bg)
                            .border(0.5.dp, edge, RoundedCornerShape(12.dp))
                            .padding(vertical = 11.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            c, color = fg,
                            fontSize = 12.5.sp, fontFamily = Body, fontWeight = FontWeight.Bold,
                        )
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
        }

        Spacer(Modifier.height(2.dp))

        // optional note
        Box(
            Modifier.fillMaxWidth()
                .clip(RoundedCornerShape(13.dp))
                .background(com.ascend.lifeos.ui.theme.Ivory.copy(alpha = 0.03f))
                .border(0.5.dp, com.ascend.lifeos.ui.theme.Ivory.copy(alpha = 0.09f), RoundedCornerShape(13.dp))
                .padding(horizontal = 14.dp, vertical = 11.dp),
        ) {
            if (note.isEmpty()) Text("note (optional)", color = TextDim, fontSize = 12.5.sp, fontFamily = Body)
            BasicTextField(
                value = note,
                onValueChange = { if (it.length <= 60) note = it },
                singleLine = true,
                textStyle = TextStyle(color = TextPrimary, fontSize = 12.5.sp, fontFamily = Body, fontWeight = FontWeight.Medium),
                cursorBrush = SolidColor(FinAccent),
                modifier = Modifier.fillMaxWidth(),
            )
        }

        Spacer(Modifier.height(14.dp))

        Box(
            Modifier.fillMaxWidth()
                .clip(RoundedCornerShape(15.dp))
                .background(if (canSave) FinAccent else FinAccent.copy(alpha = 0.18f))
                .clickable(enabled = canSave) {
                    // one booking entry point: FinanceStore bumps its rev too,
                    // so Finance UI refreshes without relying on double-subscribe
                    com.ascend.lifeos.data.finance.FinanceStore.bookTxn(ctx, -cents, cat ?: "Other", note)
                    haptic(ctx, 24)
                    onSaved()
                }
                .padding(vertical = 14.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                "Save", color = if (canSave) Void else TextDim,
                fontSize = 14.5.sp, fontFamily = Body, fontWeight = FontWeight.ExtraBold,
            )
        }
    }
}

// ─── weight — last value ± steppers ──────────────────────────────────────────

@Composable
private fun WeightPane(ctx: Context, onSaved: () -> Unit) {
    var kg by remember {
        mutableDoubleStateOf(Repo.weightLog().lastOrNull()?.kg ?: Repo.data.profile.weightKg.toDouble())
    }
    Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        Spacer(Modifier.height(6.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            QlStep("−1") { kg = (kg - 1.0).coerceAtLeast(30.0) }
            Spacer(Modifier.width(8.dp))
            QlStep("−.1") { kg = (kg - 0.1).coerceAtLeast(30.0) }
            Text(
                "%.1f".format(kg), color = TextPrimary, style = metricStyle(38),
                modifier = Modifier.widthIn(min = 112.dp), textAlign = TextAlign.Center,
            )
            QlStep("+.1") { kg = (kg + 0.1).coerceAtMost(250.0) }
            Spacer(Modifier.width(8.dp))
            QlStep("+1") { kg = (kg + 1.0).coerceAtMost(250.0) }
        }
        Spacer(Modifier.height(4.dp))
        Text("kilograms", color = TextDim, fontSize = 11.sp, fontFamily = Body)
        Spacer(Modifier.height(18.dp))
        Box(
            Modifier.fillMaxWidth()
                .clip(RoundedCornerShape(15.dp))
                .background(Mod.Body)
                .clickable {
                    Repo.logWeight(kg)
                    haptic(ctx, 24)
                    onSaved()
                }
                .padding(vertical = 14.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text("Save", color = Void, fontSize = 14.5.sp, fontFamily = Body, fontWeight = FontWeight.ExtraBold)
        }
    }
}

@Composable
private fun QlStep(label: String, onClick: () -> Unit) {
    Box(
        Modifier.size(44.dp).clip(CircleShape)
            .background(com.ascend.lifeos.ui.theme.Ivory.copy(alpha = 0.05f))
            .border(0.5.dp, com.ascend.lifeos.ui.theme.Ivory.copy(alpha = 0.10f), CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(label, color = TextPrimary, fontSize = 13.sp, fontFamily = Body, fontWeight = FontWeight.Bold)
    }
}

// ─── logged ✓ ────────────────────────────────────────────────────────────────

@Composable
private fun DonePane(onDismiss: () -> Unit) {
    LaunchedEffect(Unit) { delay(650); onDismiss() }
    Column(
        Modifier.fillMaxWidth().padding(vertical = 26.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            Modifier.size(52.dp).clip(CircleShape)
                .background(Good.copy(alpha = 0.14f))
                .border(0.5.dp, Good.copy(alpha = 0.4f), CircleShape),
            contentAlignment = Alignment.Center,
        ) { Text("✓", color = Good, fontSize = 22.sp, fontWeight = FontWeight.Bold) }
        Spacer(Modifier.height(10.dp))
        Text("Logged ✓", color = TextPrimary, fontSize = 14.sp, fontFamily = Body, fontWeight = FontWeight.Bold)
    }
}

// ─── haptics — ActiveWorkout pattern, gated on the setting ───────────────────

private fun haptic(ctx: Context, ms: Long) {
    if (!Prefs.bool(ctx, Prefs.HAPTICS_ON, true)) return
    try {
        val vib = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            (ctx.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager).defaultVibrator
        } else {
            @Suppress("DEPRECATION") ctx.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vib.vibrate(VibrationEffect.createOneShot(ms, VibrationEffect.DEFAULT_AMPLITUDE))
        } else {
            @Suppress("DEPRECATION") vib.vibrate(ms)
        }
    } catch (_: Exception) {}
}

/** Cross-screen signals into Home (widget deep link → quick-log sheet). */
object HomeSignals {
    val quickLog = androidx.compose.runtime.mutableStateOf(false)
}
