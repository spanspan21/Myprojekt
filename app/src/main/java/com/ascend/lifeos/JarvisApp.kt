package com.ascend.lifeos

import android.app.Application
import com.ascend.lifeos.data.CrashLog
import com.ascend.lifeos.data.Repo
import com.ascend.lifeos.di.AppContainer

/**
 * JARVIS process entry — installs the crash black box before any component
 * (activity, receiver, widget, service) gets a chance to die silently, and
 * builds the app-wide dependency container (audit Phase 3: introduce DI).
 */
class JarvisApp : Application() {
    override fun onCreate() {
        super.onCreate()
        val bootT0 = android.os.SystemClock.elapsedRealtime()
        fun mark(tag: String) = android.util.Log.d("BootProf", "$tag +${android.os.SystemClock.elapsedRealtime() - bootT0}ms")
        CrashLog.install(this)
        container = AppContainer(this)
        mark("container")
        // Object-level Compose states must be BORN in the global snapshot: if a
        // singleton holding a mutableStateOf is first touched from inside a
        // composition, its lazy object-init runs in that composition's read
        // snapshot and readers then throw "Reading a state that was created
        // after the snapshot was taken" — seen as a RELEASE-ONLY cold-start
        // crash in AscendApp (debug builds happened to touch these earlier).
        // Same bug class as Prefs.bool (56bb4e7). Touching them here, before
        // any composition exists, creates every state in the applied global
        // snapshot. This is the COMPLETE set of object-level Compose-state
        // singletons in the app (verified by grepping mutableStateOf/…StateOf on
        // objects) — every one is cheap to init (rev counters / null holders, no
        // eager IO), so warming them all forecloses the whole release-only
        // cold-start crash class regardless of which screen first reads one.
        // New object-level state singletons belong on this list.
        runCatching {
            // shell + first-frame (the ones the AscendApp/Home crash proved)
            com.ascend.lifeos.data.Modules.rev.intValue
            com.ascend.lifeos.data.DeepLink.pending.value
            com.ascend.lifeos.data.ActivityStore.rev
            com.ascend.lifeos.data.life.LifeStores.rev
            com.ascend.lifeos.data.finance.FinanceStore.rev
            com.ascend.lifeos.data.finance.FinanceRoom.rev
            com.ascend.lifeos.data.sleep.SleepStore.rev
            com.ascend.lifeos.ui.ShellMode.current.value
            com.ascend.lifeos.ui.ShellSignals.target.value
            com.ascend.lifeos.ui.home.HomeSignals.quickLog.value
            com.ascend.lifeos.ui.home.SettingsSignals.page.value
            com.ascend.lifeos.ui.boot.TourSignals.replay.value
            com.ascend.lifeos.ui.boot.TourTargets.bounds
            com.ascend.lifeos.ui.kit.AppFeedback.current
            com.ascend.lifeos.ui.theme.densityScale.floatValue
            // child-screen singletons (same class, lower frequency) — cheap init
            com.ascend.lifeos.data.Repo.jarvisReaction.value
            com.ascend.lifeos.data.calendar.TaskBlocks.rev
            com.ascend.lifeos.data.life.Achievements.rev
            com.ascend.lifeos.data.life.Decisions.rev
            com.ascend.lifeos.data.rules.CustomRules.rev
            com.ascend.lifeos.data.WeatherRepo.tempC
            com.ascend.lifeos.data.finance.BankLink.rev
            com.ascend.lifeos.data.finance.GoCardlessLink.rev
            com.ascend.lifeos.data.finance.Currency.current.value
            com.ascend.lifeos.wellbeing.GuardRuntime.payload.value
            // theme layer — read by EVERY composition host, including the two
            // MainActivity-less ones (InterceptActivity, guard overlay): the
            // colour tokens are live getters on these two states (K1).
            com.ascend.lifeos.ui.theme.themeSpec.value
            com.ascend.lifeos.ui.theme.accentState.value
            // file-level + store states that previously relied on incidental
            // MainActivity init order instead of this list (K1 follow-ups)
            com.ascend.lifeos.ui.hud.cookingRecipe.value
            com.ascend.lifeos.data.OwnRecipes.rev
        }
        mark("snapshotWarmup")
        // Init the store at the Application level so receivers/widgets that run
        // before any Activity find Repo ready (audit: init-ordering hazard).
        runCatching { Repo.initIfNeeded(this) }
        mark("repo")
        // Warm the Room finance store (migrates from prefs once, keeps prefs as backup).
        runCatching { com.ascend.lifeos.data.finance.FinanceRoom.init(this) }
        mark("financeRoom")
        // Health bridge: hourly Health Connect pull, app open or not — the
        // in-app replacement for the retired Health Sync subscription. Plus one
        // immediate pull per process start so opening the app is always fresh.
        runCatching { com.ascend.lifeos.data.HealthBridge.schedule(this) }
        runCatching { com.ascend.lifeos.data.HealthBridge.syncNow(this) }
        mark("healthBridge")
        // Guard survives app updates: the foreground service dies with the old
        // process and nothing restarted it until the toggle was cycled by hand.
        runCatching {
            if (com.ascend.lifeos.wellbeing.WellbeingStore.isEnabled(this)) {
                com.ascend.lifeos.wellbeing.JarvisGuardService.start(this)
            }
        }
        // Mirror all data to the private web dashboard on startup (one-way,
        // best-effort, off the main thread). No-op unless sync is configured.
        val app = this
        runCatching {
            if (com.ascend.lifeos.data.cloud.CloudSync.enabled()) {
                Thread {
                    val r = com.ascend.lifeos.data.cloud.CloudSync.pushNow(app)
                    android.util.Log.i(
                        "CloudSync",
                        "startup sync: " + r.fold({ "ok ($it docs)" }, { "FAIL: ${it.message}" })
                    )
                }.start()
            }
        }
        mark("onCreateDone")
    }

    companion object {
        /** The one dependency container, built in onCreate. */
        lateinit var container: AppContainer
            private set
    }
}
