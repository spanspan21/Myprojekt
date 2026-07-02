package com.ascend.lifeos.ui.screens

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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ascend.lifeos.data.ChessApi
import com.ascend.lifeos.data.Repo
import com.ascend.lifeos.ui.components.AscendCard
import com.ascend.lifeos.ui.components.AscendTextField
import com.ascend.lifeos.ui.components.LineChart
import com.ascend.lifeos.ui.components.ProgressBar
import com.ascend.lifeos.ui.components.SectionLabel
import com.ascend.lifeos.ui.theme.Accent
import com.ascend.lifeos.ui.theme.Amber
import com.ascend.lifeos.ui.theme.Bg
import com.ascend.lifeos.ui.theme.Line2
import com.ascend.lifeos.ui.theme.Red
import com.ascend.lifeos.ui.theme.Surface
import com.ascend.lifeos.ui.theme.SurfaceHi
import com.ascend.lifeos.ui.theme.TextDim
import com.ascend.lifeos.ui.theme.TextPrimary
import kotlinx.coroutines.launch

@Composable
fun ChessScreen() {
    val appData = Repo.data
    val ch = appData.profile.chess
    val linked = ch.account != null
    val scope = rememberCoroutineScope()

    var platform by rememberSaveable { mutableStateOf(ch.account?.platform ?: "chesscom") }
    var username by remember { mutableStateOf(ch.account?.username ?: "") }
    var status by remember { mutableStateOf("Rating, Puzzle & Bilanz automatisch übernehmen.") }

    // Auto-sync on open when linked.
    LaunchedEffect(Unit) {
        val acc = ch.account
        if (acc != null) {
            ChessApi.fetch(acc.platform, acc.username).onSuccess { Repo.applyChessSync(it, acc.platform, acc.username) }
        }
    }

    fun sync(u: String, plat: String) {
        if (u.isBlank()) { status = "Benutzername eingeben"; return }
        status = "Verknüpfe $u …"
        scope.launch {
            ChessApi.fetch(plat, u)
                .onSuccess { Repo.applyChessSync(it, plat, u); status = "✓ Verknüpft: ${if (plat == "lichess") "Lichess" else "Chess.com"} · $u" }
                .onFailure { e -> status = if (e is ChessApi.NotFound) "✗ Nutzer nicht gefunden." else "✗ Keine Verbindung. Internet prüfen." }
        }
    }

    val total = ch.games.w + ch.games.d + ch.games.l

    Column(
        Modifier.fillMaxWidth().statusBarsPadding().verticalScroll(rememberScrollState())
            .padding(horizontal = 17.dp).padding(top = 14.dp, bottom = 28.dp)
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
            Column(Modifier.weight(1f)) {
                Text("Schach", color = TextPrimary, fontSize = 26.sp, fontWeight = FontWeight.ExtraBold)
                Text("Rating, Partien & Performance", color = TextDim, fontSize = 12.sp)
            }
            Pill("Peak ${ch.peak}", Amber)
        }

        SectionLabel("Übersicht")
        AscendCard {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("${ch.rating}", color = TextPrimary, fontSize = 44.sp, fontWeight = FontWeight.ExtraBold, modifier = Modifier.weight(1f))
                if (!linked) {
                    StepBox("−") { Repo.chessRatingDelta(-1) }
                    Text("${ch.rating}", color = TextPrimary, fontSize = 15.sp, fontWeight = FontWeight.Bold, modifier = Modifier.width(52.dp), textAlign = TextAlign.Center)
                    StepBox("+") { Repo.chessRatingDelta(1) }
                }
            }
            Spacer(Modifier.height(15.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Rec("${ch.games.w}", "Siege", Accent, Modifier.weight(1f))
                Rec("${ch.games.d}", "Remis", Amber, Modifier.weight(1f))
                Rec("${ch.games.l}", "Niederl.", Red, Modifier.weight(1f))
                Rec(if (total > 0) "${ch.games.w * 100 / total}%" else "–", "Siegquote", TextPrimary, Modifier.weight(1f))
            }
            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Tile(ch.rapid?.rating, "Rapid", ch.rapid?.let { if (it.best > 0) "best ${it.best}" else if (it.games > 0) "${it.games} P." else "" } ?: "", Modifier.weight(1f))
                Tile(ch.blitz?.rating, "Blitz", ch.blitz?.let { if (it.best > 0) "best ${it.best}" else if (it.games > 0) "${it.games} P." else "" } ?: "", Modifier.weight(1f))
                Tile(ch.bullet?.rating, "Bullet", ch.bullet?.let { if (it.best > 0) "best ${it.best}" else if (it.games > 0) "${it.games} P." else "" } ?: "", Modifier.weight(1f))
                Tile(ch.puzzle, "Puzzle", "Taktik", Modifier.weight(1f))
            }
            if (!linked) {
                Spacer(Modifier.height(14.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    GameBtn("+ Sieg", Accent, Modifier.weight(1f)) { Repo.chessLogGame("w") }
                    GameBtn("+ Remis", Amber, Modifier.weight(1f)) { Repo.chessLogGame("d") }
                    GameBtn("+ Niederlage", Red, Modifier.weight(1f)) { Repo.chessLogGame("l") }
                }
                Spacer(Modifier.height(10.dp))
                Text("Rating einstellen, dann Ergebnis loggen — oder unten dein Konto verknüpfen.", color = TextDim, fontSize = 11.sp, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
            }
        }

        SectionLabel("Konto verknüpfen")
        AscendCard {
            Row(Modifier.clip(RoundedCornerShape(13.dp)).background(SurfaceHi).border(1.dp, Line2, RoundedCornerShape(13.dp)).padding(4.dp)) {
                Seg("♟ Chess.com", platform == "chesscom", Modifier.weight(1f)) { platform = "chesscom" }
                Seg("♞ Lichess", platform == "lichess", Modifier.weight(1f)) { platform = "lichess" }
            }
            Spacer(Modifier.height(11.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                AscendTextField(username, { username = it }, "Benutzername", Modifier.weight(1f), onDone = { sync(username, platform) })
                Spacer(Modifier.width(9.dp))
                AddBtn("Verknüpfen") { sync(username, platform) }
            }
            Spacer(Modifier.height(11.dp))
            Text(status, color = TextDim, fontSize = 11.sp, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
            if (linked) {
                Spacer(Modifier.height(12.dp))
                WideBtn("Jetzt aktualisieren") { ch.account?.let { sync(it.username, it.platform) } }
                Spacer(Modifier.height(8.dp))
                Text("Verknüpfung trennen", color = Red, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth().clickable { Repo.unlinkChess() }.padding(6.dp))
            }
        }

        SectionLabel("Rating-Verlauf", "$total Partien")
        AscendCard {
            val series = ch.history.map { it.rating }.ifEmpty { listOf(ch.rating) }
            LineChart(series)
            Spacer(Modifier.height(6.dp))
            Row(Modifier.fillMaxWidth()) {
                Text("min ${series.min()}", color = TextDim, fontSize = 11.sp)
                Spacer(Modifier.weight(1f))
                Text("max ${series.max()}", color = TextPrimary, fontSize = 11.sp)
            }
        }

        SectionLabel("Ziel")
        AscendCard {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Rating-Ziel", color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                    Text("${ch.rating} / ${ch.goal} · noch ${(ch.goal - ch.rating).coerceAtLeast(0)} Punkte", color = TextDim, fontSize = 11.sp)
                }
                StepBox("−") { Repo.chessGoalDelta(-50) }
                Text("${ch.goal}", color = TextPrimary, fontSize = 15.sp, fontWeight = FontWeight.Bold, modifier = Modifier.width(52.dp), textAlign = TextAlign.Center)
                StepBox("+") { Repo.chessGoalDelta(50) }
            }
            Spacer(Modifier.height(11.dp))
            ProgressBar((ch.rating / ch.goal.toFloat()).coerceIn(0f, 1f), Amber)
            if (!linked) {
                Spacer(Modifier.height(15.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("Puzzle-Rating", color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                        Text("Taktik-Training", color = TextDim, fontSize = 11.sp)
                    }
                    StepBox("−") { Repo.chessPuzzleDelta(-10) }
                    Text("${ch.puzzle}", color = TextPrimary, fontSize = 15.sp, fontWeight = FontWeight.Bold, modifier = Modifier.width(52.dp), textAlign = TextAlign.Center)
                    StepBox("+") { Repo.chessPuzzleDelta(10) }
                }
            }
        }
    }
}

@Composable
private fun Rec(value: String, label: String, color: Color, modifier: Modifier) {
    Column(modifier.clip(RoundedCornerShape(14.dp)).background(SurfaceHi).padding(vertical = 11.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, color = color, fontSize = 19.sp, fontWeight = FontWeight.ExtraBold)
        Text(label.uppercase(), color = TextDim, fontSize = 8.5.sp, letterSpacing = 0.5.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun Tile(rating: Int?, label: String, sub: String, modifier: Modifier) {
    Column(modifier.clip(RoundedCornerShape(13.dp)).background(SurfaceHi).padding(vertical = 11.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Text(if (rating != null && rating > 0) "$rating" else "–", color = TextPrimary, fontSize = 17.sp, fontWeight = FontWeight.ExtraBold)
        Text(label.uppercase(), color = TextDim, fontSize = 8.sp, letterSpacing = 0.5.sp, fontWeight = FontWeight.SemiBold)
        if (sub.isNotEmpty()) Text(sub, color = TextDim, fontSize = 8.5.sp)
    }
}

@Composable
private fun GameBtn(text: String, color: Color, modifier: Modifier, onClick: () -> Unit) {
    Box(modifier.clip(RoundedCornerShape(13.dp)).background(SurfaceHi).border(1.dp, Line2, RoundedCornerShape(13.dp)).clickable { onClick() }.padding(vertical = 13.dp), contentAlignment = Alignment.Center) {
        Text(text, color = color, fontSize = 13.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun Seg(text: String, on: Boolean, modifier: Modifier, onClick: () -> Unit) {
    Box(modifier.clip(RoundedCornerShape(10.dp)).background(if (on) Surface else Color.Transparent).clickable { onClick() }.padding(vertical = 11.dp), contentAlignment = Alignment.Center) {
        Text(text, color = if (on) TextPrimary else TextDim, fontSize = 14.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun StepBox(label: String, onClick: () -> Unit) {
    Box(Modifier.size(38.dp).clip(RoundedCornerShape(11.dp)).background(SurfaceHi).border(1.dp, Line2, RoundedCornerShape(11.dp)).clickable { onClick() }, contentAlignment = Alignment.Center) {
        Text(label, color = TextPrimary, fontSize = 19.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun AddBtn(text: String, onClick: () -> Unit) {
    Box(Modifier.clip(RoundedCornerShape(13.dp)).background(Accent).clickable { onClick() }.padding(horizontal = 16.dp, vertical = 12.dp), contentAlignment = Alignment.Center) {
        Text(text, color = Bg, fontSize = 14.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun WideBtn(text: String, onClick: () -> Unit) {
    Box(Modifier.fillMaxWidth().clip(RoundedCornerShape(13.dp)).background(SurfaceHi).border(1.dp, Line2, RoundedCornerShape(13.dp)).clickable { onClick() }.padding(vertical = 13.dp), contentAlignment = Alignment.Center) {
        Text(text, color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun Pill(text: String, color: Color) {
    Box(Modifier.clip(RoundedCornerShape(11.dp)).background(color.copy(alpha = 0.09f)).border(1.dp, color.copy(alpha = 0.22f), RoundedCornerShape(11.dp)).padding(horizontal = 12.dp, vertical = 6.dp)) {
        Text(text, color = color, fontSize = 11.5.sp, fontWeight = FontWeight.Bold)
    }
}
