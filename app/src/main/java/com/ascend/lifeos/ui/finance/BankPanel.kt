package com.ascend.lifeos.ui.finance

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import com.ascend.lifeos.ui.motion.pressScale
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccountBalance
import androidx.compose.material.icons.rounded.Sync
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ascend.lifeos.data.finance.BankAspsp
import com.ascend.lifeos.data.finance.BankLink
import com.ascend.lifeos.data.finance.FinanceStore
import com.ascend.lifeos.ui.kit.Panel
import com.ascend.lifeos.ui.theme.Body
import com.ascend.lifeos.ui.theme.Crit
import com.ascend.lifeos.ui.theme.TextDim
import com.ascend.lifeos.ui.theme.TextMuted
import com.ascend.lifeos.ui.theme.TextPrimary
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter

// ─── Bank-Verknüpfung (Enable Banking, read-only) ────────────────────────────
// Der Panel-Block in FinanceHome: unverbunden ein Einladungs-Panel, verbunden
// die Konten mit letztem Sync, Freigabe-Ablauf und Sync/Trennen. Die SCA läuft
// im Browser der Bank — zurück geht es über die GitHub-Pages-Callback-Seite.

@Composable
internal fun BankPanel() {
    val ctx = LocalContext.current
    val rev = BankLink.rev // subscribe
    val busy = BankLink.busy
    val statusLine = BankLink.status
    val linked = remember(rev) { BankLink.linked(ctx) }
    var showPicker by remember { mutableStateOf(false) }

    Panel {
        Column(Modifier.fillMaxWidth().padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.AccountBalance, null, tint = FinAccent, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(8.dp))
                Text(
                    if (linked) BankLink.bankName(ctx) ?: "Bank" else "Bank verbinden",
                    color = TextPrimary, fontSize = com.ascend.lifeos.ui.theme.FS.s14, fontFamily = Body, fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f),
                )
                if (linked) {
                    Box(
                        Modifier.clip(CircleShape)
                            .background(FinAccent.copy(alpha = if (busy) 0.06f else 0.14f))
                            .clickable(enabled = !busy) { BankLink.requestSync(ctx) }
                            .padding(horizontal = 12.dp, vertical = 7.dp),
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Rounded.Sync, "Sync bank", tint = FinAccent, modifier = Modifier.size(13.dp))
                            Spacer(Modifier.width(5.dp))
                            Text(
                                if (busy) "läuft…" else "Sync",
                                color = FinAccent, fontSize = com.ascend.lifeos.ui.theme.FS.s11_5, fontFamily = Body, fontWeight = FontWeight.Bold,
                            )
                        }
                    }
                }
            }

            if (!linked) {
                Spacer(Modifier.height(8.dp))
                Text(
                    "Umsätze automatisch importieren — nur Lesezugriff, Freigabe per TAN bei deiner Bank. Der AboRadar erkennt deine Abos dann von selbst.",
                    color = TextMuted, fontSize = com.ascend.lifeos.ui.theme.FS.s12, fontFamily = Body, lineHeight = 17.sp,
                )
                Spacer(Modifier.height(12.dp))
                ActionButton(if (busy) "Verbinde …" else "Bank auswählen", enabled = !busy) { showPicker = true }
            } else {
                val accounts = remember(rev) { BankLink.linkedAccounts(ctx) }
                val finance = remember(rev, FinanceStore.rev) { FinanceStore.accounts(ctx).associateBy { it.id } }
                Spacer(Modifier.height(10.dp))
                accounts.forEach { acc ->
                    Row(Modifier.padding(vertical = 3.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(acc.label, color = TextPrimary, fontSize = com.ascend.lifeos.ui.theme.FS.s12_5, fontFamily = Body, fontWeight = FontWeight.SemiBold, maxLines = 1)
                            if (acc.iban.length > 4) {
                                Text("···${acc.iban.takeLast(4)}", color = TextDim, fontSize = com.ascend.lifeos.ui.theme.FS.s10, fontFamily = Body)
                            }
                        }
                        Text(
                            euros(finance[acc.financeAccountId]?.balanceCents ?: 0L),
                            color = TextPrimary, fontSize = com.ascend.lifeos.ui.theme.FS.s13, fontFamily = Body, fontWeight = FontWeight.Bold,
                        )
                    }
                }
                Spacer(Modifier.height(8.dp))
                val vu = BankLink.validUntil(ctx)
                val last = BankLink.lastSyncTs(ctx)
                val lastLabel = when {
                    last == 0L -> "noch nie"
                    System.currentTimeMillis() - last < 90_000 -> "gerade eben"
                    System.currentTimeMillis() - last < 3_600_000 -> "vor ${(System.currentTimeMillis() - last) / 60_000} min"
                    else -> "vor ${(System.currentTimeMillis() - last) / 3_600_000} h"
                }
                Text(
                    "Sync: $lastLabel" + (vu?.let { " · Freigabe bis ${it.format(DateTimeFormatter.ofPattern("dd.MM.yyyy"))}" } ?: ""),
                    color = TextDim, fontSize = com.ascend.lifeos.ui.theme.FS.s10_5, fontFamily = Body,
                )
                // Freigabe läuft ab → rechtzeitig neu verbinden können
                if (vu != null && !vu.isAfter(LocalDate.now().plusDays(7))) {
                    Spacer(Modifier.height(8.dp))
                    ActionButton("Freigabe erneuern", enabled = !busy) { showPicker = true }
                }
                Spacer(Modifier.height(6.dp))
                Text(
                    "Trennen",
                    color = TextDim, fontSize = com.ascend.lifeos.ui.theme.FS.s11, fontFamily = Body, fontWeight = FontWeight.Bold,
                    modifier = Modifier.clickable(enabled = !busy) { BankLink.unlink(ctx) }.padding(vertical = 4.dp),
                )
            }

            statusLine?.let {
                Spacer(Modifier.height(6.dp))
                Text(
                    it,
                    color = if (it.startsWith("Fehler") || it.contains("abgelaufen")) Crit else FinAccent,
                    fontSize = com.ascend.lifeos.ui.theme.FS.s11, fontFamily = Body, fontWeight = FontWeight.SemiBold, lineHeight = 15.sp,
                )
            }
        }
    }

    if (showPicker) BankPickerSheet(onDismiss = { showPicker = false })
}

/** Bankauswahl: lädt die deutschen Institute live, filtert lokal. */
@Composable
private fun BankPickerSheet(onDismiss: () -> Unit) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    var query by remember { mutableStateOf("") }
    var banks by remember { mutableStateOf<List<BankAspsp>?>(null) }
    var error by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        runCatching { BankLink.banks(ctx) }
            .onSuccess { banks = it }
            .onFailure { error = it.message?.take(160) ?: "Bankliste nicht ladbar" }
    }

    SheetShell("Bank auswählen", onDismiss) {
        SearchField(query, { query = it }, "Bank suchen — z. B. Sparkasse, DKB, N26")
        Spacer(Modifier.height(12.dp))
        when {
            error != null -> Text("Fehler: $error", color = Crit, fontSize = com.ascend.lifeos.ui.theme.FS.s12_5, fontFamily = Body, lineHeight = 17.sp)
            banks == null -> Text("Lade Institute …", color = TextMuted, fontSize = com.ascend.lifeos.ui.theme.FS.s12_5, fontFamily = Body)
            else -> {
                val hits = remember(query, banks) {
                    val q = query.trim().lowercase()
                    val all = banks.orEmpty()
                    if (q.isEmpty()) all else all.filter { it.name.lowercase().contains(q) }
                }
                if (hits.isEmpty()) {
                    com.ascend.lifeos.ui.kit.EmptyState(androidx.compose.material.icons.Icons.Rounded.AccountBalance, "Keine Treffer", "Anders schreiben?", com.ascend.lifeos.ui.theme.Mod.Finance)
                } else {
                    LazyColumn(Modifier.fillMaxWidth().heightIn(max = 420.dp)) {
                        items(hits, key = { it.name }) { bank ->
                            Row(
                                Modifier.animateItem().fillMaxWidth().clip(RoundedCornerShape(11.dp))
                                    .pressScale {
                                        scope.launch {
                                            runCatching { BankLink.startAuth(ctx, bank) }
                                                .onSuccess { url ->
                                                    onDismiss()
                                                    runCatching {
                                                        ctx.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                                                    }
                                                }
                                                .onFailure { error = it.message?.take(160) }
                                        }
                                    }
                                    .padding(horizontal = 6.dp, vertical = 11.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Icon(Icons.Rounded.AccountBalance, bank.name, tint = TextDim, modifier = Modifier.size(15.dp))
                                Spacer(Modifier.width(10.dp))
                                Text(bank.name, color = TextPrimary, fontSize = com.ascend.lifeos.ui.theme.FS.s13_5, fontFamily = Body, fontWeight = FontWeight.SemiBold, maxLines = 1)
                            }
                        }
                    }
                }
                Spacer(Modifier.height(8.dp))
                Text(
                    "Du wirst zu deiner Bank weitergeleitet und gibst dort per TAN frei. JARVIS sieht deine Zugangsdaten nie — nur die Umsätze (Lesezugriff, bis zu 90 Tage gültig).",
                    color = TextDim, fontSize = com.ascend.lifeos.ui.theme.FS.s10_5, fontFamily = Body, lineHeight = 15.sp,
                )
            }
        }
    }
}
