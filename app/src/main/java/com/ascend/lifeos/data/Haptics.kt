package com.ascend.lifeos.data

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager

/**
 * The haptic palette — five named moments, one API ladder. Every buzz in the
 * app speaks this language: tick < confirm < success < epic, plus warn.
 * Primitives on API 30/31+, predefined effects on 29+, waveforms down to 26.
 * All calls respect the Settings → Haptics toggle.
 */
object Haptics {

    private fun vibrator(ctx: Context): Vibrator? = runCatching {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            (ctx.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager).defaultVibrator
        } else {
            @Suppress("DEPRECATION") ctx.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        }
    }.getOrNull()

    private fun on(ctx: Context): Boolean = Prefs.bool(ctx, Prefs.HAPTICS_ON, true)

    private fun primitives(v: Vibrator, vararg ids: Int): Boolean =
        Build.VERSION.SDK_INT >= 31 && runCatching { v.areAllPrimitivesSupported(*ids) }.getOrDefault(false)

    /** Chip select, stepper step, pill switch, dock tap. Barely there. */
    fun tick(ctx: Context) {
        if (!on(ctx)) return
        val v = vibrator(ctx) ?: return
        runCatching {
            when {
                primitives(v, VibrationEffect.Composition.PRIMITIVE_LOW_TICK) ->
                    v.vibrate(
                        VibrationEffect.startComposition()
                            .addPrimitive(VibrationEffect.Composition.PRIMITIVE_LOW_TICK, 0.6f)
                            .compose(),
                    )
                Build.VERSION.SDK_INT >= 29 -> v.vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_TICK))
                else -> v.vibrate(VibrationEffect.createOneShot(10, 120))
            }
        }
    }

    /** Set logged, water +1, item checked off. A solid, short answer. */
    fun confirm(ctx: Context) {
        if (!on(ctx)) return
        val v = vibrator(ctx) ?: return
        runCatching {
            when {
                primitives(v, VibrationEffect.Composition.PRIMITIVE_TICK, VibrationEffect.Composition.PRIMITIVE_CLICK) ->
                    v.vibrate(
                        VibrationEffect.startComposition()
                            .addPrimitive(VibrationEffect.Composition.PRIMITIVE_TICK, 0.5f)
                            .addPrimitive(VibrationEffect.Composition.PRIMITIVE_CLICK, 0.9f, 40)
                            .compose(),
                    )
                Build.VERSION.SDK_INT >= 29 -> v.vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_CLICK))
                else -> v.vibrate(VibrationEffect.createOneShot(24, VibrationEffect.DEFAULT_AMPLITUDE))
            }
        }
    }

    /** Mission complete, goal reached, rule armed. A small rise. */
    fun success(ctx: Context) {
        if (!on(ctx)) return
        val v = vibrator(ctx) ?: return
        runCatching {
            when {
                primitives(v, VibrationEffect.Composition.PRIMITIVE_QUICK_RISE, VibrationEffect.Composition.PRIMITIVE_CLICK) ->
                    v.vibrate(
                        VibrationEffect.startComposition()
                            .addPrimitive(VibrationEffect.Composition.PRIMITIVE_QUICK_RISE, 0.7f)
                            .addPrimitive(VibrationEffect.Composition.PRIMITIVE_CLICK, 1f, 30)
                            .compose(),
                    )
                Build.VERSION.SDK_INT >= 29 -> v.vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_DOUBLE_CLICK))
                else -> v.vibrate(VibrationEffect.createWaveform(longArrayOf(0, 16, 60, 28), -1))
            }
        }
    }

    /** PR, level-up, streak milestone, Wrapped finale. The big one — rare. */
    fun epic(ctx: Context) {
        if (!on(ctx)) return
        val v = vibrator(ctx) ?: return
        runCatching {
            when {
                primitives(
                    v,
                    VibrationEffect.Composition.PRIMITIVE_SLOW_RISE,
                    VibrationEffect.Composition.PRIMITIVE_QUICK_RISE,
                    VibrationEffect.Composition.PRIMITIVE_THUD,
                ) ->
                    v.vibrate(
                        VibrationEffect.startComposition()
                            .addPrimitive(VibrationEffect.Composition.PRIMITIVE_SLOW_RISE, 0.6f)
                            .addPrimitive(VibrationEffect.Composition.PRIMITIVE_QUICK_RISE, 0.9f, 20)
                            .addPrimitive(VibrationEffect.Composition.PRIMITIVE_THUD, 1f, 60)
                            .compose(),
                    )
                Build.VERSION.SDK_INT >= 29 -> v.vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_HEAVY_CLICK))
                else -> v.vibrate(VibrationEffect.createWaveform(longArrayOf(0, 20, 40, 20, 40, 60), -1))
            }
        }
    }

    /** Budget 80%, deload signal, gate intercept. Two low knocks. */
    fun warn(ctx: Context) {
        if (!on(ctx)) return
        val v = vibrator(ctx) ?: return
        runCatching {
            when {
                primitives(v, VibrationEffect.Composition.PRIMITIVE_LOW_TICK) ->
                    v.vibrate(
                        VibrationEffect.startComposition()
                            .addPrimitive(VibrationEffect.Composition.PRIMITIVE_LOW_TICK, 1f)
                            .addPrimitive(VibrationEffect.Composition.PRIMITIVE_LOW_TICK, 1f, 120)
                            .compose(),
                    )
                else -> v.vibrate(VibrationEffect.createWaveform(longArrayOf(0, 30, 110, 30), -1))
            }
        }
    }
}
