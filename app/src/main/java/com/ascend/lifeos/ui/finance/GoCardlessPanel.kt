package com.ascend.lifeos.ui.finance

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccountBalance
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.ascend.lifeos.data.finance.GcBank
import com.ascend.lifeos.ui.motion.pressScale
import com.ascend.lifeos.data.finance.GoCardlessLink
import com.ascend.lifeos.ui.kit.JarvisSheet
import com.ascend.lifeos.ui.kit.Panel
import com.ascend.lifeos.ui.kit.SectionLabel
import com.ascend.lifeos.ui.theme.*
import kotlinx.coroutines.launch

/**
 * Open Banking panel (GoCardless). Paste your two API keys once, connect your bank
 * in the browser, then transactions sync read-only. No rebuild, no key files.
 */
@Composable
fun GoCardlessPanel() {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    val uriHandler = LocalUriHandler.current
    val rev = GoCardlessLink.rev
    val status = GoCardlessLink.status
    val busy = GoCardlessLink.busy

    var pickerOpen by remember { mutableStateOf(false) }
    var banks by remember { mutableStateOf<List<GcBank>>(emptyList()) }
    var loadingBanks by remember { mutableStateOf(false) }

    val configured = remember(rev) { GoCardlessLink.configured(ctx) }
    val linked = remember(rev) { GoCardlessLink.linked(ctx) }
    val bankName = remember(rev) { GoCardlessLink.bankName(ctx) }
    val accounts = remember(rev) { GoCardlessLink.linkedAccountLabels(ctx) }
    val lastSync = remember(rev) { GoCardlessLink.lastSyncTs(ctx) }

    Panel(Modifier.fillMaxWidth(), corner = 20.dp) {
        Column(Modifier.padding(16.dp)) {
            SectionLabel("Bank sync · Open Banking", accent = Mod.Finance)
            Spacer(Modifier.height(10.dp))

            when {
                !configured -> CredentialSetup(onSaved = { id, key -> GoCardlessLink.setCredentials(ctx, id, key) })

                !linked -> {
                    Text(
                        "Keys saved. Connect your bank — you'll confirm in the browser with your online-banking login.",
                        color = TextDim, fontFamily = Body, fontSize = FS.s12, lineHeight = FS.s16.let { it },
                    )
                    Spacer(Modifier.height(12.dp))
                    Pill(if (loadingBanks) "Loading banks…" else "Connect bank", filled = true) {
                        if (loadingBanks) return@Pill
                        loadingBanks = true
                        scope.launch {
                            banks = runCatching { GoCardlessLink.banks(ctx) }.getOrDefault(emptyList())
                            loadingBanks = false
                            if (banks.isNotEmpty()) pickerOpen = true
                        }
                    }
                }

                else -> {
                    Text(bankName ?: "Bank linked", color = TextPrimary, fontFamily = Body, fontSize = FS.s14, fontWeight = FontWeight.Bold)
                    accounts.forEach { Text("· $it", color = TextDim, fontFamily = Body, fontSize = FS.s11_5) }
                    Text(
                        if (lastSync > 0) "read-only · re-consent every 90 days" else "not synced yet",
                        color = TextDim, fontFamily = Body, fontSize = FS.s11,
                    )
                    Spacer(Modifier.height(12.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Pill(if (busy) "Syncing…" else "Sync now", filled = true, modifier = Modifier.weight(1f)) {
                            if (!busy) GoCardlessLink.requestSync(ctx)
                        }
                        Pill("Unlink", modifier = Modifier.weight(1f)) { GoCardlessLink.unlink(ctx) }
                    }
                }
            }

            status?.let {
                Spacer(Modifier.height(8.dp))
                Text(it, color = TextMuted, fontFamily = Body, fontSize = FS.s11)
            }
        }
    }

    if (pickerOpen) {
        BankPickerSheet(
            banks = banks,
            onDismiss = { pickerOpen = false },
            onPick = { bank ->
                pickerOpen = false
                scope.launch {
                    val link = runCatching { GoCardlessLink.startAuth(ctx, bank) }.getOrNull()
                    if (link != null) runCatching { uriHandler.openUri(link) }
                }
            },
        )
    }
}

@Composable
private fun CredentialSetup(onSaved: (String, String) -> Unit) {
    var id by remember { mutableStateOf("") }
    var key by remember { mutableStateOf("") }
    Text(
        "Sign up free at gocardless.com/bank-account-data, then paste your Secret ID and Secret Key:",
        color = TextDim, fontFamily = Body, fontSize = FS.s12,
    )
    Spacer(Modifier.height(10.dp))
    Field("Secret ID", id) { id = it }
    Spacer(Modifier.height(8.dp))
    Field("Secret Key", key) { key = it }
    Spacer(Modifier.height(12.dp))
    Pill("Save keys", filled = true) { if (id.isNotBlank() && key.isNotBlank()) onSaved(id, key) }
}

@Composable
private fun Field(hint: String, value: String, onChange: (String) -> Unit) {
    Box(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(11.dp))
            .background(Ivory.copy(alpha = 0.05f)).border(0.5.dp, Line, RoundedCornerShape(11.dp))
            .padding(horizontal = 12.dp, vertical = 12.dp),
    ) {
        if (value.isEmpty()) Text(hint, color = TextMuted, fontFamily = Body, fontSize = FS.s13)
        BasicTextField(
            value = value, onValueChange = onChange, singleLine = true,
            textStyle = TextStyle(color = TextPrimary, fontFamily = Body, fontSize = FS.s13),
            cursorBrush = SolidColor(Mod.Finance), modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun BankPickerSheet(banks: List<GcBank>, onDismiss: () -> Unit, onPick: (GcBank) -> Unit) {
    var query by remember { mutableStateOf("") }
    val filtered = remember(query, banks) {
        if (query.isBlank()) banks else banks.filter { it.name.contains(query, ignoreCase = true) }
    }
    JarvisSheet(onDismiss = onDismiss) {
        Column(Modifier.fillMaxWidth().padding(20.dp)) {
            Text("CHOOSE YOUR BANK", color = Mod.Finance, fontFamily = Body, fontSize = FS.s10, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(12.dp))
            Field("Search…", query) { query = it }
            Spacer(Modifier.height(10.dp))
            if (filtered.isEmpty()) {
                com.ascend.lifeos.ui.kit.EmptyState(
                    Icons.Rounded.AccountBalance,
                    "No banks found", "Try a different search term", Mod.Finance,
                )
            } else {
                LazyColumn(Modifier.heightIn(max = 360.dp)) {
                    items(filtered, key = { it.id }) { b ->
                        Text(
                            b.name, color = TextPrimary, fontFamily = Body, fontSize = FS.s13_5,
                            modifier = Modifier.animateItem().fillMaxWidth().pressScale { onPick(b) }.padding(vertical = 12.dp),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun Pill(label: String, modifier: Modifier = Modifier, filled: Boolean = false, onClick: () -> Unit) {
    Box(
        modifier.clip(RoundedCornerShape(11.dp))
            .background(if (filled) Mod.Finance else Ivory.copy(alpha = 0.06f))
            .pressScale(onClick = onClick).padding(horizontal = 14.dp, vertical = 11.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(label, color = if (filled) Void else TextMuted, fontFamily = Body, fontSize = FS.s12_5, fontWeight = FontWeight.Bold)
    }
}
