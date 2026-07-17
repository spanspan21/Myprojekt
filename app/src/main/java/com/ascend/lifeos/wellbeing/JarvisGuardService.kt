package com.ascend.lifeos.wellbeing

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import android.provider.Settings
import android.view.KeyEvent
import android.view.View
import android.view.WindowManager
import android.widget.FrameLayout
import androidx.compose.runtime.Recomposer
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.AndroidUiDispatcher
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.compositionContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import com.ascend.lifeos.core.todayKey
import com.ascend.lifeos.data.masterplan.JarvisRoutingEngine
import com.ascend.lifeos.data.masterplan.MasterPlanDatabase
import com.ascend.lifeos.data.skill.SkillMeta
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class JarvisGuardService : Service() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var loop: Job? = null
    private var wm: WindowManager? = null
    private var overlay: View? = null                 // window FALLBACK only
    private var lastHeavyCheck = 0L                   // throttles the event-stream walk
    private var lastBudgetCheck = 0L                  // throttles the global day-budget walk
    private var lastGrayWrite: Boolean? = null        // last grayscale state we wrote
    private var power: PowerManager? = null
    private var overlayLifecycle: OverlayLifecycleOwner? = null
    private var overlayRecomposer: Recomposer? = null // cancelled per overlay — used to leak
    private var overlayRecomposeJob: Job? = null

    // Battery: the poll loop only runs while the screen is on. Screen-off kills
    // it completely; this receiver restarts it on the next unlock.
    private val screenReceiver = object : android.content.BroadcastReceiver() {
        override fun onReceive(ctx: Context?, intent: Intent?) {
            when (intent?.action) {
                Intent.ACTION_SCREEN_ON, Intent.ACTION_USER_PRESENT -> startLoop()
                Intent.ACTION_SCREEN_OFF -> {
                    GuardRuntime.lastPkg = null
                    loop?.cancel()
                    loop = null
                }
            }
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        instance = this
        wm = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        power = getSystemService(Context.POWER_SERVICE) as? PowerManager
        createChannel()
        runCatching {
            registerReceiver(
                screenReceiver,
                android.content.IntentFilter().apply {
                    addAction(Intent.ACTION_SCREEN_ON)
                    addAction(Intent.ACTION_SCREEN_OFF)
                    addAction(Intent.ACTION_USER_PRESENT)
                },
            )
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startForegroundCompat()
        when (intent?.action) {
            ACTION_STOP -> { removeOverlay(); stopSelf(); return START_NOT_STICKY }
            else -> startLoop()
        }
        return START_STICKY
    }

    /**
     * Instant path: the accessibility service saw a window change. Evaluates
     * the rules for [pkg] right now — the wall beats the app's cold start
     * instead of trailing it by a poll interval.
     */
    fun instantCheck(pkg: String) {
        scope.launch { runCatching { tick(forcedFg = pkg) } }
    }

    private fun startLoop() {
        if (loop?.isActive == true) return
        loop = scope.launch {
            while (isActive) {
                // Screen off → stop polling entirely; the screen receiver revives us.
                if (runCatching { power?.isInteractive == false }.getOrDefault(false)) {
                    GuardRuntime.lastPkg = null
                    break
                }
                runCatching { tick() }
                // With instant detection bound, opens are event-driven and the
                // loop only guards mid-session crossings — 5s always. Without
                // it, the fast cadence exists to catch gate-app opens quickly.
                val fast = !JarvisAccessibilityService.connected && runCatching {
                    WellbeingStore.gateApps(this@JarvisGuardService).isNotEmpty()
                }.getOrDefault(false)
                delay(if (fast) 2_000L else 5_000L)
            }
        }
    }

    // UsageStats queries walk the whole 06:00→now event stream — heavy enough to
    // jank/ANR the instant lock if run on Main. tick()/showIntercept() are already
    // suspend, so route every such read through IO (and swallow the rare query
    // exception, so a stats hiccup never blocks the lock decision).
    private suspend fun usageMinIO(pkg: String): Int =
        withContext(Dispatchers.IO) {
            (runCatching { DigitalWellbeingManager.usageTodayMs(this@JarvisGuardService, pkg) }.getOrDefault(0L) / 60_000L).toInt()
        }

    private suspend fun durationsIO(end: Long): Map<String, Long> =
        withContext(Dispatchers.IO) {
            runCatching { DigitalWellbeingManager.foregroundDurations(this@JarvisGuardService, DigitalWellbeingManager.startOfToday(), end) }.getOrDefault(emptyMap())
        }

    private suspend fun tick(forcedFg: String? = null) {
        if (!WellbeingStore.isEnabled(this)) return
        tickWindDown()
        tickA11yRebind()
        if (overlay != null || GuardRuntime.lockVisible || GuardRuntime.payload.value != null) return
        // Crash-safe casino settlement (CASINO_GUARD_PLAN §18): a result resolved
        // before an animation that never finished still lands. This MUST run only
        // while NO lock host is up — otherwise the poll loop (which keeps ticking
        // during play) would consume the worst-case pending a live Blackjack/Mines
        // round wrote at deal time, double-settling essentially every round.
        runCatching { com.ascend.lifeos.data.casino.CasinoStore.settlePendingIfAny(this) }
        // Strict-mode disable: complete the cooling-off shutdown when its timer
        // is up (the switch was flipped earlier; the wall held until now).
        if (WellbeingStore.settleDisableIfDue(this)) { stop(this); return }
        // Guard pause (D3): everything above is maintenance and keeps running;
        // from here on it's walls — and walls sleep while the pause stands.
        if (WellbeingStore.isPaused(this)) return
        // Module toggle: "off = gone" must hold for enforcement too — walls sleep
        // when the Guard module is switched off in Settings. Exception: an armed
        // Strict Mode is an explicit commitment device and outranks the module
        // toggle, otherwise one Settings tap would defeat the whole point.
        if (!com.ascend.lifeos.data.Modules.isOn(this, "guard") && !WellbeingStore.isStrict(this)) return
        if (!DigitalWellbeingManager.hasUsageAccess(this) || !DigitalWellbeingManager.canOverlay(this)) return
        if (runCatching { power?.isInteractive == false }.getOrDefault(false)) {
            GuardRuntime.lastPkg = null // screen off ends the session; next unlock counts as a new open
            return
        }

        // Just-in-time 80%-of-day-budget warning. The budget is GLOBAL, so it
        // runs before every per-app rule return below — burning the budget in
        // unguarded apps used to never trigger it. Own throttle: one walk/min.
        val nowForBudget = System.currentTimeMillis()
        if (nowForBudget - lastBudgetCheck >= 60_000L) {
            lastBudgetCheck = nowForBudget
            runCatching {
                val budgetDay = WellbeingStore.budgetMin(this)
                if (budgetDay > 0) {
                    val durations = durationsIO(nowForBudget)
                    val totalMin = (durations.values.sum() / 60_000L).toInt()
                    if (totalMin >= budgetDay * 0.8 && WellbeingStore.markBudgetWarned(this, todayKey())) {
                        com.ascend.lifeos.data.Notifier.show(this, "screen80")
                    }
                }
            }
        }

        val limits = WellbeingStore.limits(this)
        val gates = WellbeingStore.gateApps(this)
        val budgets = WellbeingStore.openBudgets(this)
        val appCats = WellbeingStore.appCategories(this)
        val catBudgets = WellbeingStore.categoryBudgets(this)
        val anyCategoryRule = appCats.isNotEmpty() && catBudgets.isNotEmpty()
        if (limits.isEmpty() && gates.isEmpty() && budgets.isEmpty() && !anyCategoryRule) return

        // Sticky foreground (M1 fix): MOVE_TO_FOREGROUND fires once on entry, so
        // during continuous use the 10s event window goes quiet. The last known
        // package IS still in front until a real switch produces a new event —
        // without this, one dismissed overlay meant free scrolling forever.
        val fg = forcedFg
            ?: DigitalWellbeingManager.foregroundApp(this)
            ?: GuardRuntime.lastPkg
            ?: return
        val now = System.currentTimeMillis()

        // Foreground transition → maybe a new session. A return within 90s
        // continues the old session (M3: app-hopping reset) and does NOT count
        // a fresh open (M4: screen off/on burned an open per unlock).
        if (fg != GuardRuntime.lastPkg) {
            GuardRuntime.lastPkg = fg
            lastHeavyCheck = 0L // fast path (R1): a rule check runs this tick, not in ~5s
            val gap = now - (GuardRuntime.lastSeenAt[fg] ?: 0L)
            if (gap > SESSION_CONTINUITY_MS) {
                GuardRuntime.sessionStart = now
                if (budgets.containsKey(fg)) runCatching { WellbeingStore.recordOpen(this, fg, todayKey()) }
            }
        }
        GuardRuntime.lastSeenAt[fg] = now
        WellbeingStore.recordTick(this)

        val limitMin = limits[fg]
        val budget = budgets[fg]
        val gated = fg in gates
        val category = appCats[fg]
        val catBudgetMin = category?.let { catBudgets[it] }
        if (limitMin == null && budget == null && !gated && catBudgetMin == null) return
        if (now < (GuardRuntime.cooldownUntil[fg] ?: 0L)) return

        // Casino loss lockout — the extra pause a lost stake bought. Checked
        // before every other rule: the house is paid first (plan §19).
        val casLock = com.ascend.lifeos.data.casino.CasinoStore.lockoutUntil(this, fg)
        if (now < casLock) {
            val usedNow = usageMinIO(fg)
            val hm = java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault()).format(java.util.Date(casLock))
            showIntercept(
                fg, InterceptMode.LIMIT, usedNow, limitMin ?: 0,
                statusText = "House lockout · until $hm",
                resetText = "the stake bought this pause",
            )
            return
        }

        // 0. Phone-free window — every guarded app is shut, gate passes included.
        if (limitMin != null || budget != null || gated) {
            val cal = java.util.Calendar.getInstance()
            val nowMin = cal.get(java.util.Calendar.HOUR_OF_DAY) * 60 + cal.get(java.util.Calendar.MINUTE)
            val window = runCatching { WellbeingStore.activePhoneFreeWindow(this, nowMin) }.getOrNull()
            if (window != null) {
                WellbeingStore.recordWindowViolation(this, todayKey())
                val usedNow = usageMinIO(fg)
                showIntercept(
                    fg, InterceptMode.FOCUS, usedNow, 0,
                    statusText = "Phone-free window · until %02d:%02d".format(window.second / 60, window.second % 60),
                    resetText = "unlocks at %02d:%02d".format(window.second / 60, window.second % 60),
                )
                return
            }
        }

        // 1. Pause gate — one breath before the app opens. Runs before any limit logic.
        if (gated && now >= (GuardRuntime.passUntil[fg] ?: 0L)) {
            showIntercept(fg, InterceptMode.GATE, 0, 0)
            return
        }
        if (limitMin == null && budget == null && catBudgetMin == null) return

        // The full event-stream walk below is the expensive part — keep it at the
        // normal ~5s cadence for the poll loop. Instant (event-driven) checks
        // reset the throttle above, so an app OPEN is always evaluated now.
        if (now - lastHeavyCheck < 4_500L) return
        lastHeavyCheck = now

        val dayDurations = durationsIO(now)
        val usedMs = dayDurations[fg] ?: 0L
        val usedMin = (usedMs / 60_000L).toInt()

        // 2. Focus session — every limited app is shut, no matter the budget.
        if (limitMin != null && WellbeingStore.inFocus(this)) {
            val hm = java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault())
                .format(java.util.Date(WellbeingStore.focusUntil(this)))
            showIntercept(
                fg, InterceptMode.FOCUS, usedMin, 0,
                resetText = "focus ends at $hm",
            )
            return
        }

        // 3. Morning block — limited apps stay dark before the cut-off.
        val morningUntil = WellbeingStore.morningBlockUntil(this)
        if (limitMin != null && morningUntil > 0) {
            val cal = java.util.Calendar.getInstance()
            val nowMin = cal.get(java.util.Calendar.HOUR_OF_DAY) * 60 + cal.get(java.util.Calendar.MINUTE)
            if (nowMin < morningUntil) {
                WellbeingStore.recordWindowViolation(this, todayKey())
                showIntercept(
                    fg, InterceptMode.LIMIT, usedMin, 0,
                    statusText = "Morning block · protected until %02d:%02d".format(morningUntil / 60, morningUntil % 60),
                    resetText = "unlocks at %02d:%02d".format(morningUntil / 60, morningUntil % 60),
                )
                return
            }
        }

        // 4. Per-open budget — too many opens today, or this session ran too long.
        if (budget != null) {
            val (opensPerDay, minutesPerOpen) = budget
            val opens = runCatching { WellbeingStore.opensToday(this, fg, todayKey()) }.getOrDefault(0)
            val overOpens = opens > opensPerDay
            val overSession = now - GuardRuntime.sessionStart >= minutesPerOpen * 60_000L
            if (overOpens || overSession) {
                showIntercept(
                    fg, InterceptMode.LIMIT, usedMin, 0,
                    statusText = if (overOpens) "Open budget reached · open $opens/$opensPerDay"
                    else "Session budget reached · open $opens/$opensPerDay",
                    resetText = "opens reset at 06:00",
                )
                return
            }
        }

        // 4.5 Approaching the wall — a quiet heads-up ~5 minutes out (once per
        //     app per day) so the hard stop never feels like an ambush.
        val casBonus = com.ascend.lifeos.data.casino.CasinoStore.bonusMin(this, fg)
        if (limitMin != null) {
            val leftMs = (limitMin + casBonus) * 60_000L - usedMs
            if (leftMs in 1..(5 * 60_000L)) {
                val soonKey = "limit_soon_$fg"
                val today = com.ascend.lifeos.core.todayKey()
                if (com.ascend.lifeos.data.Prefs.string(this, soonKey, "") != today) {
                    com.ascend.lifeos.data.Prefs.setString(this, soonKey, today)
                    runCatching {
                        com.ascend.lifeos.data.Notifier.showLimitSoon(
                            this, fg, ((leftMs + 59_999L) / 60_000L).toInt().coerceAtLeast(1),
                        )
                    }
                }
            }
        }

        // 5. Daily limit reached — casino bonus minutes raise the bar, and the
        //    plain LIMIT intercept is the only place the tables are offered.
        if (limitMin != null && usedMs >= (limitMin + casBonus) * 60_000L) {
            val effLimit = limitMin + casBonus
            // Ceil, not floor: a win must cover every second already burnt past
            // the wall, or "+5 min" silently pays out 4.
            val deficit = (((usedMs - effLimit * 60_000L) + 59_999L) / 60_000L).toInt().coerceAtLeast(0)
            // Skill-time today earns extra attempts (plan §6): the offer must
            // count them, or an earned spin would be hidden at the wall.
            val skillMinNow = usageMinIO(packageName)
            showIntercept(
                fg, InterceptMode.LIMIT, usedMin, effLimit,
                bonusWon = com.ascend.lifeos.data.casino.CasinoStore.wonBonusMin(this, fg),
                casinoPkg = if (com.ascend.lifeos.data.casino.CasinoStore.offerAvailable(this, fg, skillMinNow)) fg else null,
                deficitMin = deficit,
                resetText = "fresh minutes at 06:00",
            )
            return
        }

        // 6. Category budget — one shared pool across every app of the category.
        if (category != null && catBudgetMin != null) {
            val catUsedMs = appCats.entries
                .filter { it.value == category }
                .sumOf { dayDurations[it.key] ?: 0L }
            if (catUsedMs >= catBudgetMin * 60_000L) {
                showIntercept(
                    fg, InterceptMode.LIMIT, usedMin, 0,
                    statusText = "%s budget reached · %d/%dm".format(
                        category.replaceFirstChar { it.uppercase() },
                        (catUsedMs / 60_000L).toInt(),
                        catBudgetMin,
                    ),
                    resetText = "pool resets at 06:00",
                )
            }
        }
    }

    /**
     * Instant detection self-heal. Verified live on the S24: a force-stop or
     * app update unbinds the accessibility service and it never rebinds on its
     * own — One UI can even prune the component from the enabled list. Both
     * degrade walls to the 5s poll. With the WRITE_SECURE_SETTINGS grant the
     * service list can be rewritten remove→wait→add, which forces a rebind
     * (verified: a manual re-add binds within 2s).
     *
     * The dance runs under NonCancellable — a cancelled poll loop must never
     * strand the half-done state with the service removed. Retries every 90s
     * until `connected` flips; a no-op without the grant (poll keeps guarding).
     * The a11yOpted pref distinguishes "system pruned it" (heal) from "never
     * user-enabled" (hands off).
     */
    private var lastA11yKick = 0L
    private suspend fun tickA11yRebind() {
        val enabledInSettings = JarvisAccessibilityService.isEnabled(this)
        if (JarvisAccessibilityService.connected || enabledInSettings) {
            WellbeingStore.setA11yOpted(this, true)
            if (JarvisAccessibilityService.connected) return
        }
        if (!enabledInSettings && !WellbeingStore.a11yOpted(this)) return
        val now = System.currentTimeMillis()
        if (now - lastA11yKick < 90_000L) return
        val granted = runCatching {
            checkSelfPermission(android.Manifest.permission.WRITE_SECURE_SETTINGS) == PackageManager.PERMISSION_GRANTED
        }.getOrDefault(false)
        if (!granted) return
        lastA11yKick = now
        withContext(kotlinx.coroutines.NonCancellable) {
            runCatching {
                val cr = contentResolver
                val key = Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
                val flat = "$packageName/${JarvisAccessibilityService::class.java.name}"
                val short = "$packageName/.wellbeing.JarvisAccessibilityService"
                val others = (Settings.Secure.getString(cr, key) ?: "").split(':').filter {
                    it.isNotBlank() && !it.equals(flat, true) && !it.equals(short, true)
                }
                if (enabledInSettings) {
                    Settings.Secure.putString(cr, key, others.joinToString(":"))
                    delay(800)
                }
                Settings.Secure.putString(cr, key, (others + flat).joinToString(":"))
                Settings.Secure.putInt(cr, "accessibility_enabled", 1)
            }
        }
    }

    /**
     * Grayscale wind-down via the accessibility daltonizer. Needs the
     * WRITE_SECURE_SETTINGS permission (one-time adb grant) — without it this
     * silently no-ops. Never writes unless the feature is (or was) active, so a
     * user's own daltonizer config is left alone.
     */
    private fun tickWindDown() {
        val start = WellbeingStore.windDownStartMin(this)
        if (start <= 0 && lastGrayWrite != true) return
        val granted = runCatching {
            checkSelfPermission(android.Manifest.permission.WRITE_SECURE_SETTINGS) == PackageManager.PERMISSION_GRANTED
        }.getOrDefault(false)
        if (!granted) return
        val cal = java.util.Calendar.getInstance()
        val nowMin = cal.get(java.util.Calendar.HOUR_OF_DAY) * 60 + cal.get(java.util.Calendar.MINUTE)
        val shouldGray = start > 0 && (nowMin >= start || nowMin < 4 * 60)
        if (lastGrayWrite == shouldGray) return
        runCatching {
            if (shouldGray) {
                Settings.Secure.putInt(contentResolver, "accessibility_display_daltonizer_enabled", 1)
                Settings.Secure.putInt(contentResolver, "accessibility_display_daltonizer", 0)
            } else {
                Settings.Secure.putInt(contentResolver, "accessibility_display_daltonizer_enabled", 0)
                Settings.Secure.putInt(contentResolver, "accessibility_display_daltonizer", -1)
            }
            lastGrayWrite = shouldGray
        }
    }

    // ---- the wall ----------------------------------------------------------------

    /**
     * Builds the payload and raises the full-screen lock. Primary host is
     * InterceptActivity (real full-screen, pauses the app underneath); if the
     * launch is swallowed (OEM background-start quirk), the same composable
     * goes up as a window overlay ~1s later. One UI, two transports.
     */
    private suspend fun showIntercept(
        pkg: String,
        mode: InterceptMode,
        used: Int,
        effLimit: Int,
        statusText: String? = null,
        resetText: String = "",
        bonusWon: Int = 0,
        casinoPkg: String? = null,
        deficitMin: Int = 0,
    ) {
        if (overlay != null || GuardRuntime.lockVisible || GuardRuntime.payload.value != null) return

        WellbeingStore.recordIntercept(this)

        // The gate view shows neither guilt bars nor the alt plan — skip that work
        // so the breathing screen appears fast. It gets one small offer instead.
        val skillMin = if (mode == InterceptMode.GATE) 0
        else usageMinIO(packageName)

        val lockedOut = DoomscrollDetector.isLockedOut(this, pkg)
        val snoozes = DoomscrollDetector.snoozesToday(this, pkg)

        val altText = if (mode == InterceptMode.GATE) "" else runCatching {
            withContext(Dispatchers.IO) {
                val domains = MasterPlanDatabase.get(applicationContext).dao().domainsOnce()
                val plan = JarvisRoutingEngine().planDay(domains, readiness = null, availableMinutes = 15)
                plan.items.firstOrNull()
            }
        }.getOrNull()?.let { item ->
            "Do this instead: ${item.task?.title ?: item.node.node.title}\n${item.domainTitle} · ${item.minutes} min"
        } ?: "Open JARVIS and put 15 minutes into one of your goals."

        // Gate offer: one concrete 2-minute alternative. Priority: a due skill
        // review, then a physical micro-dose, then breath work.
        val offerText = if (mode != InterceptMode.GATE) "" else {
            val dueTitle = runCatching {
                withContext(Dispatchers.IO) {
                    val domains = MasterPlanDatabase.get(applicationContext).dao().domainsOnce()
                    val completed = domains.flatMap { it.completedNodeIds }.toSet()
                    val dueId = SkillMeta.dueReviews(applicationContext, System.currentTimeMillis(), completed).firstOrNull()
                    dueId?.let { id ->
                        domains.asSequence().flatMap { it.nodes.asSequence() }
                            .firstOrNull { it.node.id == id }?.node?.title
                    }
                }
            }.getOrNull()
            when {
                dueTitle != null -> "Review: $dueTitle"
                System.currentTimeMillis() / 60_000L % 2L == 0L -> "20 push-ups. Right now."
                else -> "2 minutes of box breathing."
            }
        }

        val p = GuardRuntime.InterceptPayload(
            mode = mode,
            pkg = pkg,
            appLabel = DigitalWellbeingManager.appLabel(this, pkg),
            usedMinutes = used,
            limitMinutes = effLimit,
            bonusWonMinutes = bonusWon,
            statusText = statusText,
            resetText = resetText,
            skillMinutes = skillMin,
            altText = altText,
            offerText = offerText,
            lockedOut = lockedOut,
            snoozeCount = snoozes,
            casinoPkg = casinoPkg,
            deficitMin = deficitMin,
            interceptNo = WellbeingStore.interceptsToday(this),
            guardStreak = WellbeingStore.guardStreak(this),
            reclaimToday = WellbeingStore.reclaimToday(this, todayKey()),
        )
        GuardRuntime.payload.value = p

        runCatching {
            startActivity(
                Intent(this, InterceptActivity::class.java)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_NO_ANIMATION),
            )
        }

        // Watchdog: if no host went visible, raise the window fallback.
        scope.launch {
            delay(900)
            if (GuardRuntime.payload.value === p && !GuardRuntime.lockVisible && overlay == null) {
                runCatching { showWindowFallback() }
            }
        }
    }

    // ---- window fallback (same composable, overlay transport) --------------------

    private fun showWindowFallback() {
        if (overlay != null) return

        val lifecycleOwner = OverlayLifecycleOwner()
        overlayLifecycle = lifecycleOwner

        val composeView = ComposeView(this).apply {
            setViewTreeLifecycleOwner(lifecycleOwner)
            setViewTreeSavedStateRegistryOwner(lifecycleOwner)

            val recomposer = Recomposer(AndroidUiDispatcher.CurrentThread)
            compositionContext = recomposer
            overlayRecomposer = recomposer
            overlayRecomposeJob = scope.launch(AndroidUiDispatcher.CurrentThread) { recomposer.runRecomposeAndApplyChanges() }

            setContent {
                val p by GuardRuntime.payload
                val cur = p ?: return@setContent
                val ctx = this@JarvisGuardService
                JarvisInterceptScreen(
                    p = cur,
                    onLater = { GuardRuntime.actLater(ctx, cur.pkg); removeOverlay() },
                    onSkill = { GuardRuntime.actSkill(ctx, cur.pkg); removeOverlay() },
                    onExit = { GuardRuntime.actExitHome(ctx, cur.pkg); removeOverlay() },
                    onOfferDone = { GuardRuntime.actOfferDone(ctx, cur.pkg); removeOverlay() },
                    onGateContinue = { GuardRuntime.actGatePass(ctx, cur.pkg); removeOverlay() },
                    onCasinoWin = { GuardRuntime.actCasinoWin(ctx, cur.pkg); removeOverlay() },
                    onCasinoLose = { GuardRuntime.actCasinoLose(ctx, cur.pkg); removeOverlay() },
                    onPanic = { GuardRuntime.actPanicFocus(ctx, cur.pkg); removeOverlay() },
                )
            }
        }

        // Hardware/gesture back leaves the app — never tunnels into it.
        val frame = object : FrameLayout(this) {
            override fun dispatchKeyEvent(event: KeyEvent): Boolean {
                if (event.keyCode == KeyEvent.KEYCODE_BACK) {
                    if (event.action == KeyEvent.ACTION_UP) {
                        GuardRuntime.payload.value?.let { GuardRuntime.actExitHome(context, it.pkg) }
                        removeOverlay()
                    }
                    return true
                }
                return super.dispatchKeyEvent(event)
            }
        }
        frame.addView(
            composeView,
            FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT),
        )

        lifecycleOwner.onCreate()
        lifecycleOwner.onResume()

        val lp = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            else @Suppress("DEPRECATION") WindowManager.LayoutParams.TYPE_PHONE,
            // Focusable on purpose: the wall owns back + touch while it stands.
            WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN
                or WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT,
        )
        runCatching { wm?.addView(frame, lp) }
            .onSuccess { overlay = frame; GuardRuntime.lockVisible = true }
    }

    private fun removeOverlay() {
        overlayLifecycle?.onDestroy()
        overlayLifecycle = null
        overlay?.let {
            runCatching { wm?.removeView(it) }
            GuardRuntime.lockVisible = false
        }
        overlay = null
        // the recompose coroutine is per-overlay; without this every intercept
        // leaked a Recomposer + endless coroutine into the long-lived service
        runCatching { overlayRecomposer?.cancel() }
        overlayRecomposer = null
        overlayRecomposeJob?.cancel()
        overlayRecomposeJob = null
    }

    // ---- foreground plumbing ----------------------------------------------------

    private fun createChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val nm = getSystemService(NotificationManager::class.java)
            nm.createNotificationChannel(
                NotificationChannel(CHANNEL, "Jarvis Guard", NotificationManager.IMPORTANCE_MIN),
            )
        }
    }

    private fun startForegroundCompat() {
        val notif: Notification = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Notification.Builder(this, CHANNEL)
                .setContentTitle("Jarvis Guard active")
                .setContentText("Protecting your focus")
                .setSmallIcon(android.R.drawable.ic_lock_idle_lock)
                .build()
        } else {
            @Suppress("DEPRECATION") Notification.Builder(this)
                .setContentTitle("Jarvis Guard active").setSmallIcon(android.R.drawable.ic_lock_idle_lock).build()
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startForeground(NOTIF_ID, notif, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
        } else {
            startForeground(NOTIF_ID, notif)
        }
    }

    override fun onDestroy() {
        removeOverlay()
        loop?.cancel()
        scope.cancel()
        runCatching { unregisterReceiver(screenReceiver) }
        if (instance === this) instance = null
        super.onDestroy()
    }

    companion object {
        private const val CHANNEL = "jarvis_guard"
        private const val NOTIF_ID = 4711
        private const val SESSION_CONTINUITY_MS = 90_000L // M3/M4: return <90s = same session
        const val ACTION_STOP = "com.ascend.lifeos.STOP_GUARD"

        /** Same-process hook for the accessibility fast path. */
        @Volatile var instance: JarvisGuardService? = null
            private set

        fun start(ctx: Context) {
            val i = Intent(ctx, JarvisGuardService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) ctx.startForegroundService(i) else ctx.startService(i)
        }

        fun stop(ctx: Context) {
            ctx.startService(Intent(ctx, JarvisGuardService::class.java).setAction(ACTION_STOP))
        }
    }
}

/**
 * Minimal LifecycleOwner + SavedStateRegistryOwner so Compose can run inside a
 * WindowManager overlay from a Service (which has no Activity lifecycle).
 */
private class OverlayLifecycleOwner : androidx.lifecycle.LifecycleOwner, SavedStateRegistryOwner {
    private val lifecycleRegistry = LifecycleRegistry(this)
    private val savedStateController = SavedStateRegistryController.create(this)

    override val lifecycle: Lifecycle get() = lifecycleRegistry
    override val savedStateRegistry: SavedStateRegistry get() = savedStateController.savedStateRegistry

    fun onCreate() {
        savedStateController.performRestore(null)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_CREATE)
    }
    fun onResume() {
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_START)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_RESUME)
    }
    fun onDestroy() {
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_PAUSE)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_STOP)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY)
    }
}
