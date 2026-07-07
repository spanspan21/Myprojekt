package com.ascend.lifeos.data

import android.content.ContentValues
import android.content.Context
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * One-page monthly report PDF (A4), rendered on-device with android.graphics.pdf
 * and saved to Downloads/JARVIS via MediaStore — no storage permission needed.
 * Numbers come straight from the stores; a metric without data prints "—".
 */
object MonthlyPdf {

    suspend fun export(ctx: Context): Uri? = withContext(Dispatchers.IO) {
        runCatching {
            val doc = PdfDocument()
            val page = doc.startPage(PdfDocument.PageInfo.Builder(595, 842, 1).create()) // A4 @72dpi
            draw(ctx, page.canvas)
            doc.finishPage(page)

            val month = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM"))
            val name = "JARVIS_month_$month.pdf"
            val values = ContentValues().apply {
                put(MediaStore.Downloads.DISPLAY_NAME, name)
                put(MediaStore.Downloads.MIME_TYPE, "application/pdf")
                if (Build.VERSION.SDK_INT >= 29) {
                    put(MediaStore.Downloads.RELATIVE_PATH, "Download/JARVIS")
                    put(MediaStore.Downloads.IS_PENDING, 1)
                }
            }
            if (Build.VERSION.SDK_INT >= 29) {
                val resolver = ctx.contentResolver
                val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values) ?: return@runCatching null
                resolver.openOutputStream(uri)?.use { doc.writeTo(it) }
                doc.close()
                values.clear(); values.put(MediaStore.Downloads.IS_PENDING, 0)
                resolver.update(uri, values, null, null)
                uri
            } else {
                // MediaStore.Downloads is API 29+ — the field access alone threw
                // on Android 8/9 and runCatching turned it into "Export failed"
                @Suppress("DEPRECATION")
                val dir = java.io.File(
                    android.os.Environment.getExternalStoragePublicDirectory(android.os.Environment.DIRECTORY_DOWNLOADS),
                    "JARVIS",
                ).apply { mkdirs() }
                val f = java.io.File(dir, name)
                f.outputStream().use { doc.writeTo(it) }
                doc.close()
                android.net.Uri.fromFile(f)
            }
        }.getOrNull()
    }

    private fun draw(ctx: Context, c: android.graphics.Canvas) {
        val now = LocalDate.now()
        val monthStart = now.withDayOfMonth(1)
        val keys = Repo.lastDayKeys(now.dayOfMonth) // this month so far

        val ink = Paint().apply { color = Color.rgb(20, 22, 26); isAntiAlias = true }
        val dim = Paint().apply { color = Color.rgb(120, 126, 134); isAntiAlias = true }
        val accent = Paint().apply { color = Color.rgb(0, 150, 110); isAntiAlias = true }
        val line = Paint().apply { color = Color.rgb(225, 228, 232); strokeWidth = 0.8f }

        fun Paint.sized(sp: Float, bold: Boolean = false) = Paint(this).apply {
            textSize = sp
            typeface = if (bold) Typeface.create(Typeface.DEFAULT, Typeface.BOLD) else Typeface.DEFAULT
        }

        var y = 64f
        c.drawText("JARVIS", 48f, y, accent.sized(13f, true).apply { letterSpacing = 0.35f })
        y += 26f
        c.drawText(
            "Monthly Report — " + monthStart.format(DateTimeFormatter.ofPattern("MMMM yyyy", Locale.ENGLISH)),
            48f, y, ink.sized(24f, true),
        )
        y += 16f
        c.drawText("Generated ${now.format(DateTimeFormatter.ofPattern("dd.MM.yyyy"))} · data on device only", 48f, y, dim.sized(9f))
        y += 20f
        c.drawLine(48f, y, 547f, y, line)
        y += 30f

        // gather
        val sleeps = keys.mapNotNull { Repo.bodyDay(it)?.sleepMin }
        val rhrs = keys.mapNotNull { Repo.bodyDay(it)?.restingHr }
        val steps = keys.mapNotNull { Repo.bodyDay(it)?.steps }
        val kcals = keys.mapNotNull { k -> Repo.data.days[k]?.meals?.sumOf { it.kcal }?.takeIf { it > 0 } }
        val prots = keys.mapNotNull { k -> Repo.data.days[k]?.meals?.sumOf { it.protein }?.takeIf { it > 0 } }
        val weights = Repo.weightLog().filter {
            it.ts >= monthStart.toEpochDay() * 86_400_000L
        }
        val sets = keys.sumOf { k -> Repo.data.days[k]?.let { Repo.workoutSets(it) } ?: 0 }
        val sessions = runCatching {
            kotlinx.coroutines.runBlocking {
                com.ascend.lifeos.data.training.TrainingDatabase.get(ctx).dao()
                    .sessionCountSince(monthStart.toEpochDay() * 86_400_000L)
            }
        }.getOrDefault(0)

        fun avg(xs: List<Int>): String = if (xs.isEmpty()) "—" else "${xs.average().toInt()}"
        fun sleepStr(): String {
            if (sleeps.isEmpty()) return "—"
            val a = sleeps.average().toInt(); return "${a / 60}h ${a % 60}m"
        }

        val rows = listOf(
            Triple("TRAIN", "Workouts", "$sessions sessions"),
            Triple("", "Volume", if (sets > 0) "$sets quick-logged sets" else "see session log"),
            Triple("BODY", "Ø Sleep", sleepStr()),
            Triple("", "Ø Resting HR", avg(rhrs) + if (rhrs.isNotEmpty()) " bpm" else ""),
            Triple("", "Ø Steps", avg(steps)),
            Triple(
                "", "Weight",
                when {
                    weights.size >= 2 -> "%.1f → %.1f kg".format(weights.first().kg, weights.last().kg)
                    weights.size == 1 -> "%.1f kg".format(weights.first().kg)
                    else -> "—"
                },
            ),
            Triple("FUEL", "Ø Calories", avg(kcals) + if (kcals.isNotEmpty()) " kcal on ${kcals.size} logged days" else ""),
            Triple("", "Ø Protein", avg(prots) + if (prots.isNotEmpty()) " g" else ""),
            Triple("STREAK", "Current", "${Repo.data.profile.streak} days"),
        )

        var section = ""
        rows.forEach { (sec, label, value) ->
            if (sec.isNotBlank() && sec != section) {
                section = sec
                y += 10f
                c.drawText(sec, 48f, y, accent.sized(10f, true).apply { letterSpacing = 0.25f })
                y += 20f
            }
            c.drawText(label, 70f, y, dim.sized(11f))
            c.drawText(value, 260f, y, ink.sized(12f, true))
            y += 22f
        }

        y += 14f
        c.drawLine(48f, y, 547f, y, line)
        y += 24f

        // sleep bars for the month (one bar per day, honest gaps)
        c.drawText("SLEEP — this month", 48f, y, accent.sized(10f, true))
        y += 14f
        val barBottom = y + 90f
        val w = (499f / keys.size).coerceAtMost(14f)
        keys.forEachIndexed { i, k ->
            val min = Repo.bodyDay(k)?.sleepMin ?: return@forEachIndexed
            val h = (min / 600f * 90f).coerceAtMost(90f)
            val x = 48f + i * (499f / keys.size)
            c.drawRect(x, barBottom - h, x + w - 2f, barBottom, accent)
        }
        // 8h reference
        val ref = barBottom - (480f / 600f * 90f)
        val dash = Paint(line).apply { pathEffect = android.graphics.DashPathEffect(floatArrayOf(4f, 4f), 0f) }
        c.drawLine(48f, ref, 547f, ref, dash)
        c.drawText("8h", 550f - 18f, ref - 3f, dim.sized(8f))

        c.drawText("JARVIS — offline-first · no cloud · generated on device", 48f, 810f, dim.sized(8f))
    }
}
