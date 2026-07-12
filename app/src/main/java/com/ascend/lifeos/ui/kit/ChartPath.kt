package com.ascend.lifeos.ui.kit

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope

/** Catmull-Rom → Bézier, die Kurven-Signatur des Web-Dashboards (viz-Kit):
 *  weiche, organische Linien statt harter Polyline-Ecken. k=6 ist der
 *  Standard-Spannungsfaktor — die Kurve läuft DURCH jeden Messpunkt. */
fun smoothPath(points: List<Offset>): Path {
    val p = Path()
    if (points.isEmpty()) return p
    p.moveTo(points[0].x, points[0].y)
    if (points.size == 1) return p
    for (i in 0 until points.size - 1) {
        val p0 = points[maxOf(i - 1, 0)]
        val p1 = points[i]
        val p2 = points[i + 1]
        val p3 = points[minOf(i + 2, points.lastIndex)]
        val c1 = Offset(p1.x + (p2.x - p0.x) / 6f, p1.y + (p2.y - p0.y) / 6f)
        val c2 = Offset(p2.x - (p3.x - p1.x) / 6f, p2.y - (p3.y - p1.y) / 6f)
        p.cubicTo(c1.x, c1.y, c2.x, c2.y, p2.x, p2.y)
    }
    return p
}

/** Endpunkt-Halo wie im Web-Dashboard: weicher Ring + Kern — „hier steht
 *  der jüngste Wert". radiusPx = Kern; der Halo ist 2× so groß. */
fun DrawScope.endpointHalo(color: Color, at: Offset, radiusPx: Float) {
    drawCircle(color.copy(alpha = 0.22f), radiusPx * 2f, at)
    drawCircle(color, radiusPx, at)
}
