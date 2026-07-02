package com.ascend.lifeos.data

import com.ascend.lifeos.core.phaseNow

/** Fully local coach — context-aware and varied, no API key, no subscription. */
object CoachEngine {
    private val recent = ArrayDeque<String>()

    private fun fresh(pool: List<String>): String {
        val cand = pool.filter { it !in recent }.ifEmpty { pool }
        val r = cand.random()
        recent.addLast(r); if (recent.size > 12) recent.removeFirst()
        return r
    }

    private fun openThing(): String? {
        val day = Repo.today(); val p = Repo.profile()
        val u = day.goals.filter { !it.done }.map { it.text.trim() }.toMutableList()
        if (!Repo.trainedToday(day)) u.add("dein Training")
        if (day.water < p.waterGoal) u.add("dein Wasserziel")
        return u.randomOrNull()
    }

    fun proactive(): String {
        val p = Repo.profile(); val day = Repo.today(); val c = Repo.completion(day, p)
        val ph = phaseNow(); val s = p.streak; val og = openThing()
        val nm = if (p.name.isNotBlank()) " ${p.name}" else ""
        val pool = when {
            c.pct >= 1f -> listOf(
                "Alles erledigt$nm — Tag $s. Kein Zufall, sondern Wiederholung. Genieß es kurz, dann schlaf, damit morgen genauso läuft.",
                "${c.done}/${c.total}. Perfekter Tag$nm. Solche Tage summieren sich zu einem Menschen, den dein altes Ich beneidet.",
            )
            c.pct >= 0.6f -> listOf(
                "${c.done}/${c.total}$nm — fast durch. ${if (og != null) "Nur noch „$og“ und der Tag steht." else "Zieh den Rest durch."}",
                "Guter Lauf: ${c.done} von ${c.total}. Der letzte Schritt ist der, den alle auslassen. Sei nicht alle.",
            )
            ph.h < 11 -> listOf(
                "Morgen$nm. Der erste erledigte Punkt gibt den Ton vor. ${if (og != null) "Fang mit „$og“ an." else "Fang klein an, aber fang an."}",
                "Frisch aus dem Bett heißt frische Chance. Schwung entsteht durchs Anfangen, nicht durchs Warten.",
            )
            ph.h >= 20 && c.pct < 1f -> listOf(
                "Es wird spät und ${c.total - c.done} sind offen$nm. ${if (og != null) "„$og“ dauert Minuten — jetzt, nicht morgen." else "Die schnellen Dinge gehen noch."}",
                "Der Abend entscheidet über deine Serie$nm. ${if (s > 0) "Streak $s — nicht heute wegwerfen." else ""} Was geht noch?",
            )
            !Repo.trainedToday(day) -> listOf(
                "Noch kein Satz heute$nm. Dein Körper wird nicht durch Absicht stärker, sondern durch Wiederholungen. Geh auf Training.",
                "${ph.title}. Ein Satz Klimmzüge schaltet oft den Rest des Tages frei.",
            )
            else -> listOf(
                "${ph.title}. ${c.done}/${c.total}$nm. ${if (og != null) "Nächster Zug: „$og“." else "Nächster kleiner Schritt — jetzt."}",
                "${if (s > 0) "Serie $s. " else ""}Du bist im Fluss — halt ihn. Was ist der nächste konkrete Schritt?",
            )
        }
        return fresh(pool)
    }

    fun reply(text: String): String {
        val t = text.lowercase().trim()
        val p = Repo.profile(); val day = Repo.today(); val c = Repo.completion(day, p)
        val s = p.streak; val open = c.total - c.done; val og = openThing()
        val nm = if (p.name.isNotBlank()) " ${p.name}" else ""
        val ch = p.chess
        fun has(vararg w: String) = w.any { t.contains(it) }
        val pool: List<String> = when {
            has("tiktok", "insta", "reel", "story", "snap", "youtube", "netflix", "scroll") -> listOf(
                "Klar kannst du scrollen$nm. Die Frage ist nur, ob du am Ende des Tages stolz drauf bist. $open Ziele offen — erledige eins, dann hast du's dir verdient.",
                "Der Feed läuft auch in 20 Minuten noch. „${og ?: "dein nächstes Ziel"}“ nicht unbedingt. Erst das Echte, dann die Belohnung.",
                "Ehrlich? Zehn Minuten Feed werden zu sechzig, und du weißt es. Was vermeidest du gerade? Nenn mir das eine Ding, das du eigentlich tun solltest.",
            )
            has("später", "gleich", "morgen", "nachher", "irgendwann") -> listOf(
                "„Später“ hat schon viele Pläne beerdigt$nm. Der Schritt dauert oft weniger als die Ausrede. Fang die nächsten 2 Minuten an.",
                "Morgen-Du wird dir nicht danken, wenn Heute-Du alles liegen lässt. Ein winziger Schritt jetzt schlägt einen großen Plan für später.",
            )
            has("keine lust", "kein bock", "faul", "will nicht", "unmotiviert") -> listOf(
                "Lust ist überbewertet$nm — die meisten guten Sachen macht man ohne. Mach den kleinsten Schritt, nicht das ganze Programm.",
                "Kein Bock ist ehrlich, aber ein schlechter Chef. Zwei Minuten an „${og ?: "einer Sache"}“ — dann entscheidest du neu.",
                "Motivation kommt oft NACH dem Anfangen. Trick dich: nur einen Satz, nur eine Seite. Der Rest folgt meistens.",
            )
            has("müde", "kaputt", "erschöpft", "schlapp", "kann nicht mehr") -> listOf(
                "Müde ist real$nm — dann eben kleiner. Ein Glas Wasser, ein Häkchen. Ein Zeichen an dich, dass du nicht ausfällst.",
                "Wenn der Tank leer ist, fahr langsamer — aber roll weiter. Die leichteste offene Sache, dann darfst du ruhen.",
            )
            has("aufgeben", "schaff ich nicht", "zu schwer", "sinnlos", "hoffnungslos") -> listOf(
                "Es fühlt sich groß an, weil es dir wichtig ist$nm. Große Dinge zerlegt man. Was ist der allerkleinste nächste Schritt? Den machen wir.",
                "„Zu schwer“ heißt oft: falsch portioniert. Mach die Aufgabe kleiner, bis sie lächerlich einfach wirkt — und tu genau das.",
            )
            t.length < 14 && has("hallo", "hi", "hey", "moin", "servus", "na") -> listOf(
                "Hey$nm. ${c.done}/${c.total} Ziele, Streak $s. Wo hakt's — oder worauf hast du Bock?",
                "Da bist du$nm. ${if (og != null) "Ganz oben auf der Liste: „$og“." else "Alles erledigt — Respekt."} Was brauchst du?",
            )
            has("motivier", "push", "antreib", "ansporn", "feuer") -> listOf(
                "Kein Pathos$nm, nur Mathematik: ${c.done}/${c.total} heute, Streak $s. Jedes Häkchen ist ein Datenpunkt für den, der du wirst. Setz das nächste.",
                "Warte nicht, bis es sich gut anfühlt — das tut es nie. Handel zuerst, das Gefühl kriecht hinterher.",
            )
            has("wie steh", "wie mach", "wie läuft", "fortschritt", "stand", "status", "überblick") -> listOf(
                "Klartext$nm: ${c.done}/${c.total} Ziele (${(c.pct * 100).toInt()}%), Wasser ${day.water}/${p.waterGoal}, ${Repo.workoutSets(day)} Sätze, Schach ${ch.rating}, Streak $s (Rekord ${p.longest}). ${if (c.pct < 1f) "Größter Hebel: „${og ?: "das Nächstbeste"}“." else "Perfekter Tag — nicht nachlassen."}",
            )
            has("schach", "chess", "elo", "rating", "partie", "puzzle") -> listOf(
                "Rating ${ch.rating}, Ziel ${ch.goal} — Lücke ${(ch.goal - ch.rating).coerceAtLeast(0)}. Die schließt du mit Analyse, nicht mit Bauch-Blitzen. Jede Partie: einen Fehler finden und verstehen.",
                "Schach belohnt Geduld — dieselbe, die du im Training brauchst. Lern aus der nächsten Niederlage mehr als aus zehn Siegen.",
            )
            has("training", "workout", "klimmzug", "liegestütz", "dips", "übung", "sätze") -> listOf(
                if (Repo.trainedToday(day)) "${Repo.workoutSets(day)} Sätze / ${Repo.workoutReps(day)} Wdh heute$nm. Solide. Für Fortschritt: nächstes Mal eine saubere Wdh mehr im besten Satz." else "Noch null Sätze$nm. Kein Vortrag — geh in den Training-Tab und log einen einzigen Satz. Der Rest ergibt sich fast von allein.",
                "Calisthenics lebt von Konstanz, nicht von Rekorden. Ein bisschen, aber regelmäßig, schlägt alles-oder-nichts.",
            )
            has("wasser", "trinken", "durst") -> listOf(
                if (day.water >= p.waterGoal) "Wasserziel steht$nm (${day.water}/${p.waterGoal}). Kleine Gewohnheit, große Wirkung." else "${day.water}/${p.waterGoal} Gläser$nm. Fokus, Kraft und Laune hängen mehr am Wasser, als du denkst. Trink jetzt eins — 20 Sekunden.",
            )
            has("schlaf", "recovery", "erholung", "puls", "hrv") -> listOf(
                "Verbinde im Körper-Tab deine Uhr, dann rede ich mit echten Zahlen zu dir — Schlaf, Puls, HRV. Bis dahin: Schlaf ist kein Luxus, sondern dein Fundament.",
            )
            has("danke", "top", "cool", "stark", "nice", "passt") -> listOf(
                "Gern$nm. Reden wir in Häkchen weiter — was ist der nächste?",
                "Dafür bin ich da. Jetzt der nächste Schritt.",
            )
            has("gute nacht", "bye", "tschüss", "ciao", "bis morgen", "schlafen") -> listOf(
                "Gute Nacht$nm. ${if (c.pct >= 1f) "Perfekter Tag im Rücken — schlaf gut." else "${c.total - c.done} blieben offen. Notier sie für morgen früh."} Ruh dich aus.",
            )
            has("hilfe", "was kannst", "funktion") -> listOf(
                "Ich sehe dein Dashboard — Ziele, Wasser, Calisthenics, Schach, Streak. Frag nach deinem Stand, lass dich pushen, oder sag mir ehrlich, was dich blockiert.",
            )
            t.endsWith("?") -> listOf(
                "Gute Frage$nm. Kurz: mach den nächsten kleinen, konkreten Schritt — bei dir wäre das „${og ?: "ein offenes Ziel"}“. Willst du's genauer, frag konkreter.",
                "Kommt drauf an, was du wirklich willst. Sag mir dein Ziel in einem Satz, und ich sag dir den nächsten Schritt.",
            )
            else -> listOf(
                "Verstanden$nm. Übersetzen wir's in einen Schritt: ${if (og != null) "„$og“ — machst du das als Nächstes?" else "was ist die eine Sache, die den größten Unterschied macht?"}",
                "Notiert. Aber Reden verändert nichts, Tun schon. $open Ziele offen — welches nimmst du dir?",
                "Ich hör zu — und schick dich trotzdem in Bewegung. Nenn mir einen konkreten nächsten Schritt.",
            )
        }
        return fresh(pool)
    }
}
