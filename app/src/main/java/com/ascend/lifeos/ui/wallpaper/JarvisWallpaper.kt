package com.ascend.lifeos.ui.wallpaper

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RadialGradient
import android.graphics.Shader
import android.os.Handler
import android.os.Looper
import android.service.wallpaper.WallpaperService
import android.view.SurfaceHolder
import kotlin.math.sin

/**
 * JARVIS live wallpaper — the app's void with two slow-breathing accent
 * nebulae. Renders at 20 fps only while visible; a static frame costs nothing
 * when the launcher is covered. No sensors, no location, no network.
 */
class JarvisWallpaper : WallpaperService() {
    override fun onCreateEngine(): Engine = NebulaEngine()

    private inner class NebulaEngine : Engine() {
        private val handler = Handler(Looper.getMainLooper())
        private var visible = false
        private var t = 0f

        private val drawRunnable = object : Runnable {
            override fun run() {
                drawFrame()
                if (visible) handler.postDelayed(this, 50)
            }
        }

        override fun onVisibilityChanged(v: Boolean) {
            visible = v
            handler.removeCallbacks(drawRunnable)
            if (v) handler.post(drawRunnable)
        }

        override fun onSurfaceDestroyed(holder: SurfaceHolder) {
            super.onSurfaceDestroyed(holder)
            visible = false
            handler.removeCallbacks(drawRunnable)
        }

        private fun drawFrame() {
            val holder = surfaceHolder ?: return
            var c: Canvas? = null
            try {
                c = holder.lockCanvas() ?: return
                t += 0.05f
                render(c)
            } finally {
                c?.let { runCatching { holder.unlockCanvasAndPost(it) } }
            }
        }

        private fun render(c: Canvas) {
            val w = c.width.toFloat()
            val h = c.height.toFloat()
            c.drawColor(Color.rgb(5, 5, 5))

            // slow breathing: 12 s cycle, drift with parallax
            val breath = (sin(t / 3.8) * 0.5f + 0.5f).toFloat()      // 0..1
            val drift = (sin(t / 9.0) * 0.06f).toFloat()

            fun nebula(cx: Float, cy: Float, r: Float, color: Int, alpha: Int) {
                val p = Paint().apply {
                    isAntiAlias = true
                    shader = RadialGradient(
                        cx, cy, r,
                        intArrayOf(
                            Color.argb(alpha, Color.red(color), Color.green(color), Color.blue(color)),
                            Color.TRANSPARENT,
                        ),
                        null, Shader.TileMode.CLAMP,
                    )
                }
                c.drawCircle(cx, cy, r, p)
            }

            // mint — top left, breathes
            nebula(
                w * (0.22f + drift), h * 0.16f,
                w * (0.55f + breath * 0.08f),
                Color.rgb(52, 224, 161), (14 + breath * 10).toInt(),
            )
            // violet — bottom right, counter-phase
            nebula(
                w * (0.85f - drift), h * 0.78f,
                w * (0.62f + (1f - breath) * 0.08f),
                Color.rgb(124, 140, 248), (11 + (1f - breath) * 8).toInt(),
            )
            // faint hairline ring center-right, the HUD signature
            val ring = Paint().apply {
                isAntiAlias = true; style = Paint.Style.STROKE; strokeWidth = 1.2f
                color = Color.argb(26, 255, 255, 255)
            }
            c.drawCircle(w * 0.78f, h * 0.30f, w * 0.20f + breath * 8f, ring)
        }
    }
}
