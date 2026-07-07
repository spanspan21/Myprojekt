package com.ascend.lifeos.data.masterplan

import android.content.Context
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * The JSON wire contract for the PC-authored master plans.
 *
 * Design rule: the plan file speaks in *slugs*, never database UUIDs. A node is
 * identified by its human-readable `key` ("http_basics"); prerequisites
 * reference those same keys. The importer ([toRows]) resolves keys → row ids so
 * the author never has to manage identity. That keeps hand-writing hundreds of
 * nodes tractable and diff-friendly in git.
 */
@Serializable
data class DomainPlanDto(
    val id: String,                       // stable domain id, e.g. "ai_automation"
    val title: String,
    val tagline: String = "",
    /** ARGB as hex string ("#5B9DFF") or plain int string; parsed leniently. */
    val accent: String = "#34E0A1",
    val icon: String = "target",
    val order: Int = 0,
    val nodes: List<NodePlanDto> = emptyList(),
)

@Serializable
data class NodePlanDto(
    val key: String,                      // unique within the domain
    val title: String,
    val subtitle: String = "",
    val energy: String = "MED",           // LOW | MED | HIGH
    @SerialName("minutes") val estimatedMinutes: Int = 30,
    val prerequisites: List<String> = emptyList(), // other node keys
    val x: Float? = null,                 // optional authored 0..1 position
    val y: Float? = null,
    val tasks: List<TaskPlanDto> = emptyList(),
    val resources: List<ResourcePlanDto> = emptyList(),
)

@Serializable
data class TaskPlanDto(
    val title: String,
    val detail: String = "",
)

@Serializable
data class ResourcePlanDto(
    val title: String,
    val url: String = "",
    val kind: String = "ARTICLE",
    val provider: String = "",
)

/** Row bundle ready for [MasterPlanDao.importDomain]. */
data class DomainRows(
    val domain: SkillDomainEntity,
    val nodes: List<NodeEntity>,
    val tasks: List<TaskEntity>,
    val resources: List<ResourceEntity>,
)

/**
 * Resolve the slug-based plan into id-stamped rows. Node ids are minted as
 * "<domainId>:<key>" — deterministic, so re-importing the same plan updates the
 * same rows (and thus preserves task-completion history when a task id is stable
 * too). Prerequisite keys are mapped to those same node ids; unknown keys are
 * dropped rather than dangling.
 */
fun DomainPlanDto.toRows(): DomainRows {
    fun nodeId(key: String) = "$id:$key"
    val knownKeys = nodes.map { it.key }.toSet()

    val domain = SkillDomainEntity(
        id = id,
        title = title,
        tagline = tagline,
        accentColor = parseArgb(accent),
        iconKey = icon,
        orderIndex = order,
    )

    val nodeRows = ArrayList<NodeEntity>(nodes.size)
    val taskRows = ArrayList<TaskEntity>()
    val resourceRows = ArrayList<ResourceEntity>()

    nodes.forEach { n ->
        val nid = nodeId(n.key)
        nodeRows += NodeEntity(
            id = nid,
            domainId = id,
            title = n.title,
            subtitle = n.subtitle,
            requiredEnergy = runCatching { EnergyLevel.valueOf(n.energy.uppercase()) }
                .getOrDefault(EnergyLevel.MED),
            estimatedMinutes = n.estimatedMinutes.coerceIn(1, 24 * 60),
            prerequisiteNodeIds = n.prerequisites.filter { it in knownKeys }.map { nodeId(it) },
            anchorX = n.x?.coerceIn(0f, 1f),
            anchorY = n.y?.coerceIn(0f, 1f),
        )
        n.tasks.forEachIndexed { i, t ->
            taskRows += TaskEntity(
                id = "$nid#t$i",
                nodeId = nid,
                title = t.title,
                detail = t.detail,
                orderIndex = i,
            )
        }
        n.resources.forEachIndexed { i, r ->
            resourceRows += ResourceEntity(
                id = "$nid#r$i",
                nodeId = nid,
                title = r.title,
                url = r.url,
                kind = runCatching { ResourceKind.valueOf(r.kind.uppercase()) }
                    .getOrDefault(ResourceKind.ARTICLE),
                provider = r.provider,
            )
        }
    }
    return DomainRows(domain, nodeRows, taskRows, resourceRows)
}

private fun parseArgb(raw: String): Long {
    val s = raw.trim().removePrefix("#").removePrefix("0x").removePrefix("0X")
    val rgb = s.toLongOrNull(16) ?: return 0xFF34E0A1
    // Add opaque alpha when the author gave only RRGGBB.
    return if (s.length <= 6) 0xFF000000 or rgb else rgb
}

/**
 * Reads and imports every `*.json` plan bundled under assets/masterplans/.
 * Call once on first run (guard with [MasterPlanDao.domainCount]); safe to
 * re-run — REPLACE keeps it idempotent.
 */
class MasterPlanImporter(
    private val context: Context,
    private val dao: MasterPlanDao,
) {
    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    suspend fun importBundledPlans(dir: String = "masterplans"): Result<Int> = runCatching {
        val assets = context.assets
        val files = assets.list(dir)?.filter { it.endsWith(".json") }.orEmpty()
        var count = 0
        var firstError: Throwable? = null
        for (file in files) {
            // per-file: one broken plan must not sink the remaining imports
            runCatching {
                val text = assets.open("$dir/$file").bufferedReader().use { it.readText() }
                val plan = json.decodeFromString<DomainPlanDto>(text)
                val rows = plan.toRows()
                dao.importDomain(rows.domain, rows.nodes, rows.tasks, rows.resources)
                count++
            }.onFailure { if (firstError == null) firstError = it }
        }
        firstError?.let { throw it }   // Result stays a failure → version not persisted
        count
    }

    /** Import a single plan supplied as a raw JSON string (e.g. from a share sheet). */
    suspend fun importJson(text: String): Result<String> = runCatching {
        val plan = json.decodeFromString<DomainPlanDto>(text)
        val rows = plan.toRows()
        require(rows.nodes.isNotEmpty()) { "Plan '${plan.id}' has no nodes" }
        dao.importDomain(rows.domain, rows.nodes, rows.tasks, rows.resources)
        rows.domain.id
    }
}
