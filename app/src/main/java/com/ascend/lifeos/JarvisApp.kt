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
    }

    companion object {
        /** The one dependency container, built in onCreate. */
        lateinit var container: AppContainer
            private set
    }
}
