package com.ascend.lifeos.data

/**
 * Suche v2 — das Ranking (FUEL-Masterplan Kap. 34).
 * Lokal, deterministisch, erklärbar: Match-Qualität × Nähe × eigene Historie.
 * Der Alias-Bug (P2: query.contains(alias) → „protein" fand das Ei) ist hier
 * strukturell unmöglich: Aliase matchen nur exakt oder als Prefix.
 */
object FoodRank {

    /** lowercase + Umlaut-Falten + trim — „Latté"/„LATTE"/„latte" identisch. */
    fun normalize(s: String): String = s.trim().lowercase()
        .replace("ä", "a").replace("ö", "o").replace("ü", "u").replace("ß", "ss")
        .replace("é", "e").replace("è", "e").replace("á", "a")

    /**
     * 100 exakter Name · 80 Name beginnt mit q · 60 ein WORT beginnt mit q ·
     * 45 Alias EXAKT · 40 Alias-Prefix · 30 Substring · 0 kein Treffer.
     * (Exakt > Prefix: „milch" gehört der Milch, nicht der Milchschokolade.)
     */
    fun matchQuality(name: String, aliases: List<String>, queryNorm: String): Int {
        if (queryNorm.isBlank()) return 0
        val n = normalize(name)
        if (n == queryNorm) return 100
        if (n.startsWith(queryNorm)) return 80
        if (n.split(' ', '-', '(', ',').any { it.startsWith(queryNorm) }) return 60
        var alias = 0
        for (a in aliases) {
            val an = normalize(a)
            if (an == queryNorm) return 45
            if (an.startsWith(queryNorm)) alias = maxOf(alias, 40)
        }
        if (alias > 0) return alias
        if (n.contains(queryNorm)) return 30
        return 0
    }

    /** Kürze-Bonus: kurze Namen zuerst (8 − 0,2·len, min 0). */
    fun brevity(name: String): Int = (8 - name.length / 5).coerceAtLeast(0)

    /**
     * Gesamt-Score eines Treffers. History-Sets kommen aus dem Aufrufer
     * (Recents-Namen, Favoriten-Namen, Log-Häufigkeit der letzten 30 Tage).
     */
    fun score(
        name: String,
        aliases: List<String>,
        queryNorm: String,
        tierBonus: Int,                    // +25 eigene · +15 Staple · 0 OFF
        recentNames: Set<String> = emptySet(),
        favoriteNames: Set<String> = emptySet(),
        logCounts: Map<String, Int> = emptyMap(),
    ): Int {
        val mq = matchQuality(name, aliases, queryNorm)
        if (mq == 0) return 0
        val nn = normalize(name)
        var s = mq + tierBonus + brevity(name)
        if (nn in recentNames) s += 50
        if (nn in favoriteNames) s += 30
        s += (2 * (logCounts[nn] ?: 0)).coerceAtMost(20)
        return s
    }

    /** Levenshtein ≤ 1 für den „Meintest du …?"-Vorschlag (Kap. 35). */
    fun oneEditAway(a: String, b: String): Boolean {
        if (a == b) return true
        val la = a.length; val lb = b.length
        if (kotlin.math.abs(la - lb) > 1) return false
        var i = 0; var j = 0; var edits = 0
        while (i < la && j < lb) {
            if (a[i] == b[j]) { i++; j++; continue }
            if (++edits > 1) return false
            when {
                la == lb -> { i++; j++ }
                la > lb -> i++
                else -> j++
            }
        }
        return edits + (la - i) + (lb - j) <= 1
    }
}
