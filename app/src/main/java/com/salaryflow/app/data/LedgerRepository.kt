package com.salaryflow.app.data

import kotlinx.coroutines.flow.Flow
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId

class LedgerRepository(private val dao: LedgerDao, private val zoneId: ZoneId = ZoneId.systemDefault()) {

    fun observeAll(): Flow<List<TransactionEntity>> = dao.observeAll()

    fun observeMonth(month: YearMonth): Flow<List<TransactionEntity>> =
        dao.observeBetween(month.startMillis(zoneId), month.endMillis(zoneId))

    fun observeExpenseSum(month: YearMonth): Flow<Long> =
        dao.observeExpenseSum(month.startMillis(zoneId), month.endMillis(zoneId))

    fun observeIncomeSum(month: YearMonth): Flow<Long> =
        dao.observeIncomeSum(month.startMillis(zoneId), month.endMillis(zoneId))

    fun observeSavingInSum(month: YearMonth): Flow<Long> =
        dao.observeSavingInSum(month.startMillis(zoneId), month.endMillis(zoneId))

    fun observeExpenseByCategory(month: YearMonth): Flow<List<CategoryTotal>> =
        dao.observeExpenseByCategory(month.startMillis(zoneId), month.endMillis(zoneId))

    fun observeSavingsCents(): Flow<Long> = dao.observeSavingsCents()

    /** 今日支出，用于首页与通知栏。 */
    fun observeDaySum(type: TxType, date: LocalDate): Flow<Long> {
        val from = date.atStartOfDay(zoneId).toInstant().toEpochMilli()
        val to = date.plusDays(1).atStartOfDay(zoneId).toInstant().toEpochMilli()
        return when (type) {
            TxType.EXPENSE -> dao.observeExpenseSum(from, to)
            TxType.INCOME -> dao.observeIncomeSum(from, to)
            TxType.SAVING_IN -> dao.observeSavingInSum(from, to)
            TxType.SAVING_OUT -> dao.observeSavingInSum(from, to)
        }
    }

    suspend fun addExpense(
        amountCents: Long,
        category: String,
        note: String,
        occurredAt: Long = System.currentTimeMillis(),
    ) = add(TxType.EXPENSE, amountCents, category, note, occurredAt)

    suspend fun addIncome(
        amountCents: Long,
        category: String,
        note: String,
        occurredAt: Long = System.currentTimeMillis(),
    ) = add(TxType.INCOME, amountCents, category, note, occurredAt)

    suspend fun addSaving(
        amountCents: Long,
        note: String,
        occurredAt: Long = System.currentTimeMillis(),
        type: TxType = TxType.SAVING_IN,
    ) = add(type, amountCents, if (type == TxType.SAVING_IN) "manual_saving" else "saving_out", note, occurredAt)

    private suspend fun add(
        type: TxType,
        amountCents: Long,
        category: String,
        note: String,
        occurredAt: Long,
    ) {
        if (amountCents <= 0) return
        dao.insert(
            TransactionEntity(
                type = type,
                amountCents = amountCents,
                category = category,
                note = note,
                occurredAt = occurredAt,
            ),
        )
    }

    suspend fun delete(transaction: TransactionEntity) = dao.delete(transaction)

    suspend fun clearAll() = dao.clearAll()
}

private fun YearMonth.startMillis(zoneId: ZoneId): Long =
    atDay(1).atStartOfDay(zoneId).toInstant().toEpochMilli()

private fun YearMonth.endMillis(zoneId: ZoneId): Long =
    plusMonths(1).atDay(1).atStartOfDay(zoneId).toInstant().toEpochMilli()
