package com.ascend.lifeos.data

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.sin

/**
 * JARVIS interface sounds — synthesized at runtime (sine partials + exponential
 * decay), so the APK ships zero audio assets. Everything behind Prefs.SOUNDS_ON.
 *
 *   confirm  — soft two-note blip (logging, saves)
 *   levelUp  — rising triad (PR, level-up, skill unlock)
 *   focus    — single deep pad (focus session start)
 */
object SoundFx {
    private const val RATE = 22050

    fun confirm(ctx: Context) = play(ctx, tone(660.0, 90) + tone(990.0, 120))
    fun levelUp(ctx: Context) = play(ctx, tone(523.25, 110) + tone(659.25, 110) + tone(783.99, 200))
    fun focus(ctx: Context) = play(ctx, tone(196.0, 420, harmonics = true))

    private fun tone(freq: Double, ms: Int, harmonics: Boolean = false): ShortArray {
        val n = RATE * ms / 1000
        return ShortArray(n) { i ->
            val t = i.toDouble() / RATE
            val env = exp(-4.5 * i / n)                       // exponential decay
            var s = sin(2 * PI * freq * t)
            if (harmonics) s = s * 0.7 + sin(2 * PI * freq * 2 * t) * 0.2 + sin(2 * PI * freq * 3 * t) * 0.1
            (s * env * 9000).toInt().toShort()
        }
    }

    private fun play(ctx: Context, pcm: ShortArray) {
        if (!Prefs.bool(ctx, Prefs.SOUNDS_ON, false)) return
        Thread {
            runCatching {
                val track = AudioTrack.Builder()
                    .setAudioAttributes(
                        AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
                            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                            .build(),
                    )
                    .setAudioFormat(
                        AudioFormat.Builder()
                            .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                            .setSampleRate(RATE)
                            .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                            .build(),
                    )
                    .setBufferSizeInBytes(pcm.size * 2)
                    .setTransferMode(AudioTrack.MODE_STATIC)
                    .build()
                track.write(pcm, 0, pcm.size)
                track.play()
                Thread.sleep(pcm.size * 1000L / RATE + 150)
                track.release()
            }
        }.start()
    }
}
