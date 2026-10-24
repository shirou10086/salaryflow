package com.salaryflow.app.data

import com.salaryflow.app.core.SalaryClock
import com.salaryflow.app.core.SettlementPeriod
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId

data class SettlementResult(
    val period: SettlementPeriod,
    val periodsSettled: Int,
    val incomeCents: Long,
    val landedCents: Long,
)

/**
 * 发薪日结算：
 * - 到期后按完整月薪写入一条工资收入，存款余额会随账本自动增加；
 * - 不再使用任意攒钱比例，避免和真实记账产生双重计算；
 * - 通过 settlementKey 保证多次触发也只入账一次；
 * - 支持补结算漏掉的周期（最多 24 期）。
 */
class PaydaySettlementManager(
    private val dao: LedgerDao,
    private val settingsStore: SettingsStore,
    private val zoneId: ZoneId = ZoneId.systemDefault(),
) {

    suspend fun settleIfDue(now: LocalDateTime = LocalDateTime.now(zoneId)): SettlementResult? {
        val settings = settingsStore.current()
        if (!settings.autoSettleEnabled) return null

        val clock = SalaryClock(settings.toSalaryConfig())
        val lastSettled = dao.latestSettledPeriodEnd()?.toLocalDateOrNull()
            ?: settings.lastSettledPeriodEnd?.toLocalDateOrNull()

        // 首次安装不从历史工资里“凭空”补发。先记下最近一次发薪日作为基线，
        // 从下一次到期的完整周期开始自动寄存。
        if (lastSettled == null) {
            val baseline = clock.paydayOnOrBefore(now.toLocalDate())
            settingsStore.update { it.copy(lastSettledPeriodEnd = baseline.toString()) }
            return null
        }

        val due = clock.duePeriods(lastSettled, now)
        if (due.isEmpty()) return null

        var totalIncome = 0L
        due.forEach { period ->
            val occurredAt = period.end.atTime(9, 0).atZone(zoneId).toInstant().toEpochMilli()

            if (settings.autoIncomeEnabled) {
                insertOnce(
                    TransactionEntity(
                        type = TxType.INCOME,
                        amountCents = settings.monthlySalaryCents,
                        category = "salary",
                        note = "发薪日自动记账 · ${period.label}",
                        occurredAt = occurredAt,
                        settlementKey = "income:${period.key}",
                        periodEnd = period.key,
                        auto = true,
                    ),
                )
                totalIncome += settings.monthlySalaryCents
            }
        }

        val last = due.last()
        settingsStore.update {
            it.copy(
                lastSettledPeriodEnd = last.key,
                pendingCelebrationKey = last.key,
                lastSettlementAmountCents = settings.monthlySalaryCents,
            )
        }
        return SettlementResult(
            period = last,
            periodsSettled = due.size,
            incomeCents = totalIncome,
            landedCents = settings.monthlySalaryCents,
        )
    }

    private suspend fun insertOnce(transaction: TransactionEntity) {
        val key = transaction.settlementKey ?: return
        if (dao.findBySettlementKey(key) == null) {
            dao.insert(transaction)
        }
    }
}

private fun String.toLocalDateOrNull(): LocalDate? =
    runCatching { LocalDate.parse(this) }.getOrNull()
