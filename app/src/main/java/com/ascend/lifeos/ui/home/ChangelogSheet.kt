package com.ascend.lifeos.ui.home

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.clickable
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ascend.lifeos.ui.theme.*

/**
 * "System Updates" — shown once after each new build lands on the device.
 * Keyed on PackageInfo.lastUpdateTime, remembered in the settings prefs.
 * Keep the list short: only what the user can actually see or toggle.
 */
object Changelog {
    // newest first — edit this list per release
    val ENTRIES = listOf(
        "Crash black box — if JARVIS ever crashes, Settings → Diagnostics holds the report, ready to share",
        "Settings hub — every module, notification and experiment is a toggle now",
        "Protocols — WHEN→THEN directives on Home (game day, exams, low recovery)",
        "School OS — grades in points, homework with Untis deadlines, vocab decks",
        "Mind · Finance · Goals — journal & breathing, pocket-money ledger, quarter goals",
        "Year heatmap, correlation explorer and your training Wrapped",
        "Season phases for hockey — plan volume adapts to off/pre/in-season",
        "Form check videos + experimental camera rep counter",
        "Optional spoken briefing (off by default — Settings → Jarvis)",
    )

    private fun stamp(ctx: Context): Long =
        runCatching { ctx.packageManager.getPackageInfo(ctx.packageName, 0).lastUpdateTime }.getOrDefault(0L)

    fun shouldShow(ctx: Context): Boolean {
        val s = stamp(ctx)
        return s > 0 && ctx.getSharedPreferences("settings", Context.MODE_PRIVATE)
            .getLong("changelog_seen", 0L) != s
    }

    fun markSeen(ctx: Context) {
        ctx.getSharedPreferences("settings", Context.MODE_PRIVATE)
            .edit().putLong("changelog_seen", stamp(ctx)).apply()
    }
}

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun ChangelogSheet(onDismiss: () -> Unit) {
    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = Color(0xFF0B0D10), dragHandle = null) {
        Column(
            Modifier.fillMaxWidth().padding(22.dp).navigationBarsPadding()
                .verticalScroll(rememberScrollState()),
        ) {
            Text(
                "SYSTEM UPDATES", color = Mod.Home, fontFamily = Display,
                fontSize = 10.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 3.sp,
            )
            Spacer(Modifier.height(4.dp))
            Text("New in this build", color = TextPrimary, fontFamily = Display, fontSize = 21.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(14.dp))
            Changelog.ENTRIES.forEach {
                Row(Modifier.padding(vertical = 5.dp), verticalAlignment = Alignment.Top) {
                    Box(Modifier.padding(top = 6.dp).size(5.dp).clip(CircleShape).background(Mod.Home))
                    Spacer(Modifier.width(10.dp))
                    Text(it, color = TextMuted, fontSize = 13.sp, fontFamily = Body, lineHeight = 18.sp)
                }
            }
            Spacer(Modifier.height(16.dp))
            Box(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(13.dp)).background(Mod.Home)
                    .clickable(onClick = onDismiss).padding(vertical = 12.dp),
                contentAlignment = Alignment.Center,
            ) { Text("Understood", color = Void, fontSize = 13.5.sp, fontFamily = Body, fontWeight = FontWeight.ExtraBold) }
            Spacer(Modifier.height(14.dp))
        }
    }
}
