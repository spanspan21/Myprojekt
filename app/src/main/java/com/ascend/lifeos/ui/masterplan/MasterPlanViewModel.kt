package com.ascend.lifeos.ui.masterplan

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.ascend.lifeos.data.masterplan.DayPlan
import com.ascend.lifeos.data.masterplan.DomainWithGraph
import com.ascend.lifeos.data.masterplan.JarvisRoutingEngine
import com.ascend.lifeos.data.masterplan.MasterPlanDatabase
import com.ascend.lifeos.data.masterplan.MasterPlanImporter
import com.ascend.lifeos.data.masterplan.NodeWithChildren
import com.ascend.lifeos.data.masterplan.TaskStatus
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * Owns all Master-Plan state. On first run it seeds the DB from the bundled
 * JSON plans; thereafter it just streams the graph and runs the deterministic
 * [JarvisRoutingEngine] over whatever the user says they have today.
 */
class MasterPlanViewModel(app: Application) : AndroidViewModel(app) {

    // Obtain the DAO from the app-wide container rather than newing it up inline
    // (audit Phase 3: DI seam). Falls back to the direct call if ever accessed
    // before the container is built.
    private val dao = runCatching { com.ascend.lifeos.JarvisApp.container.masterPlanDao }
        .getOrElse { MasterPlanDatabase.get(app).dao() }
    private val importer = MasterPlanImporter(app, dao)
    private val engine = JarvisRoutingEngine()

    val domains: StateFlow<List<DomainWithGraph>> = dao.observeDomains()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val prefs = app.getSharedPreferences("masterplan_prefs", android.content.Context.MODE_PRIVATE)

    init {
        // Seed on first run, and re-seed whenever the bundled plans are bumped
        // (PLAN_VERSION). Version-gating means updated assets actually land on
        // the next launch instead of being skipped because the DB is non-empty.
        viewModelScope.launch {
            val seeded = prefs.getInt(KEY_PLAN_VERSION, 0)
            if (dao.domainCount() == 0 || seeded != PLAN_VERSION) {
                // only persist the version on success — a failed import used to
                // leave an empty vault locked in until the next version bump
                val result = importer.importBundledPlans()
                if (result.isSuccess) {
                    prefs.edit().putInt(KEY_PLAN_VERSION, PLAN_VERSION).apply()
                }
            }
        }
    }

    fun node(id: String): Flow<NodeWithChildren?> = dao.observeNode(id)

    fun setTaskDone(taskId: String, done: Boolean) {
        viewModelScope.launch {
            dao.setTaskStatus(taskId, if (done) TaskStatus.DONE else TaskStatus.TODO)
        }
    }

    /**
     * The Focus screen's cross-domain plan for today: real readiness + the chosen
     * time block → a handful of time-sliced tasks spread across domains.
     */
    fun planDay(readiness: Int?, availableMinutes: Int): DayPlan =
        engine.planDay(domains.value, readiness, availableMinutes)

    /** Pure pass-through to the engine so the UI never imports data-layer logic. */
    fun route(
        nodes: List<NodeWithChildren>,
        sleepScore: Int,
        hrvReadiness: Int,
        availableMinutes: Int,
    ): JarvisRoutingEngine.Directive =
        engine.route(
            nodes = nodes,
            bio = JarvisRoutingEngine.BioSignal(sleepScore, hrvReadiness),
            availableMinutes = availableMinutes,
            slack = 20,
        )

    private companion object {
        const val KEY_PLAN_VERSION = "plan_version"
        /** Bump whenever the bundled JSON plans change to force a re-import. */
        const val PLAN_VERSION = 3
    }
}
