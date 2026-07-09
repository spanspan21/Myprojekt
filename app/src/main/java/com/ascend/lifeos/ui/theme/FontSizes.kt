package com.ascend.lifeos.ui.theme

import androidx.compose.ui.unit.sp

/**
 * Named font-size tokens (audit A1). Every distinct size the app used inline is
 * named here, so screens reference `FS.sN` instead of scattering `fontSize = N.sp`
 * literals (the audit found ~1,100 such inline declarations bypassing the type
 * system). Values are byte-for-byte identical to the old literals — this migration
 * is purely mechanical and changes nothing visually; it makes the sizes greppable,
 * countable and refactorable from ONE place, which is the point. Consolidating the
 * long tail toward the semantic JarvisText roles is a follow-up on top of this.
 */
object FS {
    val s7 = 7.sp
    val s7_5 = 7.5.sp
    val s8 = 8.sp
    val s8_5 = 8.5.sp
    val s9 = 9.sp
    val s9_5 = 9.5.sp
    val s10 = 10.sp
    val s10_5 = 10.5.sp
    val s11 = 11.sp
    val s11_5 = 11.5.sp
    val s12 = 12.sp
    val s12_5 = 12.5.sp
    val s13 = 13.sp
    val s13_5 = 13.5.sp
    val s14 = 14.sp
    val s14_5 = 14.5.sp
    val s15 = 15.sp
    val s15_5 = 15.5.sp
    val s16 = 16.sp
    val s17 = 17.sp
    val s18 = 18.sp
    val s19 = 19.sp
    val s20 = 20.sp
    val s21 = 21.sp
    val s22 = 22.sp
    val s23 = 23.sp
    val s24 = 24.sp
    val s26 = 26.sp
    val s27 = 27.sp
    val s28 = 28.sp
    val s30 = 30.sp
    val s32 = 32.sp
    val s34 = 34.sp
    val s36 = 36.sp
    val s40 = 40.sp
    val s42 = 42.sp
    val s46 = 46.sp
    val s48 = 48.sp
    val s56 = 56.sp
    val s64 = 64.sp
}
