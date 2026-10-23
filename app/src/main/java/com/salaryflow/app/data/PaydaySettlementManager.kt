package com.salaryflow.app.data

import com.salaryflow.app.core.Money
import com.salaryflow.app.core.SalaryClock
import com.salaryflow.app.core.SettlementPeriod
import kotlinx.coroutines.flow.first
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId

data class SettlementResult(
    val period: SettlementPeriod,
    val periodsSettled: Int,
    val incomeCents: Long,
    val savingCents: Long,
)

/**
 * 发薪日结算：
 * - 到期周期按「月薪 × 攒钱比例」写入攒钱罐（固定金额，不随当月工作日数量波动）；
 * - 可选同时写入一条工资收入，便于账本统计；
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
        var totalSaving = 0L
        due.forEach { period ->
            val occurredAt = period.end.atTime(9, 0).atZone(zoneId).toInstant().toEpochMilli()
            val saving = Money.ratioToCents(settings.savingsRate, settings.monthlySalaryCents)

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

            if (saving > 0) {
                insertOnce(
                    TransactionEntity(
                        type = TxType.SAVING_IN,
                        amountCents = saving,
                        category = "auto_saving",
                        note = "自动寄存 · 工资的 ${settings.savingsRatePercent}%",
                        occurredAt = occurredAt,
                        settlementKey = "saving:${period.key}",
                        periodEnd = period.key,
                        auto = true,
                    ),
                )
                totalSaving += saving
            }
        }

        val last = due.last()
        settingsStore.update {
            it.copy(
                lastSettledPeriodEnd = last.key,
                pendingCelebrationKey = last.key,
                lastSettlementAmountCents = totalSaving,
            )
        }
        return SettlementResult(last, due.size, totalIncome, totalSaving)
    }

    /**
     * 「提前入账」：把当前周期已经虚拟累积的部分按攒钱比例存进罐子。
     * 只用于用户主动操作，不影响发薪日自动结算的去重。
     */
    suspend fun settleCurrentPeriodEarly(now: LocalDateTime = LocalDateTime.now(zoneId)): SettlementResult? {
        val settings = settingsStore.current()
        val clock = SalaryClock(settings.toSalaryConfig())
        val period = clock.accruingPeriod(now)
        val accrued = clock.accruedCentsIn(period, now)
        val saving = Money.ratioToCents(settings.savingsRate, accrued.toLong())
        if (saving <= 0) return null

        val occurredAt = now.atZone(zoneId).toInstant().toEpochMilli()
        insertOnce(
            TransactionEntity(
                type = TxType.SAVING_IN,
                amountCents = saving,
                category = "auto_saving",
                note = "提前入账 · ${period.label} 已累积工资的 ${settings.savingsRatePercent}%",
                occurredAt = occurredAt,
                settlementKey = "manual:${period.key}:$occurredAt",
                periodEnd = period.key,
                auto = true,
            ),
        )
        settingsStore.update {
            it.copy(
                pendingCelebrationKey = "manual:$occurredAt",
                lastSettlementAmountCents = saving,
            )
        }
        return SettlementResult(period, 1, 0L, saving)
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
