package com.ascend.lifeos

import android.app.Application
import com.ascend.lifeos.data.CrashLog

/**
 * JARVIS process entry — installs the crash black box before any component
 * (activity, receiver, widget, service) gets a chance to die silently.
 */
class JarvisApp : Application() {
    override fun onCreate() {
        super.onCreate()
        CrashLog.install(this)
    }
}
