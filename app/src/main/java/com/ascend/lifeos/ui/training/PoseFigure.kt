package com.ascend.lifeos.ui.training

import androidx.compose.foundation.Canvas
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.runtime.Composable

// ─── Pose figures — an articulated athlete, one per movement ─────────────────
// A proper skeleton (head, spine, two arms, two legs) with anatomically-placed
// joints, drawn as solid rounded limbs so it reads like a clean training-app
// pictogram, not a stick doodle. The far-side limbs are dimmed for depth.
// Coordinates live in a 0..100 grid (x right, y down).

enum class Pose {
    STANDING, PUSHUP, PIKE_PUSHUP, HANDSTAND, DIP, PULLUP, ROW, MUSCLE_UP,
    PLANK, HOLLOW, LSIT, LEG_RAISE, SQUAT, PISTOL, LUNGE, GLUTE_BRIDGE,
    CALF_RAISE, PLANCHE, FRONT_LEVER,
    HAMSTRING_STRETCH, HIP_FLEXOR_STRETCH, SHOULDER_STRETCH, COBRA,
    CHILD_POSE, PIGEON, QUAD_STRETCH, CHEST_STRETCH,
}

private fun o(x: Float, y: Float) = Offset(x, y)

/**
 * A full articulated figure. Arms/legs are [shoulder/hip, elbow/knee, wrist/ankle].
 * [armFar]/[legFar] are the away-side limbs (drawn dim); null hides them.
 */
private class Fig(
    val head: Offset, val headR: Float,
    val neck: Offset, val hip: Offset,
    val armNear: List<Offset>, val armFar: List<Offset>?,
    val legNear: List<Offset>, val legFar: List<Offset>?,
    val props: List<List<Offset>> = emptyList(),
)

private fun floor(y: Float) = listOf(o(6f, y), o(94f, y))
private fun bar(y: Float) = listOf(o(14f, y), o(86f, y))

private fun fig(pose: Pose): Fig = when (pose) {
    Pose.STANDING -> Fig(
        o(50f, 15f), 9f, o(50f, 26f), o(50f, 58f),
        armNear = listOf(o(48f, 30f), o(41f, 44f), o(38f, 57f)),
        armFar = listOf(o(52f, 30f), o(59f, 44f), o(62f, 57f)),
        legNear = listOf(o(48f, 58f), o(45f, 74f), o(44f, 90f)),
        legFar = listOf(o(52f, 58f), o(55f, 74f), o(56f, 90f)),
        props = listOf(floor(91f)),
    )
    Pose.PUSHUP -> Fig(
        o(19f, 55f), 6f, o(27f, 58f), o(60f, 66f),
        armNear = listOf(o(29f, 59f), o(27f, 72f), o(30f, 84f)),   // bent, hand on floor
        armFar = listOf(o(29f, 60f), o(28f, 73f), o(31f, 84f)),
        legNear = listOf(o(60f, 66f), o(74f, 70f), o(88f, 82f)),   // to toes on floor
        legFar = listOf(o(60f, 67f), o(74f, 71f), o(88f, 83f)),
        props = listOf(floor(85f)),
    )
    Pose.PLANK -> Fig(
        o(19f, 56f), 6f, o(27f, 59f), o(62f, 67f),
        armNear = listOf(o(28f, 60f), o(26f, 74f), o(40f, 79f)),   // forearm down on floor
        armFar = listOf(o(29f, 61f), o(27f, 75f), o(40f, 79f)),
        legNear = listOf(o(62f, 67f), o(76f, 72f), o(90f, 79f)),
        legFar = listOf(o(62f, 68f), o(76f, 73f), o(90f, 80f)),
        props = listOf(floor(82f)),
    )
    Pose.PIKE_PUSHUP -> Fig(
        o(30f, 40f), 6f, o(37f, 44f), o(58f, 34f),                 // hips high (pike apex)
        armNear = listOf(o(38f, 45f), o(33f, 60f), o(34f, 78f)),   // arms bent to floor
        armFar = listOf(o(39f, 46f), o(34f, 61f), o(35f, 78f)),
        legNear = listOf(o(58f, 34f), o(72f, 56f), o(82f, 78f)),   // legs down to floor
        legFar = listOf(o(58f, 35f), o(72f, 57f), o(82f, 79f)),
        props = listOf(floor(81f)),
    )
    Pose.HANDSTAND -> Fig(
        o(50f, 84f), 6f, o(50f, 76f), o(50f, 40f),                 // inverted spine
        armNear = listOf(o(48f, 74f), o(47f, 82f), o(46f, 90f)),   // arms down to floor
        armFar = listOf(o(52f, 74f), o(53f, 82f), o(54f, 90f)),
        legNear = listOf(o(50f, 40f), o(48f, 24f), o(47f, 10f)),   // legs up straight
        legFar = listOf(o(50f, 40f), o(52f, 24f), o(53f, 10f)),
        props = listOf(floor(91f)),
    )
    Pose.DIP -> Fig(
        o(48f, 22f), 6f, o(48f, 30f), o(50f, 56f),                 // upright torso
        armNear = listOf(o(45f, 33f), o(41f, 44f), o(44f, 40f)),   // bent arm on bar
        armFar = listOf(o(51f, 33f), o(55f, 44f), o(52f, 40f)),
        legNear = listOf(o(50f, 56f), o(58f, 66f), o(54f, 82f)),   // knees bent under
        legFar = listOf(o(50f, 57f), o(59f, 67f), o(55f, 83f)),
        props = listOf(listOf(o(28f, 44f), o(44f, 44f)), listOf(o(52f, 40f), o(70f, 40f))),
    )
    Pose.PULLUP, Pose.MUSCLE_UP -> Fig(
        o(50f, 30f), 6f, o(50f, 37f), o(50f, 64f),                 // hang, chin near bar
        armNear = listOf(o(47f, 39f), o(43f, 26f), o(42f, 16f)),   // bent arm up to bar
        armFar = listOf(o(53f, 39f), o(57f, 26f), o(58f, 16f)),
        legNear = listOf(o(49f, 64f), o(47f, 78f), o(46f, 90f)),
        legFar = listOf(o(51f, 64f), o(53f, 78f), o(54f, 90f)),
        props = listOf(bar(14f)),
    )
    Pose.ROW -> Fig(
        o(20f, 42f), 6f, o(28f, 45f), o(72f, 60f),                 // horizontal body, feet down
        armNear = listOf(o(30f, 46f), o(36f, 34f), o(42f, 22f)),   // pulling up to bar
        armFar = listOf(o(31f, 47f), o(37f, 35f), o(43f, 23f)),
        legNear = listOf(o(72f, 60f), o(82f, 68f), o(90f, 78f)),
        legFar = listOf(o(72f, 61f), o(82f, 69f), o(90f, 79f)),
        props = listOf(bar(18f), floor(81f)),
    )
    Pose.FRONT_LEVER -> Fig(
        o(22f, 46f), 6f, o(30f, 48f), o(78f, 52f),                 // body horizontal under bar
        armNear = listOf(o(32f, 47f), o(37f, 32f), o(41f, 18f)),   // straight arms to bar
        armFar = listOf(o(33f, 48f), o(38f, 33f), o(42f, 19f)),
        legNear = listOf(o(78f, 52f), o(85f, 53f), o(92f, 54f)),   // straight legs out
        legFar = listOf(o(78f, 53f), o(85f, 54f), o(92f, 55f)),
        props = listOf(bar(16f)),
    )
    Pose.PLANCHE -> Fig(
        o(20f, 50f), 6f, o(28f, 52f), o(74f, 46f),                 // body horizontal, feet up
        armNear = listOf(o(30f, 54f), o(32f, 66f), o(34f, 80f)),   // straight arms pressing down
        armFar = listOf(o(31f, 55f), o(33f, 67f), o(35f, 80f)),
        legNear = listOf(o(74f, 46f), o(84f, 44f), o(93f, 42f)),   // legs straight back/up
        legFar = listOf(o(74f, 47f), o(84f, 45f), o(93f, 43f)),
        props = listOf(floor(83f)),
    )
    Pose.LSIT -> Fig(
        o(44f, 26f), 6f, o(44f, 34f), o(46f, 58f),                 // upright torso
        armNear = listOf(o(41f, 40f), o(39f, 50f), o(38f, 60f)),   // straight arms pressing
        armFar = listOf(o(47f, 40f), o(49f, 50f), o(50f, 60f)),
        legNear = listOf(o(46f, 58f), o(66f, 57f), o(84f, 56f)),   // legs out front (L)
        legFar = listOf(o(46f, 59f), o(66f, 58f), o(84f, 57f)),
        props = listOf(listOf(o(30f, 62f), o(40f, 62f)), listOf(o(52f, 62f), o(62f, 62f))),
    )
    Pose.LEG_RAISE -> Fig(
        o(50f, 24f), 6f, o(50f, 31f), o(50f, 54f),                 // hanging
        armNear = listOf(o(47f, 33f), o(45f, 22f), o(44f, 12f)),   // arms up to bar
        armFar = listOf(o(53f, 33f), o(55f, 22f), o(56f, 12f)),
        legNear = listOf(o(50f, 54f), o(64f, 52f), o(78f, 50f)),   // legs raised to L
        legFar = listOf(o(50f, 55f), o(64f, 53f), o(78f, 51f)),
        props = listOf(bar(10f)),
    )
    Pose.HOLLOW -> Fig(
        o(20f, 60f), 6f, o(28f, 58f), o(58f, 56f),                 // on back, banana
        armNear = listOf(o(29f, 57f), o(22f, 50f), o(16f, 44f)),   // arms overhead
        armFar = listOf(o(29f, 58f), o(22f, 51f), o(16f, 45f)),
        legNear = listOf(o(58f, 56f), o(74f, 50f), o(88f, 44f)),   // legs up off floor
        legFar = listOf(o(58f, 57f), o(74f, 51f), o(88f, 45f)),
        props = listOf(floor(72f)),
    )
    Pose.GLUTE_BRIDGE -> Fig(
        o(18f, 64f), 6f, o(26f, 62f), o(52f, 52f),                 // shoulders down, hips up
        armNear = listOf(o(27f, 63f), o(24f, 72f), o(22f, 80f)),
        armFar = listOf(o(27f, 64f), o(24f, 73f), o(22f, 81f)),
        legNear = listOf(o(52f, 52f), o(66f, 62f), o(66f, 80f)),   // bent knees, feet down
        legFar = listOf(o(52f, 53f), o(67f, 63f), o(67f, 81f)),
        props = listOf(floor(82f)),
    )
    Pose.SQUAT -> Fig(
        o(46f, 20f), 8f, o(48f, 30f), o(54f, 54f),                 // torso lean, hips back+down
        armNear = listOf(o(46f, 34f), o(38f, 40f), o(30f, 42f)),   // arms reaching forward
        armFar = listOf(o(50f, 34f), o(42f, 41f), o(34f, 43f)),
        legNear = listOf(o(54f, 54f), o(66f, 58f), o(60f, 88f)),   // deep knee bend
        legFar = listOf(o(54f, 55f), o(68f, 59f), o(62f, 88f)),
        props = listOf(floor(89f)),
    )
    Pose.PISTOL -> Fig(
        o(44f, 20f), 8f, o(46f, 30f), o(50f, 54f),                 // one-leg deep squat
        armNear = listOf(o(46f, 34f), o(58f, 38f), o(70f, 40f)),   // arms forward for balance
        armFar = listOf(o(48f, 34f), o(60f, 39f), o(72f, 41f)),
        legNear = listOf(o(50f, 54f), o(60f, 62f), o(54f, 86f)),   // squatting leg
        legFar = listOf(o(50f, 55f), o(70f, 48f), o(88f, 44f)),    // free leg straight out front
        props = listOf(floor(87f)),
    )
    Pose.LUNGE -> Fig(
        o(46f, 18f), 8f, o(46f, 28f), o(48f, 52f),                 // upright split stance
        armNear = listOf(o(46f, 32f), o(44f, 42f), o(42f, 50f)),
        armFar = listOf(o(50f, 32f), o(52f, 42f), o(54f, 50f)),
        legNear = listOf(o(48f, 52f), o(34f, 66f), o(34f, 86f)),   // front knee bent 90
        legFar = listOf(o(48f, 53f), o(66f, 70f), o(80f, 86f)),    // back leg extended
        props = listOf(floor(87f)),
    )
    Pose.CALF_RAISE -> Fig(
        o(50f, 14f), 8f, o(50f, 24f), o(50f, 56f),                 // tall, on toes
        armNear = listOf(o(48f, 28f), o(43f, 42f), o(41f, 54f)),
        armFar = listOf(o(52f, 28f), o(57f, 42f), o(59f, 54f)),
        legNear = listOf(o(49f, 56f), o(48f, 72f), o(47f, 84f)),   // straight, heels up
        legFar = listOf(o(51f, 56f), o(52f, 72f), o(53f, 84f)),
        props = listOf(floor(86f)),
    )
    // ── stretches ──
    Pose.HAMSTRING_STRETCH -> Fig(
        o(26f, 30f), 6f, o(30f, 38f), o(42f, 58f),                 // seated fold over legs
        armNear = listOf(o(31f, 40f), o(48f, 50f), o(66f, 58f)),   // reaching to toes
        armFar = listOf(o(32f, 41f), o(49f, 51f), o(66f, 59f)),
        legNear = listOf(o(42f, 58f), o(64f, 59f), o(86f, 60f)),   // straight legs
        legFar = listOf(o(42f, 59f), o(64f, 60f), o(86f, 61f)),
        props = listOf(floor(63f)),
    )
    Pose.HIP_FLEXOR_STRETCH -> Fig(
        o(42f, 22f), 7f, o(42f, 32f), o(44f, 54f),                 // tall low-lunge
        armNear = listOf(o(42f, 36f), o(40f, 26f), o(40f, 16f)),   // arm reaching up
        armFar = listOf(o(46f, 36f), o(48f, 44f), o(50f, 52f)),
        legNear = listOf(o(44f, 54f), o(30f, 68f), o(30f, 86f)),   // front knee bent
        legFar = listOf(o(44f, 55f), o(66f, 74f), o(84f, 84f)),    // back leg extended low
        props = listOf(floor(87f)),
    )
    Pose.QUAD_STRETCH -> Fig(
        o(50f, 15f), 8f, o(50f, 25f), o(50f, 54f),                 // standing tall
        armNear = listOf(o(49f, 30f), o(54f, 44f), o(60f, 56f)),   // hand grabbing foot
        armFar = listOf(o(51f, 30f), o(48f, 38f), o(46f, 30f)),    // other arm out for balance
        legNear = listOf(o(50f, 54f), o(64f, 64f), o(52f, 50f)),   // heel pulled to glute
        legFar = listOf(o(50f, 55f), o(49f, 72f), o(48f, 88f)),    // stance leg
        props = listOf(floor(89f)),
    )
    Pose.PIGEON -> Fig(
        o(22f, 38f), 6f, o(30f, 42f), o(50f, 58f),                 // torso leaning forward
        armNear = listOf(o(31f, 44f), o(26f, 54f), o(22f, 64f)),   // forearms down
        armFar = listOf(o(32f, 45f), o(27f, 55f), o(23f, 65f)),
        legNear = listOf(o(50f, 58f), o(64f, 62f), o(74f, 60f)),   // front shin folded across
        legFar = listOf(o(50f, 59f), o(70f, 72f), o(88f, 82f)),    // back leg extended
        props = listOf(floor(84f)),
    )
    Pose.COBRA -> Fig(
        o(24f, 44f), 6f, o(30f, 50f), o(56f, 66f),                 // chest lifted, hips down
        armNear = listOf(o(31f, 52f), o(30f, 62f), o(30f, 74f)),   // arms pressing up
        armFar = listOf(o(32f, 53f), o(31f, 63f), o(31f, 75f)),
        legNear = listOf(o(56f, 66f), o(72f, 72f), o(88f, 76f)),   // legs flat
        legFar = listOf(o(56f, 67f), o(72f, 73f), o(88f, 77f)),
        props = listOf(floor(78f)),
    )
    Pose.CHILD_POSE -> Fig(
        o(76f, 62f), 6f, o(68f, 66f), o(46f, 76f),                 // folded forward, hips back
        armNear = listOf(o(66f, 66f), o(46f, 70f), o(24f, 74f)),   // arms reaching forward
        armFar = listOf(o(67f, 67f), o(47f, 71f), o(25f, 75f)),
        legNear = listOf(o(46f, 76f), o(60f, 80f), o(72f, 82f)),   // shins folded under
        legFar = listOf(o(46f, 77f), o(60f, 81f), o(72f, 83f)),
        props = listOf(floor(84f)),
    )
    Pose.SHOULDER_STRETCH -> Fig(
        o(50f, 15f), 8f, o(50f, 25f), o(50f, 56f),                 // standing
        armNear = listOf(o(49f, 30f), o(62f, 33f), o(76f, 34f)),   // arm across the body
        armFar = listOf(o(51f, 30f), o(60f, 40f), o(72f, 35f)),    // other arm pulls it in
        legNear = listOf(o(49f, 56f), o(47f, 72f), o(46f, 88f)),
        legFar = listOf(o(51f, 56f), o(53f, 72f), o(54f, 88f)),
        props = listOf(floor(89f)),
    )
    Pose.CHEST_STRETCH -> Fig(
        o(50f, 15f), 8f, o(50f, 25f), o(50f, 56f),                 // standing tall
        armNear = listOf(o(48f, 30f), o(34f, 30f), o(22f, 34f)),   // arms open wide back
        armFar = listOf(o(52f, 30f), o(66f, 30f), o(78f, 34f)),
        legNear = listOf(o(49f, 56f), o(47f, 72f), o(46f, 88f)),
        legFar = listOf(o(51f, 56f), o(53f, 72f), o(54f, 88f)),
        props = listOf(floor(89f)),
    )
}

/**
 * A solid, articulated line-figure of the movement. [color] is the body; the
 * far-side limbs and props are dimmer so the pose reads with real depth.
 */
@Composable
fun PoseFigure(
    pose: Pose,
    modifier: Modifier = Modifier,
    color: Color = com.ascend.lifeos.ui.theme.Ivory,
) {
    val f = fig(pose)
    Canvas(modifier) {
        val w = size.width; val h = size.height
        val s = minOf(w, h)
        val ox = (w - s) / 2f; val oy = (h - s) / 2f
        fun m(p: Offset) = Offset(ox + p.x / 100f * s, oy + p.y / 100f * s)

        val u = s / 100f
        val limb = Stroke(width = 6.2f * u, cap = StrokeCap.Round, join = StrokeJoin.Round)
        val limbFar = Stroke(width = 5.2f * u, cap = StrokeCap.Round, join = StrokeJoin.Round)
        val torso = Stroke(width = 9.5f * u, cap = StrokeCap.Round, join = StrokeJoin.Round)
        val propStroke = Stroke(width = 3.2f * u, cap = StrokeCap.Round)
        val far = color.copy(alpha = 0.38f)

        // props (floor / bar / parallettes), dim
        f.props.forEach { poly -> polyline(poly.map(::m), color.copy(alpha = 0.20f), propStroke) }

        // far-side limbs first (behind the body)
        f.armFar?.let { polyline(it.map(::m), far, limbFar) }
        f.legFar?.let { polyline(it.map(::m), far, limbFar) }

        // torso (thick), then near limbs, then head on top
        polyline(listOf(m(f.neck), m(f.hip)), color, torso)
        polyline(f.legNear.map(::m), color, limb)
        polyline(f.armNear.map(::m), color, limb)
        drawCircle(color, radius = f.headR * u, center = m(f.head))
    }
}

private fun DrawScope.polyline(pts: List<Offset>, color: Color, stroke: Stroke) {
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
        ("pull" in k && "up" in k) || "chin" in k -> Pose.PULLUP
        "front" in k && "lever" in k -> Pose.FRONT_LEVER
        "planche" in k -> Pose.PLANCHE
        "handstand" in k && ("push" in k || "hspu" in k) -> Pose.PIKE_PUSHUP
        "handstand" in k || "press to" in k -> Pose.HANDSTAND
        "pike" in k && "push" in k -> Pose.PIKE_PUSHUP
        "dip" in k -> Pose.DIP
        "row" in k -> Pose.ROW
        "l-sit" in k || "lsit" in k || "l sit" in k || "manna" in k || "v-sit" in k -> Pose.LSIT
        "leg raise" in k || "knee raise" in k || "toes" in k || "windshield" in k || "hanging" in k -> Pose.LEG_RAISE
        "hollow" in k || "dragon" in k || "ab wheel" in k -> Pose.HOLLOW
        "plank" in k || "flag" in k -> Pose.PLANK
        "jump" in k || "broad" in k || "pogo" in k || "bound" in k -> Pose.SQUAT
        "push" in k || "planche lean" in k || "pseudo" in k -> Pose.PUSHUP
        "pistol" in k || "shrimp" in k || "sissy" in k -> Pose.PISTOL
        "lunge" in k || "cossack" in k || "split squat" in k -> Pose.LUNGE
        "squat" in k -> Pose.SQUAT
        "bridge" in k || "hip thrust" in k || "glute" in k || "nordic" in k -> Pose.GLUTE_BRIDGE
        "calf" in k || "heel raise" in k -> Pose.CALF_RAISE
        // stretches / mobility
        "hamstring" in k || "forward fold" in k || "pancake" in k || "jefferson" in k || "compression" in k -> Pose.HAMSTRING_STRETCH
        "hip flexor" in k || "couch" in k || "half-split" in k || "front split" in k || "split" in k -> Pose.HIP_FLEXOR_STRETCH
        "quad" in k && "stretch" in k -> Pose.QUAD_STRETCH
        "pigeon" in k -> Pose.PIGEON
        "cobra" in k || "backbend" in k || "wheel" in k || "bridge / wheel" in k -> Pose.COBRA
        "child" in k -> Pose.CHILD_POSE
        "shoulder" in k && ("stretch" in k || "dislocate" in k || "openers" in k) -> Pose.SHOULDER_STRETCH
        "chest" in k && "stretch" in k -> Pose.CHEST_STRETCH
        "doorway" in k || "pec" in k -> Pose.CHEST_STRETCH
        "german hang" in k || "skin the cat" in k -> Pose.LEG_RAISE
        // mobility drills (route to the closest existing figure — no more generic standing)
        "cat" in k && "cow" in k -> Pose.COBRA
        "world" in k && "greatest" in k -> Pose.LUNGE
        "spiderman" in k -> Pose.LUNGE
        "90/90" in k || "90 90" in k || "figure-4" in k || "figure 4" in k -> Pose.PIGEON
        "frog" in k || "butterfly" in k || "adductor" in k || "copenhagen" in k -> Pose.PIGEON
        "open-book" in k || "open book" in k || "thoracic" in k || "spinal twist" in k || "twist" in k -> Pose.PIGEON
        "ankle" in k || "dorsiflex" in k || "knee-to-wall" in k || "knee to wall" in k || "tib-ant" in k -> Pose.CALF_RAISE
        "downward" in k || "down dog" in k || "forward fold" in k || "pike stretch" in k -> Pose.HAMSTRING_STRETCH
        "wall slide" in k || "pass-through" in k || "pass through" in k || "sleeper" in k || "distraction" in k || "cars" in k -> Pose.SHOULDER_STRETCH
        "leg swing" in k || "cossack" in k || "stride" in k || "a-skip" in k -> Pose.LUNGE
        "figure" in k || "supine" in k || "legs-up" in k || "legs up" in k -> Pose.HAMSTRING_STRETCH
        "wall drive" in k || "march" in k -> Pose.GLUTE_BRIDGE
        else -> Pose.STANDING
    }
}
