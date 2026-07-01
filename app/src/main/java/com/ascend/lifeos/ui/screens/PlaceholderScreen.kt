package com.ascend.lifeos.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ascend.lifeos.ui.theme.Accent
import com.ascend.lifeos.ui.theme.AccentSoft
import com.ascend.lifeos.ui.theme.Line
import com.ascend.lifeos.ui.theme.SurfaceHi
import com.ascend.lifeos.ui.theme.TextMuted
import com.ascend.lifeos.ui.theme.TextPrimary

@Composable
fun PlaceholderScreen(title: String, subtitle: String) {
    Column(
        Modifier.fillMaxSize().statusBarsPadding().padding(horizontal = 17.dp).padding(top = 14.dp),
    ) {
        Text(title, color = TextPrimary, fontSize = 26.sp, fontWeight = FontWeight.ExtraBold)
        Spacer(Modifier.height(2.dp))
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Box(
                    Modifier.size(66.dp).clip(RoundedCornerShape(20.dp)).background(AccentSoft)
                        .border(1.dp, Line, RoundedCornerShape(20.dp)),
                    contentAlignment = Alignment.Center,
                ) { Icon(Icons.Rounded.Bolt, null, tint = Accent, modifier = Modifier.size(30.dp)) }
                Spacer(Modifier.height(16.dp))
                Text("In Arbeit", color = TextPrimary, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(6.dp))
                Text(subtitle, color = TextMuted, fontSize = 13.sp, textAlign = TextAlign.Center)
            }
        }
    }
}
