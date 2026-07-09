package com.ascend.lifeos.di

import android.content.Context
import com.ascend.lifeos.data.calendar.CalendarDatabase
import com.ascend.lifeos.data.masterplan.MasterPlanDatabase
import com.ascend.lifeos.data.training.TrainingDatabase

/**
 * Manual dependency container (audit Phase 3: introduce DI). Built once in
 * JarvisApp.onCreate and exposed as the single seam through which consumers
 * obtain shared dependencies (the Room DAOs today), instead of each call site
 * newing up `X.get(ctx)` inline. A lightweight, framework-free step toward
 * inversion of control; call sites migrate to it incrementally.
 */
class AppContainer(context: Context) {
    private val app = context.applicationContext

    val trainingDao by lazy { TrainingDatabase.get(app).dao() }
    val calendarDao by lazy { CalendarDatabase.get(app).dao() }
    val masterPlanDao by lazy { MasterPlanDatabase.get(app).dao() }
}
