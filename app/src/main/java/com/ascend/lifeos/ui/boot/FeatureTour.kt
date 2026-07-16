package com.ascend.lifeos.ui.boot

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.FitnessCenter
import androidx.compose.material.icons.rounded.Hexagon
import androidx.compose.material.icons.rounded.Psychology
import androidx.compose.material.icons.rounded.Restaurant
import androidx.compose.material.icons.rounded.SelfImprovement
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ascend.lifeos.ui.motion.Motion
import com.ascend.lifeos.ui.motion.pressScale
import com.ascend.lifeos.ui.theme.Body
import com.ascend.lifeos.ui.theme.Display
import com.ascend.lifeos.ui.theme.FS
import com.ascend.lifeos.ui.theme.Ivory
import com.ascend.lifeos.ui.theme.Mod
import com.ascend.lifeos.ui.theme.TextDim
import com.ascend.lifeos.ui.theme.TextMuted
import com.ascend.lifeos.ui.theme.TextPrimary
import com.ascend.lifeos.ui.theme.Void
import kotlinx.coroutines.delay
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

private data class TourSlide(
    val overline: String,
    val title: String,
    val body: String,
    val accent: Color,
    val icon: ImageVector,
    val features: List<String>,
)

private val SLIDES = listOf(
    TourSlide(
        overline = "COMMAND CENTER",
        title = "Today at a glance",
        body = "Three daily missions — Train, Fuel, Water. Close them all to keep your streak alive. JARVIS reads your day and tells you what matters right now.",
        accent = Color(0xFF2563FF),
        icon = Icons.Rounded.Hexagon,
        features = listOf("Daily missions & streak", "Smart briefing", "Quick log in 3 seconds", "Command palette"),
    ),
    TourSlide(
        overline = "TRAINING ENGINE",
        title = "Train smarter, not harder",
        body = "Evidence-based plans with progressive overload, recovery tracking and skill progressions. Every set is prescribed — volume, effort and rest are computed for you.",
        accent = Color(0xFFFF5A45),
        icon = Icons.Rounded.FitnessCenter,
        features = listOf("Auto-generated plans", "Progressive overload", "Muscle recovery map", "Superset pairing"),
    ),
    TourSlide(
        overline = "NUTRITION",
        title = "Fuel with precision",
        body = "Log food by search, barcode or your own recipes. Adaptive calorie targets learn from your real metabolism. A weekly coach check-in keeps your macros honest.",
        accent = Color(0xFF00B888),
        icon = Icons.Rounded.Restaurant,
        features = listOf("345+ foods & recipes", "Barcode scanner", "Adaptive TDEE", "Food quality scoring"),
    ),
    TourSlide(
        overline = "DIGITAL WELLBEING",
        title = "Guard your focus",
        body = "Hard screen-time limits that actually hold. Morning blocks, wind-down rules, app-specific walls — with a casino where you can gamble for unlock time.",
        accent = Color(0xFFF0A62E),
        icon = Icons.Rounded.Shield,
        features = listOf("Per-app limits", "Focus sessions", "Doomscroll detection", "Screen-time casino"),
    ),
    TourSlide(
        overline = "LIFE SYSTEM",
        title = "Everything in one place",
        body = "Calendar with auto-scheduling, habit tracking, sleep protocol, finance with net worth, school timetable, flashcards — and Prime ties it all together into one daily index.",
        accent = Color(0xFF7C5CFF),
        icon = Icons.Rounded.CalendarMonth,
        features = listOf("Calendar & time blocks", "Habit streaks", "Sleep & recovery", "Finance tracker"),
    ),
    TourSlide(
        overline = "INTELLIGENCE",
        title = "JARVIS learns you",
        body = "Cross-module correlations, anomaly detection, forecasts — all from your own data. Prime gives you one number and three next actions. No guesswork.",
        accent = Color(0xFF4F5BD5),
        icon = Icons.Rounded.Psychology,
        features = listOf("Prime readiness index", "Real correlations", "Smart directives", "Weekly report"),
    ),
    TourSlide(
        overline = "TOOLKIT",
        title = "Tools that replace apps",
        body = "Breathing exercises with four patterns, stopwatch & countdown timer, quick notes, journal prompts — everything built in so you never need another app.",
        accent = Color(0xFF4ECDC4),
        icon = Icons.Rounded.SelfImprovement,
        features = listOf("Box & 4-7-8 breathing", "Timer & stopwatch", "Wind-down evening routine", "Quick notes & nap log"),
    ),
    TourSlide(
        overline = "YOUR RULES",
        title = "Everything is configurable",
        body = "Tweak every threshold, every color boundary, every notification. Score thresholds, sleep debt warnings, step goals, water size — all in Settings. JARVIS adapts to you, not the other way around.",
        accent = Color(0xFFE8A53A),
        icon = Icons.Rounded.Settings,
        features = listOf("Adjustable score thresholds", "Custom notification schedule", "Five visual themes", "Per-module accent colors"),
    ),
)

@Composable
fun FeatureTour(onComplete: () -> Unit) {
    val ftCtx = androidx.compose.ui.platform.LocalContext.current
    var page by remember { mutableIntStateOf(0) }
    val slide = SLIDES[page]
    var entered by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { delay(100); entered = true }

    Box(Modifier.fillMaxSize().background(Void)) {
        // ambient nebula tinted to current slide
        val nebulaAlpha by animateFloatAsState(
            if (entered) 0.12f else 0f, tween(800), label = "neb",
        )
        Box(
            Modifier.size(500.dp).align(Alignment.TopEnd).offset(x = 120.dp, y = (-100).dp)
                .background(
                    Brush.radialGradient(listOf(slide.accent.copy(alpha = nebulaAlpha), Color.Transparent)),
                    CircleShape,
                ),
        )

        Column(
            Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding()
                .padding(horizontal = 28.dp).padding(top = 32.dp, bottom = 20.dp),
        ) {
            // skip button
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                Text(
                    "SKIP", color = TextDim, fontFamily = Display, fontSize = FS.s10,
                    fontWeight = FontWeight.SemiBold, letterSpacing = 2.sp,
                    modifier = Modifier.clip(RoundedCornerShape(8.dp))
                        .clickable { onComplete() }
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                )
            }

            Spacer(Modifier.weight(0.15f))

            // hero icon with animated ring
            AnimatedContent(
                page, label = "tourHero",
                transitionSpec = { fadeIn(tween(400)) togetherWith fadeOut(tween(200)) },
            ) { p ->
                val s = SLIDES[p]
                Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    TourHeroIcon(s.accent, s.icon)
                }
            }

            Spacer(Modifier.height(40.dp))

            // content
            AnimatedContent(
                page, label = "tourContent",
                transitionSpec = { fadeIn(tween(350, delayMillis = 80)) togetherWith fadeOut(tween(180)) },
            ) { p ->
                val s = SLIDES[p]
                Column {
                    Text(
                        s.overline, color = s.accent, fontFamily = Display, fontSize = FS.s10,
                        fontWeight = FontWeight.SemiBold, letterSpacing = 3.sp,
                    )
                    Spacer(Modifier.height(10.dp))
                    Text(
                        s.title, color = TextPrimary, fontFamily = Display,
                        fontSize = FS.s26, fontWeight = FontWeight.Bold, lineHeight = 32.sp,
                    )
                    Spacer(Modifier.height(14.dp))
                    Text(
                        s.body, color = TextMuted, fontFamily = Body,
                        fontSize = FS.s14, lineHeight = 22.sp,
                    )
                    Spacer(Modifier.height(22.dp))
                    s.features.forEach { feat ->
                        Row(
                            Modifier.padding(vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Box(
                                Modifier.size(6.dp).clip(CircleShape).background(s.accent),
                            )
                            Spacer(Modifier.width(12.dp))
                            Text(
                                feat, color = TextPrimary, fontFamily = Body,
                                fontSize = FS.s13, fontWeight = FontWeight.Medium,
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.weight(1f))

            // page indicators + next button
            Row(
                Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    SLIDES.indices.forEach { i ->
                        val w by animateFloatAsState(
                            if (i == page) 24f else 8f, tween(300), label = "dot$i",
                        )
                        val a by animateFloatAsState(
                            if (i == page) 1f else 0.3f, tween(300), label = "dotA$i",
                        )
                        Box(
                            Modifier.height(8.dp).width(w.dp).clip(CircleShape)
                                .background(slide.accent.copy(alpha = a)),
                        )
                    }
                }

                Spacer(Modifier.weight(1f))

                val isLast = page == SLIDES.lastIndex
                Box(
                    Modifier
                        .pressScale {
                            if (isLast) { com.ascend.lifeos.data.Haptics.epic(ftCtx); onComplete() }
                            else { com.ascend.lifeos.data.Haptics.tick(ftCtx); page++ }
                        }
                        .clip(RoundedCornerShape(16.dp))
                        .background(slide.accent)
                        .padding(horizontal = 28.dp, vertical = 14.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            if (isLast) "Let's go" else "Next",
                            color = Color(0xFF06110C), fontFamily = Body,
                            fontSize = FS.s14_5, fontWeight = FontWeight.ExtraBold,
                        )
                        if (!isLast) {
                            Spacer(Modifier.width(4.dp))
                            Icon(
                                Icons.Rounded.ChevronRight, "Next",
                                tint = Color(0xFF06110C), modifier = Modifier.size(18.dp),
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TourHeroIcon(accent: Color, icon: ImageVector) {
    val trans = rememberInfiniteTransition(label = "tourRing")
    val rot by trans.animateFloat(
        0f, 360f, infiniteRepeatable(tween(12000, easing = LinearEasing)), label = "rot",
    )
    val breath by trans.animateFloat(
        0.96f, 1.04f,
        infiniteRepeatable(
            tween(3000, easing = FastOutSlowInEasing),
            repeatMode = androidx.compose.animation.core.RepeatMode.Reverse,
        ),
        label = "breath",
    )

    Box(Modifier.size(160.dp).scale(breath), contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val r = size.minDimension / 2f - 8.dp.toPx()
            // dashed orbit ring
            val dashCount = 24
            val dashArc = 360f / dashCount * 0.6f
            val gapArc = 360f / dashCount * 0.4f
            for (i in 0 until dashCount) {
                val startAngle = rot + i * (dashArc + gapArc)
                drawArc(
                    color = accent.copy(alpha = 0.3f),
                    startAngle = startAngle, sweepAngle = dashArc,
                    useCenter = false,
                    style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round),
                    topLeft = Offset(center.x - r, center.y - r),
                    size = androidx.compose.ui.geometry.Size(r * 2, r * 2),
                )
            }
            // glow halo
            drawCircle(
                Brush.radialGradient(
                    listOf(accent.copy(alpha = 0.15f), Color.Transparent),
                    center = center, radius = r * 1.3f,
                ),
                radius = r * 1.3f,
            )
            // solid inner ring
            drawCircle(
                accent.copy(alpha = 0.12f), radius = r * 0.72f,
                style = Stroke(1.5.dp.toPx()),
            )
        }
        // center icon orb
        Box(
            Modifier.size(80.dp).clip(CircleShape)
                .background(Brush.radialGradient(listOf(accent.copy(alpha = 0.2f), accent.copy(alpha = 0.06f)))),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, null, tint = accent, modifier = Modifier.size(36.dp))
        }
    }
}
