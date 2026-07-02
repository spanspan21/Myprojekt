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
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ascend.lifeos.data.CoachEngine
import com.ascend.lifeos.data.Notifier
import com.ascend.lifeos.data.Repo
import com.ascend.lifeos.ui.components.AscendCard
import com.ascend.lifeos.ui.components.AscendTextField
import com.ascend.lifeos.ui.components.SectionLabel
import com.ascend.lifeos.ui.components.Stepper
import com.ascend.lifeos.ui.components.Toggle
import com.ascend.lifeos.ui.theme.ACCENT_PRESETS
import com.ascend.lifeos.ui.theme.Accent
import com.ascend.lifeos.ui.theme.Amber
import com.ascend.lifeos.ui.theme.applyAccent
import com.ascend.lifeos.ui.theme.Bg
import com.ascend.lifeos.ui.theme.Line
import com.ascend.lifeos.ui.theme.Line2
import com.ascend.lifeos.ui.theme.Red
import com.ascend.lifeos.ui.theme.SurfaceHi
import com.ascend.lifeos.ui.theme.TextDim
import com.ascend.lifeos.ui.theme.TextPrimary

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CoachScreen(onOpenSubs: () -> Unit = {}) {
    val appData = Repo.data
    val day = Repo.today()
    val p = appData.profile
    val clip = LocalClipboardManager.current
    val ctx = LocalContext.current

    val permLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) { Repo.setReminders(true); Notifier.schedule(ctx) }
    }
    fun toggleReminders(on: Boolean) {
        if (on) {
            if (Notifier.hasPermission(ctx)) { Repo.setReminders(true); Notifier.schedule(ctx) }
            else permLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
        } else { Repo.setReminders(false); Notifier.cancel(ctx) }
    }

    var input by remember { mutableStateOf("") }
    var name by remember { mutableStateOf(p.name) }
    var refl by remember { mutableStateOf(day.reflection) }
    var backupStatus by remember { mutableStateOf("") }

    Column(
        Modifier.fillMaxWidth().statusBarsPadding().verticalScroll(rememberScrollState())
            .padding(horizontal = 17.dp).padding(top = 14.dp, bottom = 28.dp)
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
            Column(Modifier.weight(1f)) {
                Text("Coach", color = TextPrimary, fontSize = 26.sp, fontWeight = FontWeight.ExtraBold)
                Text("Lokal, ehrlich, kostenlos — kein Abo.", color = TextDim, fontSize = 12.sp)
            }
            Pill("⚡ ${p.streak}")
        }

        SectionLabel("Chat")
        AscendCard {
            Box(Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(SurfaceHi).border(1.dp, Line, RoundedCornerShape(12.dp)).padding(14.dp)) {
                Text(remember(day, p) { CoachEngine.proactive() }, color = TextPrimary, fontSize = 14.sp, lineHeight = 21.sp)
            }
            Spacer(Modifier.height(12.dp))
            day.coachLog.forEach { m ->
                Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalArrangement = if (m.who == "me") Arrangement.End else Arrangement.Start) {
                    Box(
                        Modifier.fillMaxWidth(0.86f).clip(RoundedCornerShape(15.dp))
                            .background(if (m.who == "me") Color(0xFFEEF1F6) else SurfaceHi)
                            .border(if (m.who == "me") 0.dp else 1.dp, Line, RoundedCornerShape(15.dp))
                            .padding(horizontal = 13.dp, vertical = 10.dp),
                    ) {
                        Text(m.text, color = if (m.who == "me") Color(0xFF0A0A0A) else TextPrimary, fontSize = 14.sp, lineHeight = 20.sp)
                    }
                }
            }
            Spacer(Modifier.height(12.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                AscendTextField(input, { input = it }, "Schreib dem Coach…", Modifier.weight(1f), onDone = { Repo.coachSend(input); input = "" })
                Spacer(Modifier.width(9.dp))
                Box(
                    Modifier.size(44.dp).clip(RoundedCornerShape(50)).background(Accent).clickable { Repo.coachSend(input); input = "" },
                    contentAlignment = Alignment.Center,
                ) { Text("↑", color = Bg, fontSize = 20.sp, fontWeight = FontWeight.Bold) }
            }
        }

        SectionLabel("Reflexion heute")
        AscendCard {
            Box(Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(SurfaceHi).border(1.dp, Line2, RoundedCornerShape(12.dp)).padding(13.dp)) {
                if (refl.isEmpty()) Text("Was lief gut? Was war schwer? Was machst du morgen besser?", color = TextDim, fontSize = 14.sp)
                BasicTextField(
                    value = refl,
                    onValueChange = { refl = it; Repo.setReflection(it) },
                    textStyle = TextStyle(color = TextPrimary, fontSize = 14.sp, lineHeight = 20.sp),
                    cursorBrush = SolidColor(Accent),
                    modifier = Modifier.fillMaxWidth().height(80.dp),
                )
            }
        }

        SectionLabel("Einstellungen")
        AscendCard {
            SetRow("Dein Name", "Header & Coach") {
                AscendTextField(name, { name = it; Repo.setName(it) }, "Name", Modifier.width(150.dp))
            }
            Divider()
            SetRow("Wasser-Ziel", "Gläser pro Tag") {
                Stepper("${p.waterGoal}", { Repo.setWaterGoal(p.waterGoal - 1) }, { Repo.setWaterGoal(p.waterGoal + 1) })
            }
            Divider()
            Column(Modifier.fillMaxWidth().padding(vertical = 13.dp)) {
                Text("Akzentfarbe", color = TextPrimary, fontSize = 14.sp)
                Text("Färbt die ganze App", color = TextDim, fontSize = 11.sp)
                Spacer(Modifier.height(12.dp))
                FlowRow(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    ACCENT_PRESETS.forEach { col ->
                        val selected = p.accent == col
                        Box(
                            Modifier.size(34.dp).clip(CircleShape).background(Color(col))
                                .border(if (selected) 2.5.dp else 0.dp, TextPrimary, CircleShape)
                                .clickable { Repo.setAccent(col); applyAccent(col) },
                            contentAlignment = Alignment.Center,
                        ) { if (selected) Text("✓", color = Bg, fontSize = 15.sp, fontWeight = FontWeight.ExtraBold) }
                    }
                }
            }
            Divider()
            SetRow("Erinnerungen", if (p.reminders) "Täglich ${Notifier.MORNING_HOUR}:00 & ${Notifier.EVENING_HOUR}:00 Uhr" else "Push-Nudges morgens & abends") {
                Toggle(p.reminders) { on -> toggleReminders(on) }
            }
            Divider()
            SetRow("Streak-Freeze", if (p.freezeAvail > 0) "1 pro Woche · verfügbar" else "diese Woche genutzt") {
                Pill("🧊 ${p.freezeAvail}")
            }
            Divider()
            SetRow("Abos & Kosten", "Fixkosten tracken") {
                Text("Öffnen  →", color = Accent, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.clickable { onOpenSubs() }.padding(4.dp))
            }
            Divider()
            SetRow("Backup", if (backupStatus.isEmpty()) "In Zwischenablage sichern / laden" else backupStatus) {
                Column(horizontalAlignment = Alignment.End) {
                    SmallAction("Kopieren") { clip.setText(AnnotatedString(Repo.exportJson())); backupStatus = "✓ Kopiert" }
                    Spacer(Modifier.height(6.dp))
                    SmallAction("Einfügen") {
                        val txt = clip.getText()?.text
                        backupStatus = if (txt != null && Repo.importJson(txt)) "✓ Importiert" else "✗ Ungültig"
                    }
                }
            }
            Divider()
            SetRow("Daten zurücksetzen", "Löscht alles auf dem Gerät") {
                Text("Zurücksetzen", color = Red, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.clickable {
                    Repo.resetAll(); name = ""; refl = ""; backupStatus = ""
                }.padding(4.dp))
            }
        }

        Text("ASCEND · Life OS — alles lokal auf deinem Gerät.", color = TextDim, fontSize = 11.sp, modifier = Modifier.fillMaxWidth().padding(top = 26.dp), textAlign = androidx.compose.ui.text.style.TextAlign.Center)
    }
}

@Composable
private fun SetRow(title: String, sub: String, trailing: @Composable () -> Unit) {
    Row(Modifier.fillMaxWidth().padding(vertical = 13.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(title, color = TextPrimary, fontSize = 14.sp)
            Text(sub, color = TextDim, fontSize = 11.sp)
        }
        Spacer(Modifier.width(10.dp))
        trailing()
    }
}

@Composable
private fun Divider() {
    Box(Modifier.fillMaxWidth().height(1.dp).background(Line))
}

@Composable
private fun SmallAction(text: String, onClick: () -> Unit) {
    Box(Modifier.clip(RoundedCornerShape(9.dp)).background(SurfaceHi).border(1.dp, Line2, RoundedCornerShape(9.dp)).clickable { onClick() }.padding(horizontal = 13.dp, vertical = 7.dp)) {
        Text(text, color = TextPrimary, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun Pill(text: String) {
    Box(Modifier.clip(RoundedCornerShape(11.dp)).background(Amber.copy(alpha = 0.09f)).border(1.dp, Amber.copy(alpha = 0.22f), RoundedCornerShape(11.dp)).padding(horizontal = 12.dp, vertical = 6.dp)) {
        Text(text, color = Amber, fontSize = 11.5.sp, fontWeight = FontWeight.Bold)
    }
}
