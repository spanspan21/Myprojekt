package com.ascend.lifeos.ui.insights

import android.content.ClipData
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.DashPathEffect
import android.graphics.Paint
import android.graphics.RadialGradient
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import androidx.compose.ui.graphics.toArgb
import androidx.core.content.res.ResourcesCompat
import com.ascend.lifeos.R
import com.ascend.lifeos.core.todayKey
import com.ascend.lifeos.data.Repo
import com.ascend.lifeos.ui.home.buildWeekStats
import com.ascend.lifeos.ui.theme.Mod
import com.ascend.lifeos.ui.theme.TextDim
import com.ascend.lifeos.ui.theme.TextMuted
import com.ascend.lifeos.ui.theme.TextPrimary
import com.ascend.lifeos.ui.theme.Void
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

// ─── Shareable week card — 1080×1920 PNG, drawn with android.graphics ────────
// Storage: no FileProvider is declared in the manifest, so the card is saved to
// MediaStore.Images under Pictures/JARVIS and shared by content Uri.

private const val W = 1080
private const val H = 1920
private const val MARGIN = 90f
private const val RIGHT = W - MARGIN

/**
 * Renders the current week (via [buildWeekStats]) onto a 1080×1920 card and
 * saves it to MediaStore.Images (Pictures/JARVIS). Returns the content Uri,
 * or null if rendering/saving failed.
 */
suspend fun renderWeekCard(ctx: Context): Uri? = withContext(Dispatchers.IO) {
    runCatching {
        val s = buildWeekStats(ctx)
        val keys = Repo.lastDayKeys(7)

        val chakra = ResourcesCompat.getFont(ctx, R.font.chakra_bold)
        val manrope = ResourcesCompat.getFont(ctx, R.font.manrope_regular)

        val bmp = Bitmap.createBitmap(W, H, Bitmap.Config.ARGB_8888)
        val c = Canvas(bmp)

        // ---- atmosphere ----
        c.drawColor(Void.toArgb())
        val glowTop = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = RadialGradient(
                120f, 140f, 950f,
                Mod.Home.copy(alpha = 0.13f).toArgb(), Mod.Home.copy(alpha = 0f).toArgb(),
                Shader.TileMode.CLAMP,
            )
        }
        c.drawRect(0f, 0f, W.toFloat(), H.toFloat(), glowTop)
        val glowBottom = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = RadialGradient(
                980f, 1700f, 850f,
                Mod.Body.copy(alpha = 0.08f).toArgb(), Mod.Body.copy(alpha = 0f).toArgb(),
                Shader.TileMode.CLAMP,
            )
        }
        c.drawRect(0f, 0f, W.toFloat(), H.toFloat(), glowBottom)

        // ---- wordmark ----
        c.drawText("JARVIS", MARGIN, 225f, textPaint(TextPrimary.toArgb(), 96f, chakra, ls = 0.28f))
        c.drawText("WEEKLY REPORT", MARGIN, 288f, textPaint(Mod.Home.toArgb(), 30f, chakra, ls = 0.35f))
        val range = runCatching {
            val f = DateTimeFormatter.ofPattern("d MMM", Locale.ENGLISH)
            val fy = DateTimeFormatter.ofPattern("d MMM yyyy", Locale.ENGLISH)
            "${LocalDate.parse(keys.first()).format(f)} — ${LocalDate.parse(keys.last()).format(fy)}"
        }.getOrDefault("")
        c.drawText(range, MARGIN, 338f, textPaint(TextMuted.toArgb(), 30f, manrope))
        c.drawLine(MARGIN, 392f, RIGHT, 392f, hairline())

        // ---- 2×2 module stats, big numbers in the module accents ----
        val colX = floatArrayOf(MARGIN, 570f)
        statBlock(
            c, colX[0], 500f, chakra, manrope, Mod.Train.toArgb(),
            "TRAIN", "${s.workouts}",
            "${fmtNum(s.totalReps)} reps · ${s.prCount} PR" + if (s.prCount == 1) "" else "s",
        )
        statBlock(
            c, colX[1], 500f, chakra, manrope, Mod.Body.toArgb(),
            "Ø SLEEP", s.sleepAvgMin?.let { hm(it) } ?: "—",
            s.rhrAvg?.let { "resting HR Ø $it bpm" } ?: "no resting HR data",
        )
        statBlock(
            c, colX[0], 840f, chakra, manrope, Mod.Fuel.toArgb(),
            "Ø KCAL", s.kcalAvg?.let { fmtNum(it) } ?: "—",
            (s.proteinAvg?.let { "protein Ø ${it}g · " } ?: "") + "goal ${fmtNum(s.kcalGoal)}",
        )
        statBlock(
            c, colX[1], 840f, chakra, manrope, Mod.Guard.toArgb(),
            "Ø SCREEN", s.screenAvgMin?.let { hm(it) } ?: "—",
            if (s.reclaimedMin > 0) "≈${s.reclaimedMin} min reclaimed" else "guard on watch",
        )

        // ---- sleep bars, last 7 nights ----
        c.drawText("SLEEP · LAST 7 NIGHTS", MARGIN, 1210f, textPaint(Mod.Body.toArgb(), 26f, chakra, ls = 0.2f))
        val baseY = 1520f
        val maxBarH = 240f
        val maxV = maxOf(s.sleepSeries.maxOrNull() ?: 0f, 480f)
        val barW = 92f
        val gap = ((RIGHT - MARGIN) - 7 * barW) / 6f
        // 8h reference line
        val refY = baseY - 480f / maxV * maxBarH
        c.drawLine(
            MARGIN, refY, RIGHT, refY,
            Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = 0x28FFFFFF; strokeWidth = 2f
                pathEffect = DashPathEffect(floatArrayOf(10f, 12f), 0f)
            },
        )
        val barPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Mod.Body.copy(alpha = 0.85f).toArgb() }
        val stubPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0x14FFFFFF }
        val dayPaint = textPaint(TextDim.toArgb(), 24f, manrope).apply { textAlign = Paint.Align.CENTER }
        s.sleepSeries.forEachIndexed { i, v ->
            val x = MARGIN + i * (barW + gap)
            if (v > 0f) {
                val h = (v / maxV * maxBarH).coerceAtLeast(10f)
                c.drawRoundRect(RectF(x, baseY - h, x + barW, baseY), 10f, 10f, barPaint)
            } else {
                c.drawRoundRect(RectF(x, baseY - 8f, x + barW, baseY), 4f, 4f, stubPaint)
            }
            val letter = runCatching {
                LocalDate.parse(keys[i]).dayOfWeek
                    .getDisplayName(java.time.format.TextStyle.NARROW, Locale.ENGLISH)
            }.getOrDefault("")
            c.drawText(letter, x + barW / 2f, baseY + 46f, dayPaint)
        }

        // ---- footer ----
        c.drawLine(MARGIN, 1700f, RIGHT, 1700f, hairline())
        c.drawText("ASCEND · LIFE OS", MARGIN, 1768f, textPaint(TextDim.toArgb(), 24f, chakra, ls = 0.5f))
        c.drawText(
            "STREAK ${Repo.data.profile.streak} DAYS", RIGHT, 1768f,
            textPaint(Mod.Home.toArgb(), 24f, chakra, ls = 0.5f).apply { textAlign = Paint.Align.RIGHT },
        )

        saveToMediaStore(ctx, bmp)
    }.getOrNull()
}

/** Fires ACTION_SEND (image/png) with a chooser for the rendered card. */
fun shareCard(ctx: Context, uri: Uri) {
    val send = Intent(Intent.ACTION_SEND).apply {
        type = "image/png"
        putExtra(Intent.EXTRA_STREAM, uri)
        clipData = ClipData.newRawUri("JARVIS weekly card", uri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    ctx.startActivity(
        Intent.createChooser(send, "Share weekly report").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
    )
}

// ─── drawing helpers ─────────────────────────────────────────────────────────

private fun textPaint(color: Int, size: Float, tf: Typeface?, ls: Float = 0f): Paint =
    Paint(Paint.ANTI_ALIAS_FLAG).apply {
        this.color = color
        textSize = size
        typeface = tf
        letterSpacing = ls
        fontFeatureSettings = "tnum"
    }

private fun hairline(): Paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
    color = 0x1EFFFFFF; strokeWidth = 1.5f
}

/** One module quadrant: small tracked label, huge accent number, muted sub line. */
private fun statBlock(
    c: Canvas,
    x: Float,
    top: Float,
    chakra: Typeface?,
    manrope: Typeface?,
    accent: Int,
    label: String,
    value: String,
    sub: String,
) {
    c.drawText(label, x, top, textPaint(accent, 26f, chakra, ls = 0.2f))
    c.drawText(value, x, top + 122f, textPaint(accent, 104f, chakra))
    c.drawText(sub, x, top + 174f, textPaint(TextMuted.toArgb(), 27f, manrope))
}

private fun hm(min: Int): String = "${min / 60}h ${(min % 60).toString().padStart(2, '0')}m"

private fun fmtNum(v: Int): String = String.format(Locale.US, "%,d", v)

// ─── storage ─────────────────────────────────────────────────────────────────

private fun saveToMediaStore(ctx: Context, bmp: Bitmap): Uri? {
    val resolver = ctx.contentResolver
    val values = ContentValues().apply {
        put(MediaStore.Images.Media.DISPLAY_NAME, "jarvis_week_${todayKey()}.png")
        put(MediaStore.Images.Media.MIME_TYPE, "image/png")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/JARVIS")
            put(MediaStore.Images.Media.IS_PENDING, 1)
        }
    }
    val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values) ?: return null
    var ok = false
    resolver.openOutputStream(uri)?.use { out ->
        ok = bmp.compress(Bitmap.CompressFormat.PNG, 100, out)
    }
    if (!ok) {
        resolver.delete(uri, null, null)
        return null
    }
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        values.clear()
        values.put(MediaStore.Images.Media.IS_PENDING, 0)
        resolver.update(uri, values, null, null)
    }
    return uri
}
