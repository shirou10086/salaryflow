package com.salaryflow.app.work

import android.Manifest
import android.app.PendingIntent
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.salaryflow.app.MainActivity
import com.salaryflow.app.R
import com.salaryflow.app.SalaryFlowApp
import com.salaryflow.app.appContainer
import kotlinx.coroutines.runBlocking
import java.time.Duration
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.YearMonth
import java.time.ZoneId
import java.util.concurrent.TimeUnit

/**
 * 每月提醒用户核对一次当前存款。
 */
class BalanceReminderWorker(
    context: Context,
    params: WorkerParameters,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result = runCatching {
        val store = applicationContext.appContainer.settingsStore
        val settings = store.current()
        val monthKey = YearMonth.now().toString()

        if (
            settings.onboarded &&
            settings.balanceReminderEnabled &&
            settings.lastBalanceReminderMonth != monthKey &&
            BalanceReminderNotifier.notify(applicationContext)
        ) {
            store.markBalanceReminderSent(monthKey)
        }

        BalanceReminderScheduler.ensureScheduled(applicationContext)
        Result.success()
    }.getOrElse {
        BalanceReminderScheduler.ensureScheduled(applicationContext)
        Result.retry()
    }
}

object BalanceReminderScheduler {

    private const val UNIQUE_WORK = "balance-calibration-reminder"

    fun ensureScheduled(context: Context) {
        val workManager = WorkManager.getInstance(context)
        val settings = runCatching {
            runBlocking { context.appContainer.settingsStore.current() }
        }.getOrNull()

        if (settings == null || !settings.onboarded || !settings.balanceReminderEnabled) {
            workManager.cancelUniqueWork(UNIQUE_WORK)
            return
        }

        val request = OneTimeWorkRequestBuilder<BalanceReminderWorker>()
            .setInitialDelay(delayToNextReminder(), TimeUnit.MILLISECONDS)
            .build()
        workManager.enqueueUniqueWork(UNIQUE_WORK, ExistingWorkPolicy.REPLACE, request)
    }

    private fun delayToNextReminder(now: LocalDateTime = LocalDateTime.now()): Long {
        val zone = ZoneId.systemDefault()
        val today = now.toLocalDate()
        val thisMonth = today.withDayOfMonth(1).atTime(LocalTime.of(10, 0))
        val target = if (thisMonth.isAfter(now)) {
            thisMonth
        } else {
            YearMonth.from(today).plusMonths(1).atDay(1).atTime(LocalTime.of(10, 0))
        }
        return Duration.between(
            now.atZone(zone).toInstant(),
            target.atZone(zone).toInstant(),
        ).toMillis().coerceAtLeast(60_000L)
    }
}

private object BalanceReminderNotifier {

    fun notify(context: Context): Boolean {
        if (
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            return false
        }

        val manager = NotificationManagerCompat.from(context)
        if (!manager.areNotificationsEnabled()) return false

        val contentIntent = PendingIntent.getActivity(
            context,
            30,
            MainActivity.balanceUpdateIntent(context),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val notification = NotificationCompat.Builder(
            context,
            SalaryFlowApp.BALANCE_REMINDER_CHANNEL_ID,
        )
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(context.getString(R.string.balance_reminder_title))
            .setContentText(context.getString(R.string.balance_reminder_text))
            .setStyle(
                NotificationCompat.BigTextStyle().bigText(
                    context.getString(R.string.balance_reminder_big_text),
                ),
            )
            .setContentIntent(contentIntent)
            .addAction(
                R.drawable.ic_action_balance,
                context.getString(R.string.balance_reminder_action),
                contentIntent,
            )
            .setAutoCancel(true)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .build()

        return runCatching {
            manager.notify(SalaryFlowApp.BALANCE_REMINDER_NOTIFICATION_ID, notification)
            true
        }.getOrDefault(false)
    }
}
