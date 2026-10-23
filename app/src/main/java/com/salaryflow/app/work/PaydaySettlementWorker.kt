package com.salaryflow.app.work

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.salaryflow.app.appContainer
import com.salaryflow.app.core.SalaryClock
import com.salaryflow.app.service.SettlementNotifier
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.YearMonth
import java.time.ZoneId
import java.util.concurrent.TimeUnit

class PaydaySettlementWorker(
    context: Context,
    params: WorkerParameters,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        return runCatching {
            val result = applicationContext.appContainer.settlementManager.settleIfDue()
            if (result != null) {
                SettlementNotifier.notifySettled(applicationContext, result)
            }
            Result.success()
        }.getOrElse { Result.retry() }
    }
}

object PaydaySettlementScheduler {

    private const val PERIODIC_WORK = "payday-settlement-daily"
    private const val NEXT_PAYDAY_WORK = "payday-settlement-next"

    fun ensureScheduled(context: Context) {
        val workManager = WorkManager.getInstance(context)

        val daily = PeriodicWorkRequestBuilder<PaydaySettlementWorker>(1, TimeUnit.DAYS)
            .build()
        workManager.enqueueUniquePeriodicWork(
            PERIODIC_WORK,
            ExistingPeriodicWorkPolicy.KEEP,
            daily,
        )

        val delay = delayToNextPayday(context)
        val nextPayday = OneTimeWorkRequestBuilder<PaydaySettlementWorker>()
            .setInitialDelay(delay, TimeUnit.MILLISECONDS)
            .build()
        workManager.enqueueUniqueWork(NEXT_PAYDAY_WORK, ExistingWorkPolicy.REPLACE, nextPayday)
    }

    private fun delayToNextPayday(context: Context): Long {
        val now = LocalDateTime.now()
        val zone = ZoneId.systemDefault()
        val payday = runCatching {
            val settings = kotlinx.coroutines.runBlocking { context.appContainer.settingsStore.current() }
            val clock = SalaryClock(settings.toSalaryConfig())
            val today = LocalDate.now()
            val todayPayday = clock.paydayOf(YearMonth.from(today))
            if (todayPayday == today && now.toLocalTime().isBefore(LocalTime.of(9, 0))) {
                today
            } else {
                clock.nextPaydayAfter(today)
            }
        }.getOrElse { LocalDate.now().plusDays(1) }

        val target = payday.atTime(9, 0).atZone(zone).toInstant()
        val millis = Duration.between(now.atZone(zone).toInstant(), target).toMillis()
        return millis.coerceAtLeast(60_000L)
    }
}
