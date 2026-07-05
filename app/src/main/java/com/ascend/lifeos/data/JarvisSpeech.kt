package com.ascend.lifeos.data

import android.content.Context
import android.speech.tts.TextToSpeech
import java.util.Locale

/**
 * Jarvis speaks — optional, off by default (Prefs.TTS_BRIEFING).
 * British English voice when available, because obviously.
 */
object JarvisSpeech {
    private var tts: TextToSpeech? = null
    private var ready = false

    fun speak(ctx: Context, text: String) {
        if (!Prefs.bool(ctx, Prefs.TTS_BRIEFING, false)) return
        if (tts == null) {
            tts = TextToSpeech(ctx.applicationContext) { status ->
                ready = status == TextToSpeech.SUCCESS
                if (ready) {
                    runCatching {
                        val uk = Locale.UK
                        if (tts?.isLanguageAvailable(uk) ?: -1 >= TextToSpeech.LANG_AVAILABLE) {
                            tts?.language = uk
                        }
                    }
                    tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "jarvis")
                }
            }
        } else if (ready) {
            tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "jarvis")
        }
    }

    /** The spoken morning line — same data the Home header shows. */
    fun briefingText(ctx: Context): String {
        val p = Repo.profile()
        val name = p.name.ifBlank { "operator" }
        val parts = ArrayList<String>()
        parts.add(JarvisVoice.greeting(name).removeSuffix("."))
        Repo.recoveryScore()?.let { parts.add("Recovery $it") }
        val debt = Repo.sleepDebtMin()
        if (debt > 120) parts.add("sleep debt ${debt / 60} hours")
        val kcal = Repo.today().meals.sumOf { it.kcal }
        if (kcal > 0) parts.add("$kcal calories logged")
        return parts.joinToString(". ") + ". All systems online."
    }

    fun shutdown() {
        runCatching { tts?.shutdown() }
        tts = null; ready = false
    }
}
