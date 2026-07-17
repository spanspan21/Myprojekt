package com.ascend.lifeos.data.training.engine

// ─── Yoga pose catalog ───────────────────────────────────────────────────────
// Static data only — the YogaEngine sequences from this. Hold times are
// realistic static-hold seconds per level (1 new · 2 regular · 3 advanced):
// standing 20-40s, deep hips 45-90s, savasana 120-180s. Sun-salutation
// components carry short flow-holds because they are moved through, not parked.

data class Pose(
    val id: String,
    val english: String,
    val sanskrit: String,
    val holdSecByLevel: IntArray,     // size 3, non-decreasing (level 1..3)
    val hasSides: Boolean,
    val cue: String,                  // one coaching line
    val tags: Set<String>,            // subset of YogaPoses.TAGS
    val counterPoseId: String? = null,
) {
    // IntArray member → override equals/hashCode so data-class semantics hold.
    override fun equals(other: Any?): Boolean = other is Pose && other.id == id
    override fun hashCode(): Int = id.hashCode()
}

object YogaPoses {

    val TAGS = setOf(
        "standing", "balance", "fold", "twist", "backbend", "hip",
        "shoulder", "core", "inversion", "seated", "restorative", "sun",
    )

    private fun p(
        id: String, english: String, sanskrit: String,
        l1: Int, l2: Int, l3: Int,
        sides: Boolean, cue: String,
        vararg tags: String,
        counter: String? = null,
    ) = Pose(id, english, sanskrit, intArrayOf(l1, l2, l3), sides, cue, tags.toSet(), counter)

    val ALL: List<Pose> = listOf(
        // ── centering / breath ──────────────────────────────────────────────
        p("centering", "Centering Breath", "Sukhasana", 60, 75, 90, false,
            "Sit tall, eyes soft — six slow breaths, longer exhale than inhale.", "seated", "restorative"),

        // ── sun salutation A components ─────────────────────────────────────
        p("mountain", "Mountain", "Tadasana", 15, 20, 25, false,
            "Stack shoulders over hips, crown tall, weight even across both feet.", "standing", "sun"),
        p("forward_fold", "Standing Forward Fold", "Uttanasana", 20, 30, 40, false,
            "Hinge from the hips and let the head hang heavy — bend the knees as needed.", "standing", "fold", "sun"),
        p("halfway_lift", "Halfway Lift", "Ardha Uttanasana", 8, 10, 12, false,
            "Flat back, fingertips to shins, gaze slightly forward.", "standing", "fold", "sun"),
        p("plank", "Plank", "Phalakasana", 20, 30, 45, false,
            "One straight line from heels to crown — push the floor away.", "core", "sun"),
        p("chaturanga", "Low Push-Up", "Chaturanga Dandasana", 5, 8, 12, false,
            "Elbows hug the ribs, shoulders no lower than the elbows.", "core", "sun"),
        p("upward_dog", "Upward-Facing Dog", "Urdhva Mukha Svanasana", 10, 15, 20, false,
            "Press the tops of the feet down, lift the thighs off the mat, open the chest.",
            "backbend", "sun", counter = "down_dog"),
        p("down_dog", "Downward-Facing Dog", "Adho Mukha Svanasana", 30, 45, 60, false,
            "Long spine first — bend the knees, send the hips high and back.", "sun", "inversion"),

        // ── standing ────────────────────────────────────────────────────────
        p("chair", "Chair", "Utkatasana", 20, 30, 45, false,
            "Sit back into the heels, knees behind the toes, arms by the ears.", "standing", "core"),
        p("warrior1", "Warrior I", "Virabhadrasana I", 25, 35, 45, true,
            "Back heel grounded at 45°, hips square to the front, arms reach up.", "standing"),
        p("warrior2", "Warrior II", "Virabhadrasana II", 25, 35, 45, true,
            "Front knee tracks over the ankle; gaze out over the front fingertips.", "standing"),
        p("warrior3", "Warrior III", "Virabhadrasana III", 15, 25, 35, true,
            "Hips level, body one line from crown to lifted heel.", "standing", "balance"),
        p("triangle", "Triangle", "Trikonasana", 25, 35, 45, true,
            "Both legs straight, ribs long — reach forward before you drop the hand.", "standing"),
        p("ext_side_angle", "Extended Side Angle", "Utthita Parsvakonasana", 25, 35, 45, true,
            "One line of energy from the back heel through the top fingertips.", "standing"),
        p("reverse_warrior", "Reverse Warrior", "Viparita Virabhadrasana", 15, 25, 30, true,
            "Keep the front knee bent as the back hand spills down the leg.", "standing", "backbend"),
        p("pyramid", "Pyramid", "Parsvottanasana", 25, 35, 45, true,
            "Square the hips over the front leg, fold with a long spine.", "standing", "fold"),
        p("low_lunge", "Low Lunge", "Anjaneyasana", 30, 45, 60, true,
            "Back knee down; sink the hips forward, tailbone heavy.", "standing", "hip"),
        p("high_lunge", "High Lunge", "Ashta Chandrasana", 25, 35, 45, true,
            "Back heel high, legs scissor toward each other for stability.", "standing", "hip"),
        p("runners_lunge", "Runner's Lunge", "Ashwa Sanchalanasana", 30, 40, 50, true,
            "Fingertips frame the front foot, back leg strong and long.", "standing", "hip"),
        p("goddess", "Goddess", "Utkata Konasana", 25, 35, 45, false,
            "Knees track over the toes, tailbone drops straight down.", "standing", "hip"),
        p("garland", "Garland Squat", "Malasana", 30, 45, 60, false,
            "Heels down if you can, elbows press the knees wide, chest tall.", "standing", "hip"),
        p("wide_leg_fold", "Wide-Legged Forward Fold", "Prasarita Padottanasana", 30, 45, 60, false,
            "Feet parallel, hinge from the hips, crown toward the floor.", "standing", "fold"),
        p("standing_split", "Standing Split", "Urdhva Prasarita Eka Padasana", 15, 25, 35, true,
            "Fold over the standing leg; the lifted leg reaches, hips stay level.",
            "standing", "balance", "fold", "inversion"),

        // ── balance ─────────────────────────────────────────────────────────
        p("tree", "Tree", "Vrksasana", 25, 40, 60, true,
            "Press foot and inner thigh into each other; grow tall through the crown.", "standing", "balance"),
        p("eagle", "Eagle", "Garudasana", 20, 30, 40, true,
            "Wrap tight, sit low, lift the elbows to broaden the upper back.", "standing", "balance", "shoulder"),
        p("dancer", "Dancer", "Natarajasana", 15, 25, 35, true,
            "Kick the foot into the hand — the kick creates the lift.", "standing", "balance", "backbend"),
        p("half_moon", "Half Moon", "Ardha Chandrasana", 15, 25, 35, true,
            "Stack the top hip, top arm reaches skyward — a block under the hand helps.", "standing", "balance"),
        p("side_plank", "Side Plank", "Vasisthasana", 20, 30, 40, true,
            "Stack shoulders and hips, press the floor away, top arm reaches up.", "core", "balance"),
        p("dolphin", "Dolphin", "Ardha Pincha Mayurasana", 20, 30, 45, false,
            "Forearms press down, hips lift high — a down dog on the forearms.", "inversion", "shoulder", "core"),
        p("crow", "Crow", "Bakasana", 5, 10, 20, false,
            "Knees high on the triceps, gaze forward, lean until the feet float.", "balance", "core"),

        // ── seated / folds / hips ───────────────────────────────────────────
        p("staff", "Staff Pose", "Dandasana", 20, 30, 40, false,
            "Sit tall, legs active, feet flexed — a plank standing upright.", "seated"),
        p("butterfly", "Butterfly", "Baddha Konasana", 45, 60, 90, false,
            "Soles together, hinge forward from the hips, knees fall heavy.", "seated", "hip"),
        p("seated_forward_fold", "Seated Forward Fold", "Paschimottanasana", 45, 60, 90, false,
            "Lead with the chest out over the legs; a soft bend in the knees is fine.", "seated", "fold"),
        p("head_to_knee", "Head-to-Knee Fold", "Janu Sirsasana", 40, 50, 60, true,
            "One leg long, fold over it from the hip crease, spine stays long.", "seated", "fold"),
        p("boat", "Boat", "Navasana", 20, 30, 45, false,
            "Chest proud, spine long — bend the knees before you round the back.", "seated", "core"),
        p("hero", "Hero", "Virasana", 45, 60, 90, false,
            "Sit between the heels (block under the hips if needed), spine tall.", "seated"),
        p("toe_squat", "Toe Squat", "Vajrasana (toes tucked)", 20, 30, 45, false,
            "Tuck all ten toes, sit back on the heels, breathe into the feet.", "seated"),
        p("fire_log", "Fire Log", "Agnistambhasana", 45, 60, 90, true,
            "Shins stacked like logs, both feet flexed, fold only as far as the hips allow.", "seated", "hip"),
        p("cow_face", "Cow Face", "Gomukhasana", 45, 60, 75, true,
            "Knees stacked; one arm over, one under — use a strap if the hands don't meet.",
            "seated", "hip", "shoulder"),
        p("pigeon", "Pigeon", "Eka Pada Rajakapotasana", 45, 60, 90, true,
            "Front shin angled, hips square, fold down and breathe into the hip.", "hip"),
        p("lizard", "Lizard", "Utthan Pristhasana", 45, 60, 90, true,
            "Foot outside the hand, forearms down if they reach, back leg long.", "hip"),
        p("half_splits", "Half Splits", "Ardha Hanumanasana", 40, 50, 60, true,
            "Front leg straight, hips stacked over the back knee, fold long.", "fold", "hip"),

        // ── twists ──────────────────────────────────────────────────────────
        p("seated_twist", "Seated Twist", "Ardha Matsyendrasana", 30, 40, 50, true,
            "Inhale to lengthen the spine, exhale to rotate — twist from the waist up.", "seated", "twist"),
        p("supine_twist", "Supine Twist", "Supta Matsyendrasana", 45, 60, 90, true,
            "Both shoulders stay heavy on the mat; let the knees fall, gaze opposite.", "twist", "restorative"),
        p("thread_needle", "Thread the Needle", "Parsva Balasana", 45, 60, 75, true,
            "Slide the arm under and rest on the shoulder — the twist comes from the mid-back.",
            "shoulder", "twist", "restorative"),

        // ── backbends ───────────────────────────────────────────────────────
        p("bridge", "Bridge", "Setu Bandha Sarvangasana", 25, 35, 45, false,
            "Press the feet down, lift the hips, knees stay hip-width.", "backbend", counter = "happy_baby"),
        p("wheel", "Wheel", "Urdhva Dhanurasana", 10, 15, 25, false,
            "Push evenly through hands and feet; straighten the arms before the legs.",
            "backbend", counter = "child_pose"),
        p("camel", "Camel", "Ustrasana", 15, 25, 35, false,
            "Hips stay over the knees; lift the chest up before you lean back.", "backbend", counter = "child_pose"),
        p("cobra", "Cobra", "Bhujangasana", 15, 20, 30, false,
            "Low ribs stay down, elbows soft — the back muscles do the lifting.", "backbend", counter = "child_pose"),
        p("locust", "Locust", "Salabhasana", 15, 20, 30, false,
            "Lift chest and legs together, reach the toes away, neck long.", "backbend", "core", counter = "child_pose"),
        p("bow", "Bow", "Dhanurasana", 15, 20, 25, false,
            "Kick the shins back into the hands to open the chest.", "backbend", counter = "child_pose"),
        p("sphinx", "Sphinx", "Salamba Bhujangasana", 45, 60, 90, false,
            "Elbows under the shoulders, forearms press, heart drifts forward.",
            "backbend", "restorative", counter = "child_pose"),

        // ── restorative / supine ────────────────────────────────────────────
        p("child_pose", "Child's Pose", "Balasana", 45, 60, 90, false,
            "Knees wide, hips to heels, forehead down — breathe into the back body.", "restorative", "fold"),
        p("cat_cow", "Cat-Cow", "Marjaryasana–Bitilasana", 45, 60, 75, false,
            "Move with the breath — inhale arch, exhale round, one motion per breath.", "restorative"),
        p("puppy", "Puppy Pose", "Uttana Shishosana", 45, 60, 75, false,
            "Hips over the knees, chest melts to the mat, arms long.", "shoulder", "restorative"),
        p("happy_baby", "Happy Baby", "Ananda Balasana", 45, 60, 90, false,
            "Grab the outer feet, pull the knees toward the armpits, sacrum stays down.", "hip", "restorative"),
        p("reclined_pigeon", "Reclined Pigeon", "Supta Kapotasana", 45, 60, 90, true,
            "Figure-four the legs, pull the thigh in, keep the crossed foot flexed.", "hip", "restorative"),
        p("supine_big_toe", "Reclined Hand-to-Big-Toe", "Supta Padangusthasana", 40, 50, 60, true,
            "Bottom leg heavy, top leg long — use a strap and keep the hips level.", "hip", "restorative"),
        p("legs_up_wall", "Legs up the Wall", "Viparita Karani", 120, 150, 180, false,
            "Sit bones close to the wall, arms wide, let gravity drain the legs.", "inversion", "restorative"),
        p("savasana", "Corpse Pose", "Savasana", 120, 150, 180, false,
            "Let the floor hold you — release control of the breath completely.", "restorative"),
    )

    val byId: Map<String, Pose> = ALL.associateBy { it.id }
}
