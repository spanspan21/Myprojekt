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
        CrashLog.install(this)
        container = AppContainer(this)
        // Init the store at the Application level so receivers/widgets that run
        // before any Activity find Repo ready (audit: init-ordering hazard).
        runCatching { Repo.initIfNeeded(this) }
        // Warm the Room finance store (migrates from prefs once, keeps prefs as backup).
        runCatching { com.ascend.lifeos.data.finance.FinanceRoom.init(this) }
        // Health bridge: hourly Health Connect pull, app open or not — the
        // in-app replacement for the retired Health Sync subscription. Plus one
        // immediate pull per process start so opening the app is always fresh.
        runCatching { com.ascend.lifeos.data.HealthBridge.schedule(this) }
        runCatching { com.ascend.lifeos.data.HealthBridge.syncNow(this) }
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
    }

    companion object {
        /** The one dependency container, built in onCreate. */
        lateinit var container: AppContainer
            private set
    }
}
