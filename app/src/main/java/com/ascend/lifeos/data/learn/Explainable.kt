package com.ascend.lifeos.data.learn

/**
 * U07 Explain-Pflicht (§7.2): no learning feature ships that cannot say in one
 * sentence what it learned and from how much evidence. Every learned state in
 * data/learn implements this; the strings point at raw events the user created
 * himself ("learned from your last 12 rests"), never at opaque scores.
 *
 * The richer ui/kit ExplainSheet (Explanation with facts / sparkline /
 * confidence) is built in the UI session on top of these strings — the
 * engines stay pure and UI-free.
 */
interface Explainable {
    fun explain(): String
}
