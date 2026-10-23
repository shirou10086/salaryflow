package com.salaryflow.app.service

import android.content.Context
import com.salaryflow.app.appContainer
import com.salaryflow.app.core.SalaryClock
import com.salaryflow.app.core.WorkStatus
import com.salaryflow.app.data.AppSettings
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId

data class SalarySnapshot(
    val todayEarnedCents: Double,
    val periodAccruedCents: Double,
    val savingsCents: Long,
    val todayExpenseCents: Long,
    val status: WorkStatus,
    val workStartAt: LocalTime,
    val offWorkAt: LocalTime,
    val nextPayday: LocalDate,
    val daysToPayday: Long,
    val settledToday: Boolean,
    val dayProgress: Float,
    val centsPerSecond: Double,
    val working: Boolean,
)

object SalarySnapshotFactory {

    suspend fun create(
        context: Context,
        settings: AppSettings,
        now: LocalDateTime = LocalDateTime.now(),
        zoneId: ZoneId = ZoneId.systemDefault(),
    ): SalarySnapshot {
        val container = context.appContainer
        val dayStart = now.toLocalDate().atStartOfDay(zoneId).toInstant().toEpochMilli()
        val dayEnd = now.toLocalDate().plusDays(1).atStartOfDay(zoneId).toInstant().toEpochMilli()
        return create(
            settings = settings,
            now = now,
            savingsCents = container.ledgerDao.savingsCentsNow(),
            todayExpenseCents = container.ledgerDao.expenseSumBetween(dayStart, dayEnd),
            settledToday = container.ledgerDao.hasAutoSettlementForPeriod(now.toLocalDate().toString()),
        )
    }

    fun create(
        settings: AppSettings,
        now: LocalDateTime,
        savingsCents: Long,
        todayExpenseCents: Long,
        settledToday: Boolean = false,
    ): SalarySnapshot {
        val clock = SalaryClock(settings.toSalaryConfig())
        val status = clock.status(now)
        val working = status == WorkStatus.WORKING && !settings.counterPaused
        return SalarySnapshot(
            todayEarnedCents = clock.earnedToday(now),
            periodAccruedCents = clock.accruedCentsIn(clock.accruingPeriod(now), now),
            savingsCents = savingsCents,
            todayExpenseCents = todayExpenseCents,
            status = if (settings.counterPaused) WorkStatus.BEFORE_WORK else status,
            workStartAt = settings.toSalaryConfig().schedule.startTime,
            offWorkAt = settings.toSalaryConfig().schedule.endTime,
            nextPayday = clock.nextPaydayAfter(now.toLocalDate().minusDays(1)),
            daysToPayday = clock.daysToPayday(now.toLocalDate()),
            settledToday = settledToday,
            dayProgress = clock.dayProgress(now),
            centsPerSecond = clock.centsPerSecond,
            working = working,
        )
    }
}
