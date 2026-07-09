package com.ascend.lifeos.data.finance

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.Upsert

// ─── Finance in ONE relational store (audit Phase 3 CRITICAL) ────────────────
// Replaces the two-prefs-store split (txns in LifeStores + accounts and a
// hand-maintained txn→account MAP in FinanceStore). The account link is now a
// real foreign key on the txn row, so referential integrity is enforced by the
// database and the pruneMap/dual-rev bookkeeping disappears.

@Entity(tableName = "accounts")
data class AccountEntity(
    @PrimaryKey val id: String,
    val name: String,
    val icon: String,
    val balanceCents: Long,
    val orderIdx: Int = 0,
)

@Entity(
    tableName = "txns",
    foreignKeys = [
        ForeignKey(
            entity = AccountEntity::class,
            parentColumns = ["id"],
            childColumns = ["accountId"],
            onDelete = ForeignKey.SET_NULL,
        ),
    ],
    indices = [Index("accountId")],
)
data class TxnEntity(
    @PrimaryKey val id: String,
    val ts: Long,
    val amountCents: Long,
    val category: String,
    val note: String,
    val accountId: String? = null,
)

@Dao
interface FinanceDao {
    @Query("SELECT * FROM accounts ORDER BY orderIdx, name") suspend fun accounts(): List<AccountEntity>
    @Query("SELECT * FROM txns ORDER BY ts DESC") suspend fun txns(): List<TxnEntity>

    @Upsert suspend fun upsertAccount(a: AccountEntity)
    @Upsert suspend fun upsertTxn(t: TxnEntity)
    @Upsert suspend fun upsertAccounts(a: List<AccountEntity>)
    @Upsert suspend fun upsertTxns(t: List<TxnEntity>)

    @Query("DELETE FROM accounts WHERE id = :id") suspend fun deleteAccount(id: String)
    @Query("DELETE FROM txns WHERE id = :id") suspend fun deleteTxn(id: String)
    @Query("UPDATE txns SET accountId = :accountId WHERE id = :txnId") suspend fun setTxnAccount(txnId: String, accountId: String?)
    @Query("SELECT accountId FROM txns WHERE id = :txnId") suspend fun accountOf(txnId: String): String?
}

@Database(entities = [AccountEntity::class, TxnEntity::class], version = 1, exportSchema = true)
abstract class FinanceDatabase : RoomDatabase() {
    abstract fun dao(): FinanceDao

    companion object {
        @Volatile private var instance: FinanceDatabase? = null
        fun get(ctx: Context): FinanceDatabase = instance ?: synchronized(this) {
            instance ?: Room.databaseBuilder(
                ctx.applicationContext, FinanceDatabase::class.java, "jarvis_finance.db",
            ).build().also { instance = it }
        }
    }
}
