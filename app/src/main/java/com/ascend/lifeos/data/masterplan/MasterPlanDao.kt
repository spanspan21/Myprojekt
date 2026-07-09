package com.ascend.lifeos.data.masterplan

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.Transaction
import androidx.room.TypeConverters
import kotlinx.coroutines.flow.Flow

@Dao
interface MasterPlanDao {

    // ---- reads (reactive) ----------------------------------------------------

    @Transaction
    @Query("SELECT * FROM mp_domain ORDER BY orderIndex")
    fun observeDomains(): Flow<List<DomainWithGraph>>

    @Transaction
    @Query("SELECT * FROM mp_node WHERE id = :nodeId")
    fun observeNode(nodeId: String): Flow<NodeWithChildren?>

    @Query("SELECT COUNT(*) FROM mp_domain")
    suspend fun domainCount(): Int

    /** One-shot read of the whole graph — used by the Jarvis Override service. */
    @Transaction
    @Query("SELECT * FROM mp_domain ORDER BY orderIndex")
    suspend fun domainsOnce(): List<DomainWithGraph>

    // ---- mutation ------------------------------------------------------------

    @Query("UPDATE mp_task SET status = :status WHERE id = :taskId")
    suspend fun setTaskStatus(taskId: String, status: TaskStatus)

    @Query("DELETE FROM mp_domain WHERE id = :domainId")
    suspend fun deleteDomain(domainId: String)

    // ---- bulk import ---------------------------------------------------------

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertDomain(domain: SkillDomainEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertNodes(nodes: List<NodeEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertTasks(tasks: List<TaskEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertResources(resources: List<ResourceEntity>)

    /**
     * Land a whole imported domain atomically, as a clean replace: the old graph
     * (if any) is dropped first so removed nodes never linger as orphans, then
     * the fresh graph is written. All-or-nothing — a half-written graph would
     * render as broken constellations with dangling prerequisite edges.
     *
     * Note: replacing resets task completion for this domain. That's the correct
     * semantic for "re-import from source"; progress lives in the source of truth.
     */
    @Transaction
    suspend fun importDomain(
        domain: SkillDomainEntity,
        nodes: List<NodeEntity>,
        tasks: List<TaskEntity>,
        resources: List<ResourceEntity>,
    ) {
        deleteDomain(domain.id) // cascades to old nodes/tasks/resources
        upsertDomain(domain)
        upsertNodes(nodes)
        upsertTasks(tasks)
        upsertResources(resources)
    }
}

@Database(
    entities = [
        SkillDomainEntity::class,
        NodeEntity::class,
        TaskEntity::class,
        ResourceEntity::class,
    ],
    version = 1,
    exportSchema = true,
)
@TypeConverters(MasterPlanConverters::class)
abstract class MasterPlanDatabase : RoomDatabase() {
    abstract fun dao(): MasterPlanDao

    companion object {
        @Volatile private var instance: MasterPlanDatabase? = null

        fun get(context: Context): MasterPlanDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    MasterPlanDatabase::class.java,
                    "ascend_masterplan.db",
                )
                    // Plans are re-importable source data, never the source of
                    // truth — a schema bump can safely rebuild from the assets/PC.
                    .fallbackToDestructiveMigration()
                    .build()
                    .also { instance = it }
            }

        /** Close + forget the instance so backup/restore can swap the files. */
        fun close() = synchronized(this) {
            runCatching { instance?.close() }
            instance = null
        }
    }
}
