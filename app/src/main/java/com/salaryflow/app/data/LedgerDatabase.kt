package com.salaryflow.app.data

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Delete
import androidx.room.Entity
import androidx.room.Index
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import kotlinx.coroutines.flow.Flow

@Entity(
    tableName = "tx",
    indices = [
        Index(value = ["settlementKey"], unique = true),
        Index(value = ["occurredAt"]),
    ],
)
data class TransactionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val type: TxType,
    /** 金额，单位分，恒为正数。 */
    val amountCents: Long,
    val category: String,
    val note: String = "",
    val occurredAt: Long,
    /** 自动结算去重键，例如 saving:2026-10-15。手动记账为 null。 */
    val settlementKey: String? = null,
    /** 所属工资周期结束日 ISO 字符串，手动记账为 null。 */
    val periodEnd: String? = null,
    val auto: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
)

@Dao
interface LedgerDao {
    @Query("SELECT * FROM tx ORDER BY occurredAt DESC, id DESC")
    fun observeAll(): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM tx WHERE occurredAt >= :from AND occurredAt < :to ORDER BY occurredAt DESC, id DESC")
    fun observeBetween(from: Long, to: Long): Flow<List<TransactionEntity>>

    @Query(
        "SELECT COALESCE(SUM(amountCents), 0) FROM tx " +
            "WHERE type = 'EXPENSE' AND occurredAt >= :from AND occurredAt < :to",
    )
    fun observeExpenseSum(from: Long, to: Long): Flow<Long>

    @Query(
        "SELECT COALESCE(SUM(amountCents), 0) FROM tx " +
            "WHERE type = 'INCOME' AND occurredAt >= :from AND occurredAt < :to",
    )
    fun observeIncomeSum(from: Long, to: Long): Flow<Long>

    @Query(
        "SELECT COALESCE(SUM(amountCents), 0) FROM tx " +
            "WHERE type = 'SAVING_IN' AND occurredAt >= :from AND occurredAt < :to",
    )
    fun observeSavingInSum(from: Long, to: Long): Flow<Long>

    @Query(
        "SELECT category AS category, SUM(amountCents) AS totalCents FROM tx " +
            "WHERE type = 'EXPENSE' AND occurredAt >= :from AND occurredAt < :to " +
            "GROUP BY category ORDER BY totalCents DESC",
    )
    fun observeExpenseByCategory(from: Long, to: Long): Flow<List<CategoryTotal>>

    @Query(
        "SELECT COALESCE(SUM(CASE WHEN type = 'SAVING_IN' THEN amountCents ELSE -amountCents END), 0) " +
            "FROM tx WHERE type IN ('SAVING_IN', 'SAVING_OUT')",
    )
    fun observeSavingsCents(): Flow<Long>

    @Query("SELECT * FROM tx WHERE settlementKey = :key LIMIT 1")
    suspend fun findBySettlementKey(key: String): TransactionEntity?

    @Query("SELECT MAX(periodEnd) FROM tx WHERE settlementKey LIKE 'saving:%'")
    suspend fun latestSettledPeriodEnd(): String?

    @Query(
        "SELECT COUNT(*) > 0 FROM tx " +
            "WHERE periodEnd = :periodEnd AND auto = 1 " +
            "AND (settlementKey LIKE 'saving:%' OR settlementKey LIKE 'income:%')",
    )
    suspend fun hasAutoSettlementForPeriod(periodEnd: String): Boolean

    @Query(
        "SELECT COALESCE(SUM(amountCents), 0) FROM tx " +
            "WHERE type = 'EXPENSE' AND occurredAt >= :from AND occurredAt < :to",
    )
    suspend fun expenseSumBetween(from: Long, to: Long): Long

    @Query(
        "SELECT COALESCE(SUM(CASE WHEN type = 'SAVING_IN' THEN amountCents ELSE -amountCents END), 0) " +
            "FROM tx WHERE type IN ('SAVING_IN', 'SAVING_OUT')",
    )
    suspend fun savingsCentsNow(): Long

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(transaction: TransactionEntity): Long

    @Delete
    suspend fun delete(transaction: TransactionEntity)

    @Query("DELETE FROM tx")
    suspend fun clearAll()
}

@Database(
    entities = [TransactionEntity::class],
    version = 1,
    exportSchema = true,
)
@TypeConverters(Converters::class)
abstract class SalaryFlowDatabase : RoomDatabase() {
    abstract fun ledgerDao(): LedgerDao

    companion object {
        fun build(context: Context): SalaryFlowDatabase =
            Room.databaseBuilder(context.applicationContext, SalaryFlowDatabase::class.java, "salary_flow.db")
                .build()
    }
}
