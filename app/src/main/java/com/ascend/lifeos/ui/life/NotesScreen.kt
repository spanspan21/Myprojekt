package com.ascend.lifeos.ui.life

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.EditNote
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ascend.lifeos.data.life.LifeStores
import com.ascend.lifeos.ui.motion.pressScale
import com.ascend.lifeos.ui.kit.EmptyState
import com.ascend.lifeos.ui.kit.JarvisHeader
import com.ascend.lifeos.ui.kit.ModuleBackground
import com.ascend.lifeos.ui.kit.Panel
import com.ascend.lifeos.ui.theme.*
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun NotesScreen(onClose: () -> Unit) {
    val ctx = LocalContext.current
    @Suppress("UNUSED_EXPRESSION") LifeStores.rev
    val notes = LifeStores.notes(ctx)
    var newText by remember { mutableStateOf("") }
    var searchQuery by remember { mutableStateOf("") }
    var editingId by remember { mutableStateOf<String?>(null) }
    var editText by remember { mutableStateOf("") }
    val fmt = remember { DateTimeFormatter.ofPattern("dd.MM · HH:mm", Locale.getDefault()) }

    var armedNote by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(armedNote) { if (armedNote != null) { kotlinx.coroutines.delay(2500); armedNote = null } }

    val filtered = if (searchQuery.isBlank()) notes else {
        val q = searchQuery.lowercase()
        notes.filter { it.second.lowercase().contains(q) }
    }

    Box(Modifier.fillMaxSize()) {
        ModuleBackground(Mod.Skills)
        Column(
            Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp).padding(top = 8.dp, bottom = 100.dp),
        ) {
            JarvisHeader("Notes", "${notes.size} captured", Mod.Skills) {
                Icon(Icons.Rounded.Close, "Close", tint = TextDim, modifier = Modifier.size(20.dp).clip(CircleShape).clickable(onClick = onClose))
            }
            Spacer(Modifier.height(12.dp))

            // Search bar
            if (notes.size > 3) {
                Panel(Modifier.fillMaxWidth(), corner = 14.dp) {
                    Row(
                        Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(Icons.Rounded.Search, null, tint = TextDim, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        OutlinedTextField(
                            value = searchQuery, onValueChange = { searchQuery = it },
                            placeholder = { Text("Search notes…", color = TextDim, fontFamily = Body, fontSize = FS.s12) },
                            singleLine = true,
                            modifier = Modifier.weight(1f).height(44.dp),
                            textStyle = TextStyle(color = TextPrimary, fontFamily = Body, fontSize = FS.s12),
                        )
                        if (searchQuery.isNotBlank()) {
                            Spacer(Modifier.width(6.dp))
                            Icon(
                                Icons.Rounded.Close, "Clear search", tint = TextDim,
                                modifier = Modifier.size(16.dp).clip(CircleShape).clickable { searchQuery = "" },
                            )
                        }
                    }
                }
                Spacer(Modifier.height(12.dp))
            }

            // New note input (multi-line)
            Panel(Modifier.fillMaxWidth(), corner = 16.dp) {
                Column(Modifier.padding(12.dp)) {
                    OutlinedTextField(
                        value = newText, onValueChange = { newText = it.take(com.ascend.lifeos.data.Prefs.int(ctx, com.ascend.lifeos.data.Prefs.NOTE_CHAR_LIMIT, 1000)) },
                        placeholder = { Text("Capture a thought…", color = TextDim, fontFamily = Body, fontSize = FS.s13) },
                        singleLine = false, maxLines = 5, minLines = 2,
                        modifier = Modifier.fillMaxWidth(),
                        textStyle = TextStyle(color = TextPrimary, fontFamily = Body, fontSize = FS.s13),
                    )
                    Spacer(Modifier.height(8.dp))
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        val noteLimit = com.ascend.lifeos.data.Prefs.int(ctx, com.ascend.lifeos.data.Prefs.NOTE_CHAR_LIMIT, 1000)
                        Text("${newText.length}/$noteLimit", color = TextDim, fontFamily = Body, fontSize = FS.s10)
                        Box(
                            Modifier.clip(RoundedCornerShape(12.dp))
                                .background(if (newText.isNotBlank()) Mod.Skills else Mod.Skills.copy(alpha = 0.25f))
                                .then(if (newText.isNotBlank()) Modifier.pressScale {
                                    LifeStores.addNote(ctx, newText)
                                    newText = ""
                                    com.ascend.lifeos.data.Haptics.confirm(ctx)
                                    com.ascend.lifeos.ui.kit.AppFeedback.show("Note saved")
                                } else Modifier)
                                .padding(horizontal = 16.dp, vertical = 10.dp),
                        ) { Text("Save", color = Void, fontFamily = Body, fontSize = FS.s13, fontWeight = FontWeight.ExtraBold) }
                    }
                }
            }
            Spacer(Modifier.height(16.dp))

            if (filtered.isEmpty()) {
                EmptyState(
                    icon = Icons.Rounded.EditNote,
                    title = if (searchQuery.isNotBlank()) "No matches" else "No notes yet",
                    hint = if (searchQuery.isNotBlank()) "Try a different search term."
                    else "Type above or use \"note buy groceries\" in the command palette.",
                    accent = Mod.Skills,
                )
            } else {
                filtered.forEach { (id, text, ts) ->
                    val date = Instant.ofEpochMilli(ts).atZone(ZoneId.systemDefault())
                    val isEditing = editingId == id

                    Panel(Modifier.fillMaxWidth(), corner = 14.dp) {
                        if (isEditing) {
                            Column(Modifier.padding(12.dp)) {
                                OutlinedTextField(
                                    value = editText, onValueChange = { editText = it.take(com.ascend.lifeos.data.Prefs.int(ctx, com.ascend.lifeos.data.Prefs.NOTE_CHAR_LIMIT, 1000)) },
                                    singleLine = false, maxLines = 6, minLines = 2,
                                    modifier = Modifier.fillMaxWidth(),
                                    textStyle = TextStyle(color = TextPrimary, fontFamily = Body, fontSize = FS.s13),
                                )
                                Spacer(Modifier.height(8.dp))
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Box(
                                        Modifier.clip(RoundedCornerShape(10.dp))
                                            .background(Mod.Skills)
                                            .pressScale {
                                                LifeStores.editNote(ctx, id, editText)
                                                editingId = null
                                                com.ascend.lifeos.ui.kit.AppFeedback.show("Note updated")
                                            }
                                            .padding(horizontal = 14.dp, vertical = 8.dp),
                                    ) { Text("Update", color = Void, fontFamily = Body, fontSize = FS.s12, fontWeight = FontWeight.Bold) }
                                    Box(
                                        Modifier.clip(RoundedCornerShape(10.dp))
                                            .background(Ivory.copy(alpha = 0.06f))
                                            .pressScale { editingId = null }
                                            .padding(horizontal = 14.dp, vertical = 8.dp),
                                    ) { Text("Cancel", color = TextDim, fontFamily = Body, fontSize = FS.s12, fontWeight = FontWeight.Bold) }
                                }
                            }
                        } else {
                            Row(Modifier.padding(14.dp), verticalAlignment = Alignment.Top) {
                                Column(Modifier.weight(1f)) {
                                    Text(text, color = TextPrimary, fontFamily = Body, fontSize = FS.s13, lineHeight = 20.sp)
                                    Spacer(Modifier.height(4.dp))
                                    Text(date.format(fmt), color = TextDim, fontFamily = Body, fontSize = FS.s10)
                                }
                                Spacer(Modifier.width(8.dp))
                                Icon(
                                    Icons.Rounded.Edit, "Edit note", tint = TextDim.copy(alpha = 0.4f),
                                    modifier = Modifier.size(16.dp).clip(CircleShape)
                                        .clickable { editingId = id; editText = text },
                                )
                                Spacer(Modifier.width(8.dp))
                                Icon(
                                    Icons.Rounded.Delete, "Delete note",
                                    tint = if (armedNote == id) Crit else TextDim.copy(alpha = 0.3f),
                                    modifier = Modifier.size(16.dp).clip(CircleShape)
                                        .clickable {
                                            if (armedNote == id) {
                                                LifeStores.deleteNote(ctx, id)
                                                armedNote = null
                                                com.ascend.lifeos.ui.kit.AppFeedback.show("Note deleted")
                                            } else {
                                                armedNote = id
                                            }
                                        },
                                )
                            }
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                }
            }
        }
    }
}
