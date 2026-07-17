package com.ascend.lifeos.data.training

import com.ascend.lifeos.data.Repo

// ─── Coaching voice — Iron vs Coach ──────────────────────────────────────────
// The generated copy was written for one 16-year-old who chose the drill ethos
// ("you get drilled, not coddled", "non-negotiable"). For a general Play-Store
// audience that tone is polarizing and, for some, harmful — autonomy-supportive
// messaging links to higher long-term adherence (Teixeira et al. 2012, IJBNPA).
// The PHILOSOPHY (fixed plan, no readiness bail-outs) is unchanged; only the
// voice switches. Existing installs keep IRON; fresh boots default to COACH.

object CoachTone {

    private fun iron(): Boolean =
        runCatching { Repo.data.profile.coachTone }.getOrDefault("iron") != "coach"

    /** Today's plan header under the next session. */
    fun assignmentLabel(): String =
        if (iron()) "Today's assignment · non-negotiable" else "Today's session · your plan for today"

    /** VolumeModel deload rationale. */
    fun deloadRationale(): String =
        if (iron()) "Deload week — volume pulled back so you rebound stronger, not because you're soft."
        else "Deload week — lighter on purpose so your body rebuilds and you come back stronger."

    /** Exam-week note suffix. */
    fun examNote(): String =
        if (iron()) "Exam week — volume trimmed 30% so school gets your focus. Still show up."
        else "Exam week — volume trimmed 30% so school gets your focus. A short session still counts."
}
