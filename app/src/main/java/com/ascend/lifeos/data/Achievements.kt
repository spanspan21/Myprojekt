package com.ascend.lifeos.data

/** UI model for a single achievement / milestone. Computed live from stored data. */
data class Achievement(
    val id: String,
    val icon: String,
    val title: String,
    val desc: String,
    val unlocked: Boolean,
    val progress: Float, // 0..1
    val current: Int,
    val target: Int,
)

object AchievementEngine {

    fun all(): List<Achievement> {
        val d = Repo.data
        val p = d.profile
        val days = d.days.values
        val perfectDays = days.count { Repo.completion(it, p).pct >= 1f }
        val totalSets = days.sumOf { day -> day.cali.values.sumOf { it.size } }
        val totalReps = days.sumOf { day -> day.cali.values.sumOf { s -> s.sum() } }
        val waterDays = days.count { it.water >= p.waterGoal }
        val bestPull = p.caliBest["pullups"] ?: 0
        val peak = p.chess.peak
        val goal = p.chess.goal

        fun mk(id: String, icon: String, title: String, desc: String, cur: Int, target: Int): Achievement {
            val c = cur.coerceAtMost(target)
            return Achievement(id, icon, title, desc, cur >= target, (cur.toFloat() / target).coerceIn(0f, 1f), c, target)
        }

        return listOf(
            mk("first", "🌱", "Erster Schritt", "Einen Tag komplett abschließen", perfectDays, 1),
            mk("perfect10", "💎", "Perfektionist", "10 perfekte Tage", perfectDays, 10),
            mk("perfect30", "🏆", "Makellos", "30 perfekte Tage", perfectDays, 30),
            mk("week", "🔥", "Woche stark", "7 Tage Streak am Stück", p.longest, 7),
            mk("month", "⚡", "Eiserne Serie", "30 Tage Streak am Stück", p.longest, 30),
            mk("century", "👑", "Unaufhaltbar", "100 Tage Streak am Stück", p.longest, 100),
            mk("sets100", "🏋️", "Hundert Sätze", "100 Trainingssätze insgesamt", totalSets, 100),
            mk("reps1000", "💪", "Tausend Wdh", "1000 Wiederholungen insgesamt", totalReps, 1000),
            mk("pull10", "🎯", "Klimmzug-Held", "10 Klimmzüge in einem Satz", bestPull, 10),
            mk("hydro7", "💧", "Gut hydriert", "7 Tage das Wasserziel erreicht", waterDays, 7),
            mk("chessGoal", "♟️", "Schachziel", "Dein Rating-Ziel ($goal) erreichen", peak, goal),
        )
    }

    fun unlockedCount(list: List<Achievement> = all()): Int = list.count { it.unlocked }
}
