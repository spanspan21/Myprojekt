package com.ascend.lifeos.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

// A tuned type scale. Tight tracking + strong weights on big numbers reads as
// "premium" even with the system font (a custom font can be swapped in later).
private val F = FontFamily.Default

val AscendType = Typography(
    displayLarge = TextStyle(fontFamily = F, fontWeight = FontWeight.ExtraBold, fontSize = 40.sp, lineHeight = 42.sp, letterSpacing = (-1).sp),
    displayMedium = TextStyle(fontFamily = F, fontWeight = FontWeight.ExtraBold, fontSize = 30.sp, lineHeight = 34.sp, letterSpacing = (-0.5).sp),
    headlineLarge = TextStyle(fontFamily = F, fontWeight = FontWeight.Bold, fontSize = 26.sp, lineHeight = 30.sp, letterSpacing = (-0.4).sp),
    headlineMedium = TextStyle(fontFamily = F, fontWeight = FontWeight.Bold, fontSize = 22.sp, lineHeight = 26.sp, letterSpacing = (-0.3).sp),
    titleLarge = TextStyle(fontFamily = F, fontWeight = FontWeight.Bold, fontSize = 17.sp, lineHeight = 22.sp, letterSpacing = (-0.2).sp),
    titleMedium = TextStyle(fontFamily = F, fontWeight = FontWeight.SemiBold, fontSize = 15.sp, lineHeight = 20.sp),
    bodyLarge = TextStyle(fontFamily = F, fontWeight = FontWeight.Normal, fontSize = 15.sp, lineHeight = 22.sp),
    bodyMedium = TextStyle(fontFamily = F, fontWeight = FontWeight.Normal, fontSize = 13.5.sp, lineHeight = 20.sp),
    labelLarge = TextStyle(fontFamily = F, fontWeight = FontWeight.SemiBold, fontSize = 13.sp, lineHeight = 16.sp),
    labelMedium = TextStyle(fontFamily = F, fontWeight = FontWeight.SemiBold, fontSize = 11.sp, lineHeight = 14.sp, letterSpacing = 0.4.sp),
    labelSmall = TextStyle(fontFamily = F, fontWeight = FontWeight.Bold, fontSize = 10.sp, lineHeight = 13.sp, letterSpacing = 1.6.sp),
)
