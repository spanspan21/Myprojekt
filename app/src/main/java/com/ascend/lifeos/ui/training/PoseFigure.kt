package com.ascend.lifeos.ui.training

import androidx.compose.foundation.Canvas
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.runtime.Composable

// ─── Pose figures — one clean pictogram per movement ─────────────────────────
// Every exercise, skill and stretch gets a little line-figure showing the
// position, the same visual language as the body heat-map. Joints live in a
// 0..100 grid (x right, y down); the composable scales them into the canvas.

enum class Pose {
    STANDING, PUSHUP, PIKE_PUSHUP, HANDSTAND, DIP, PULLUP, ROW, MUSCLE_UP,
    PLANK, HOLLOW, LSIT, LEG_RAISE, SQUAT, PISTOL, LUNGE, GLUTE_BRIDGE,
    CALF_RAISE, PLANCHE, FRONT_LEVER, HANDSTAND_HOLD,
    // stretches
    HAMSTRING_STRETCH, HIP_FLEXOR_STRETCH, SHOULDER_STRETCH, COBRA,
    CHILD_POSE, PIGEON, QUAD_STRETCH, CHEST_STRETCH,
}

/** Head (center + radius) and the polylines that make the body; props are dim. */
private data class Art(
    val head: Offset,
    val headR: Float,
    val body: List<List<Offset>>,
    val props: List<List<Offset>> = emptyList(),
)

private fun p(x: Float, y: Float) = Offset(x / 100f, y / 100f)
// Offset is a value class → can't be a vararg; small fixed overloads instead.
private fun line(a: Offset, b: Offset) = listOf(a, b)
private fun line(a: Offset, b: Offset, c: Offset) = listOf(a, b, c)
private fun line(a: Offset, b: Offset, c: Offset, d: Offset) = listOf(a, b, c, d)

// floor / bar / parallette helpers
private val FLOOR = line(p(8f, 92f), p(92f, 92f))
private fun bar(y: Float) = line(p(12f, y), p(88f, y))

private fun art(pose: Pose): Art = when (pose) {
    Pose.STANDING -> Art(
        p(50f, 16f), 9f,
        listOf(
            line(p(50f, 25f), p(50f, 58f)),                       // spine
            line(p(42f, 52f), p(50f, 34f), p(58f, 52f)),          // arms
            line(p(43f, 88f), p(50f, 58f), p(57f, 88f)),          // legs
        ),
        listOf(FLOOR),
    )
    Pose.PUSHUP -> Art(
        p(20f, 44f), 7f,
        listOf(
            line(p(27f, 47f), p(62f, 58f), p(88f, 66f)),          // shoulder→hip→feet
            line(p(27f, 47f), p(24f, 66f), p(28f, 84f)),          // arm to floor
        ),
        listOf(FLOOR),
    )
    Pose.PIKE_PUSHUP -> Art(
        p(30f, 40f), 7f,
        listOf(
            line(p(36f, 44f), p(52f, 34f)),                        // shoulders up to pike apex
            line(p(52f, 34f), p(80f, 82f)),                        // hips high → legs down
            line(p(36f, 44f), p(30f, 62f), p(34f, 84f)),           // arm to floor
        ),
        listOf(FLOOR),
    )
    Pose.HANDSTAND, Pose.HANDSTAND_HOLD -> Art(
        p(50f, 82f), 7f,
        listOf(
            line(p(50f, 75f), p(50f, 30f)),                        // inverted spine
            line(p(44f, 86f), p(50f, 72f), p(56f, 86f)),           // arms to floor
            line(p(45f, 12f), p(50f, 30f), p(55f, 12f)),           // legs up
        ),
        listOf(FLOOR),
    )
    Pose.DIP -> Art(
        p(50f, 24f), 7f,
        listOf(
            line(p(50f, 32f), p(52f, 60f)),                        // upright torso
            line(p(42f, 46f), p(50f, 34f), p(40f, 40f)),           // bent arms on bars
            line(p(52f, 60f), p(58f, 78f), p(56f, 88f)),           // tucked legs
        ),
        listOf(line(p(30f, 46f), p(46f, 46f)), line(p(54f, 40f), p(72f, 40f))), // parallettes
    )
    Pose.PULLUP, Pose.MUSCLE_UP -> Art(
        p(50f, 34f), 7f,
        listOf(
            line(p(50f, 41f), p(50f, 70f)),                        // hanging torso
            line(p(42f, 20f), p(50f, 36f), p(58f, 20f)),           // arms up to bar
            line(p(46f, 88f), p(50f, 70f), p(54f, 88f)),           // legs
        ),
        listOf(bar(18f)),
    )
    Pose.ROW -> Art(
        p(22f, 40f), 7f,
        listOf(
            line(p(29f, 43f), p(78f, 62f)),                        // horizontal body
            line(p(40f, 20f), p(40f, 48f)),                        // arms up to bar
            line(p(29f, 43f), p(40f, 48f)),                        // shoulder link
        ),
        listOf(bar(18f), FLOOR),
    )
    Pose.PLANK -> Art(
        p(20f, 50f), 7f,
        listOf(
            line(p(27f, 53f), p(86f, 70f)),                        // straight body
            line(p(27f, 53f), p(24f, 72f)),                        // forearm down
            line(p(24f, 72f), p(40f, 72f)),                        // forearm on floor
        ),
        listOf(FLOOR),
    )
    Pose.HOLLOW -> Art(
        p(24f, 62f), 7f,
        listOf(
            line(p(30f, 60f), p(52f, 52f), p(78f, 44f)),           // banana curve, legs up
            line(p(30f, 60f), p(20f, 44f)),                        // arms overhead
        ),
        listOf(FLOOR),
    )
    Pose.LSIT -> Art(
        p(42f, 30f), 7f,
        listOf(
            line(p(42f, 37f), p(44f, 58f)),                        // upright torso
            line(p(44f, 58f), p(82f, 58f)),                        // legs straight out (L)
            line(p(38f, 44f), p(38f, 58f)),                        // arms pressing down
        ),
        listOf(line(p(30f, 60f), p(46f, 60f)), line(p(30f, 60f), p(30f, 60f))),
    )
    Pose.LEG_RAISE -> Art(
        p(50f, 24f), 7f,
        listOf(
            line(p(50f, 31f), p(50f, 58f)),                        // hanging torso
            line(p(44f, 12f), p(50f, 26f), p(56f, 12f)),           // arms to bar
            line(p(50f, 58f), p(72f, 50f)),                        // legs raised to L
        ),
        listOf(bar(10f)),
    )
    Pose.SQUAT -> Art(
        p(50f, 22f), 8f,
        listOf(
            line(p(50f, 30f), p(52f, 52f)),                        // torso leaning
            line(p(40f, 40f), p(50f, 34f), p(60f, 40f)),           // arms front
            line(p(52f, 52f), p(64f, 58f), p(60f, 88f)),           // deep-bent leg
        ),
        listOf(FLOOR),
    )
    Pose.PISTOL -> Art(
        p(46f, 22f), 8f,
        listOf(
            line(p(46f, 30f), p(48f, 52f)),                        // torso
            line(p(38f, 42f), p(48f, 36f), p(70f, 40f)),           // arms + one leg forward
            line(p(48f, 52f), p(60f, 60f), p(56f, 88f)),           // squatting leg
            line(p(48f, 52f), p(82f, 44f)),                        // free leg out front
        ),
        listOf(FLOOR),
    )
    Pose.LUNGE -> Art(
        p(46f, 20f), 8f,
        listOf(
            line(p(46f, 28f), p(48f, 54f)),                        // upright torso
            line(p(48f, 54f), p(34f, 70f), p(34f, 88f)),           // front leg bent
            line(p(48f, 54f), p(66f, 72f), p(74f, 88f)),           // back leg extended
        ),
        listOf(FLOOR),
    )
    Pose.GLUTE_BRIDGE -> Art(
        p(20f, 66f), 7f,
        listOf(
            line(p(26f, 64f), p(50f, 54f)),                        // torso lifted
            line(p(50f, 54f), p(64f, 70f), p(64f, 86f)),           // bent legs, feet down
        ),
        listOf(FLOOR),
    )
    Pose.CALF_RAISE -> Art(
        p(50f, 16f), 8f,
        listOf(
            line(p(50f, 24f), p(50f, 56f)),                        // straight up
            line(p(43f, 46f), p(50f, 30f), p(57f, 46f)),           // arms
            line(p(50f, 56f), p(50f, 82f)),                        // legs straight
            line(p(46f, 82f), p(54f, 82f)),                        // on toes (short base)
        ),
        listOf(FLOOR),
    )
    Pose.PLANCHE -> Art(
        p(22f, 52f), 7f,
        listOf(
            line(p(28f, 54f), p(84f, 46f)),                        // body horizontal, parallel to floor
            line(p(34f, 54f), p(34f, 78f)),                        // straight arms pressing down
        ),
        listOf(FLOOR),
    )
    Pose.FRONT_LEVER -> Art(
        p(24f, 46f), 7f,
        listOf(
            line(p(30f, 47f), p(84f, 52f)),                        // body horizontal under bar
            line(p(40f, 18f), p(40f, 49f)),                        // straight arms up to bar
        ),
        listOf(bar(16f)),
    )
    // ── stretches ──
    Pose.HAMSTRING_STRETCH -> Art(
        p(26f, 34f), 7f,
        listOf(
            line(p(31f, 38f), p(40f, 56f)),                        // folded torso
            line(p(40f, 56f), p(84f, 62f)),                        // straight legs out
            line(p(31f, 38f), p(70f, 58f)),                        // reaching arms to feet
        ),
        listOf(FLOOR),
    )
    Pose.QUAD_STRETCH -> Art(
        p(50f, 16f), 8f,
        listOf(
            line(p(50f, 24f), p(50f, 56f)),                        // standing torso
            line(p(50f, 56f), p(48f, 82f)),                        // stance leg
            line(p(50f, 56f), p(64f, 66f), p(52f, 52f)),           // heel pulled to glute
            line(p(50f, 34f), p(60f, 58f)),                        // hand grabbing foot
        ),
        listOf(FLOOR),
    )
    Pose.HIP_FLEXOR_STRETCH -> Art(
        p(40f, 26f), 8f,
        listOf(
            line(p(40f, 34f), p(42f, 56f)),                        // tall torso
            line(p(42f, 56f), p(30f, 72f), p(30f, 88f)),           // front knee bent
            line(p(42f, 56f), p(66f, 76f), p(84f, 86f)),           // back leg extended (lunge)
            line(p(40f, 40f), p(44f, 26f)),                        // arm up
        ),
        listOf(FLOOR),
    )
    Pose.PIGEON -> Art(
        p(24f, 40f), 7f,
        listOf(
            line(p(30f, 43f), p(46f, 58f)),                        // torso forward-lean
            line(p(46f, 58f), p(70f, 66f)),                        // front shin across
            line(p(46f, 58f), p(80f, 82f)),                        // back leg extended
        ),
        listOf(FLOOR),
    )
    Pose.COBRA -> Art(
        p(24f, 46f), 7f,
        listOf(
            line(p(30f, 48f), p(46f, 62f)),                        // chest lifted
            line(p(46f, 62f), p(86f, 78f)),                        // legs flat
            line(p(30f, 48f), p(30f, 72f)),                        // arms pressing up
        ),
        listOf(FLOOR),
    )
    Pose.CHILD_POSE -> Art(
        p(78f, 66f), 7f,
        listOf(
            line(p(72f, 68f), p(40f, 78f)),                        // folded torso down
            line(p(40f, 78f), p(66f, 82f)),                        // tucked legs
            line(p(72f, 68f), p(20f, 74f)),                        // arms reaching forward
        ),
        listOf(FLOOR),
    )
    Pose.SHOULDER_STRETCH -> Art(
        p(50f, 16f), 8f,
        listOf(
            line(p(50f, 24f), p(50f, 58f)),                        // standing
            line(p(50f, 30f), p(74f, 34f)),                        // one arm across body
            line(p(60f, 40f), p(74f, 34f)),                        // other arm pulling it
            line(p(44f, 88f), p(50f, 58f), p(56f, 88f)),           // legs
        ),
        listOf(FLOOR),
    )
    Pose.CHEST_STRETCH -> Art(
        p(50f, 16f), 8f,
        listOf(
            line(p(50f, 24f), p(50f, 58f)),                        // standing tall
            line(p(30f, 34f), p(50f, 30f), p(70f, 34f)),           // arms open wide back
            line(p(44f, 88f), p(50f, 58f), p(56f, 88f)),           // legs
        ),
        listOf(FLOOR),
    )
}

/**
 * A clean line-figure of the movement. [color] is the figure; props (floor, bar,
 * parallettes) are drawn dim so the body reads first.
 */
@Composable
fun PoseFigure(
    pose: Pose,
    modifier: Modifier = Modifier,
    color: Color = Color.White,
    strokeWidth: Float = 3.2f,
) {
    val a = art(pose)
    Canvas(modifier) {
        val w = size.width; val h = size.height
        // keep the figure square-ish and centred within the canvas
        val s = minOf(w, h)
        val ox = (w - s) / 2f; val oy = (h - s) / 2f
        fun map(o: Offset) = Offset(ox + o.x * s, oy + o.y * s)

        val body = Stroke(width = strokeWidth * (s / 100f), cap = StrokeCap.Round, join = StrokeJoin.Round)
        val prop = Stroke(width = strokeWidth * 0.8f * (s / 100f), cap = StrokeCap.Round)

        // props first, dim
        a.props.forEach { poly ->
            drawPolyline(poly.map(::map), color.copy(alpha = 0.22f), prop)
        }
        // body
        a.body.forEach { poly ->
            drawPolyline(poly.map(::map), color, body)
        }
        // head
        drawCircle(color, radius = a.headR * (s / 100f), center = map(a.head), style = body)
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawPolyline(
    pts: List<Offset>, color: Color, stroke: Stroke,
) {
    if (pts.size < 2) return
    val path = Path().apply {
        moveTo(pts.first().x, pts.first().y)
        for (i in 1 until pts.size) lineTo(pts[i].x, pts[i].y)
    }
    drawPath(path, color, style = stroke)
}

/** Map an exercise/skill/stretch to its pose figure. */
fun poseFor(id: String, name: String): Pose {
    val k = (id + " " + name).lowercase()
    return when {
        "muscle" in k && "up" in k -> Pose.MUSCLE_UP
        "pull" in k && "up" in k -> Pose.PULLUP
        "chin" in k -> Pose.PULLUP
        "front" in k && "lever" in k -> Pose.FRONT_LEVER
        "planche" in k -> Pose.PLANCHE
        "handstand" in k && ("push" in k || "hspu" in k || "pike push" in k) -> Pose.PIKE_PUSHUP
        "handstand" in k -> Pose.HANDSTAND
        "pike" in k && "push" in k -> Pose.PIKE_PUSHUP
        "dip" in k -> Pose.DIP
        "row" in k || ("pull" in k && "horizontal" in k) -> Pose.ROW
        "l-sit" in k || "lsit" in k || "l sit" in k -> Pose.LSIT
        "leg raise" in k || "knee raise" in k || "toes to bar" in k || "hanging" in k -> Pose.LEG_RAISE
        "hollow" in k -> Pose.HOLLOW
        "plank" in k -> Pose.PLANK
        "push" in k -> Pose.PUSHUP
        "pistol" in k || "shrimp" in k -> Pose.PISTOL
        "lunge" in k -> Pose.LUNGE
        "squat" in k -> Pose.SQUAT
        "bridge" in k || "hip thrust" in k || "glute" in k -> Pose.GLUTE_BRIDGE
        "calf" in k || "heel raise" in k -> Pose.CALF_RAISE
        // stretches
        "hamstring" in k || "forward fold" in k || "pancake" in k -> Pose.HAMSTRING_STRETCH
        "hip flexor" in k || "couch" in k || "lunge stretch" in k -> Pose.HIP_FLEXOR_STRETCH
        "quad" in k && "stretch" in k -> Pose.QUAD_STRETCH
        "pigeon" in k -> Pose.PIGEON
        "cobra" in k || "backbend" in k -> Pose.COBRA
        "child" in k -> Pose.CHILD_POSE
        "shoulder" in k && "stretch" in k -> Pose.SHOULDER_STRETCH
        "chest" in k && "stretch" in k -> Pose.CHEST_STRETCH
        "doorway" in k || "pec" in k -> Pose.CHEST_STRETCH
        else -> Pose.STANDING
    }
}
