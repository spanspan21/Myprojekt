package com.ascend.lifeos.data.masterplan

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.Relation
import androidx.room.TypeConverter
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json

/**
 * The deterministic Master-Plan model — the core of the Jarvis OS.
 *
 * There is NO on-device model here. Giant hand/PC-authored plans (JSON) are
 * imported verbatim into these tables; the app is then a pure routing engine
 * over a fixed graph. The structure is a directed acyclic graph, not a line:
 *
 *     SkillDomainEntity          a life goal ("Cybersecurity Professional")
 *       └─ NodeEntity            a milestone, wired by prerequisiteNodeIds
 *            ├─ TaskEntity       a concrete, checkable action
 *            └─ ResourceEntity   a free source (YouTube / GitHub / docs)
 *
 * Foreign keys cascade: dropping a domain wipes its whole graph atomically.
 * Every child is indexed on its parent id so joins never table-scan.
 */

/** Cognitive/physical load a node demands. Ordinal order is meaningful: LOW < MED < HIGH. */
enum class EnergyLevel { LOW, MED, HIGH }

enum class TaskStatus { TODO, DONE }

enum class ResourceKind { VIDEO, ARTICLE, DOCS, GITHUB, COURSE, INTERACTIVE }

@Entity(tableName = "mp_domain")
data class SkillDomainEntity(
    @PrimaryKey val id: String,
    /** Display name, e.g. "AI Automation Mastery". */
    val title: String,
    /** One-line framing shown under the title. */
    val tagline: String,
    /** ARGB colour that themes this domain's constellation (e.g. 0xFF5B9DFF). */
    val accentColor: Long,
    /** Stable key the UI maps to an icon; kept as data, not a drawable ref. */
    val iconKey: String = "target",
    /** Sort order across domains on the hub. */
    val orderIndex: Int = 0,
)

@Entity(
    tableName = "mp_node",
    foreignKeys = [
        ForeignKey(
            entity = SkillDomainEntity::class,
            parentColumns = ["id"],
            childColumns = ["domainId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("domainId")],
)
data class NodeEntity(
    @PrimaryKey val id: String,
    val domainId: String,
    val title: String,
    /** Short "why this matters" line. */
    val subtitle: String,
    /** Load required to attempt this node — the routing engine gates on it. */
    val requiredEnergy: EnergyLevel,
    /** Honest time-to-complete estimate in minutes — the time gate. */
    val estimatedMinutes: Int,
    /**
     * Ids of nodes that must be DONE before this one unlocks. Empty ⇒ a root.
     * This is the actual graph topology (a node may have several parents).
     */
    val prerequisiteNodeIds: List<String>,
    /**
     * Optional authored constellation position in 0..1 space. When null, the
     * canvas derives a layout from graph depth. Lets a plan hand-place its stars.
     */
    val anchorX: Float? = null,
    val anchorY: Float? = null,
)

@Entity(
    tableName = "mp_task",
    foreignKeys = [
        ForeignKey(
            entity = NodeEntity::class,
            parentColumns = ["id"],
            childColumns = ["nodeId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("nodeId")],
)
data class TaskEntity(
    @PrimaryKey val id: String,
    val nodeId: String,
    val title: String,
    /** How the user knows it's done. */
    val detail: String,
    val orderIndex: Int,
    val status: TaskStatus = TaskStatus.TODO,
)

@Entity(
    tableName = "mp_resource",
    foreignKeys = [
        ForeignKey(
            entity = NodeEntity::class,
            parentColumns = ["id"],
            childColumns = ["nodeId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("nodeId")],
)
data class ResourceEntity(
    @PrimaryKey val id: String,
    val nodeId: String,
    val title: String,
    val url: String,
    val kind: ResourceKind,
    /** Human-readable source, e.g. "YouTube", "n8n Docs". */
    val provider: String,
)

// ---- Read models ------------------------------------------------------------

data class NodeWithChildren(
    @Embedded val node: NodeEntity,
    @Relation(parentColumn = "id", entityColumn = "nodeId")
    val tasks: List<TaskEntity>,
    @Relation(parentColumn = "id", entityColumn = "nodeId")
    val resources: List<ResourceEntity>,
) {
    val doneCount: Int get() = tasks.count { it.status == TaskStatus.DONE }
    val isComplete: Boolean get() = tasks.isNotEmpty() && doneCount == tasks.size
    val progress: Float
        get() = if (tasks.isEmpty()) 0f else doneCount / tasks.size.toFloat()
}

data class DomainWithGraph(
    @Embedded val domain: SkillDomainEntity,
    @Relation(
        entity = NodeEntity::class,
        parentColumn = "id",
        entityColumn = "domainId",
    )
    val nodes: List<NodeWithChildren>,
) {
    val completedNodeIds: Set<String> get() = nodes.filter { it.isComplete }.map { it.node.id }.toSet()
    val progress: Float
        get() {
            val all = nodes.sumOf { it.tasks.size }
            return if (all == 0) 0f else nodes.sumOf { it.doneCount } / all.toFloat()
        }
}

// ---- Type converters --------------------------------------------------------

/** Enum names + a JSON-encoded id list — robust to any id shape. */
class MasterPlanConverters {
    @TypeConverter fun energy(v: EnergyLevel): String = v.name
    @TypeConverter fun toEnergy(v: String): EnergyLevel =
        runCatching { EnergyLevel.valueOf(v) }.getOrDefault(EnergyLevel.MED)

    @TypeConverter fun taskStatus(v: TaskStatus): String = v.name
    @TypeConverter fun toTaskStatus(v: String): TaskStatus =
        runCatching { TaskStatus.valueOf(v) }.getOrDefault(TaskStatus.TODO)

    @TypeConverter fun resourceKind(v: ResourceKind): String = v.name
    @TypeConverter fun toResourceKind(v: String): ResourceKind =
        runCatching { ResourceKind.valueOf(v) }.getOrDefault(ResourceKind.ARTICLE)

    @TypeConverter fun ids(v: List<String>): String = idJson.encodeToString(idListSerializer, v)
    @TypeConverter fun toIds(v: String): List<String> =
        if (v.isBlank()) emptyList()
        else runCatching { idJson.decodeFromString(idListSerializer, v) }.getOrDefault(emptyList())

    private companion object {
        val idJson = Json { ignoreUnknownKeys = true }
        val idListSerializer = ListSerializer(String.serializer())
    }
}
