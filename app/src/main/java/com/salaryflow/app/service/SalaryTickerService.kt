package com.salaryflow.app.service

import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.os.SystemClock
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.core.content.getSystemService
import com.salaryflow.app.MainActivity
import com.salaryflow.app.R
import com.salaryflow.app.SalaryFlowApp
import com.salaryflow.app.appContainer
import com.salaryflow.app.core.Money
import com.salaryflow.app.core.SalaryClock
import com.salaryflow.app.core.TimeText
import com.salaryflow.app.core.WorkStatus
import com.salaryflow.app.data.AppSettings
import com.salaryflow.app.widget.SalaryWidgetProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId

/**
 * 常驻通知栏的实时工资服务。
 *
 * 每秒（可配置）重算一次今日已赚金额并刷新通知；同时按小时检查发薪日结算，
 * 保证 App 未打开时也能自动把工资存进攒钱罐。
 */
class SalaryTickerService : Service() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var tickerJob: Job? = null
    private var settlementJob: Job? = null
    private lateinit var notificationManager: NotificationManager

    private var savingsCents: Long = 0L
    private var todayExpenseCents: Long = 0L
    private var settledToday: Boolean = false
    private var cachedExpenseDate: LocalDate? = null
    private var lastWidgetUpdate = 0L

    override fun onCreate() {
        super.onCreate()
        notificationManager = getSystemService() ?: error("NotificationManager unavailable")
        val placeholder = buildNotification(
            settings = AppSettings(),
            snapshot = SalarySnapshotFactory.create(
                settings = AppSettings(),
                now = LocalDateTime.now(),
                savingsCents = 0L,
                todayExpenseCents = 0L,
                settledToday = false,
            ),
        )
        startForeground(SalaryFlowApp.LIVE_NOTIFICATION_ID, placeholder)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP -> {
                scope.launch {
                    appContainer.settingsStore.setServiceEnabled(false)
                    stopForeground(STOP_FOREGROUND_REMOVE)
                    stopSelf()
                }
                return START_NOT_STICKY
            }

            ACTION_TOGGLE_PAUSE -> {
                scope.launch {
                    appContainer.settingsStore.update { it.copy(counterPaused = !it.counterPaused) }
                }
            }
        }

        if (tickerJob == null) {
            startTicker()
            startSettlementChecks()
        }
        return START_STICKY
    }

    private fun startTicker() {
        tickerJob = scope.launch {
            appContainer.settingsStore.settings.collectLatest { settings ->
                if (!settings.serviceEnabled) {
                    stopForeground(STOP_FOREGROUND_REMOVE)
                    stopSelf()
                    return@collectLatest
                }
                val clock = SalaryClock(settings.toSalaryConfig())
                var cacheAge = Int.MAX_VALUE
                while (isActive) {
                    val now = LocalDateTime.now()
                    if (cacheAge >= REFRESH_DB_EVERY_TICKS || cachedExpenseDate != now.toLocalDate()) {
                        refreshLedgerCache(now)
                        cacheAge = 0
                    } else {
                        cacheAge++
                    }

                    val snapshot = SalarySnapshotFactory.create(
                        settings = settings,
                        now = now,
                        savingsCents = savingsCents,
                        todayExpenseCents = todayExpenseCents,
                        settledToday = settledToday,
                    )
                    runCatching {
                        notificationManager.notify(
                            SalaryFlowApp.LIVE_NOTIFICATION_ID,
                            buildNotification(settings, snapshot),
                        )
                    }

                    val nowElapsed = SystemClock.elapsedRealtime()
                    if (nowElapsed - lastWidgetUpdate > WIDGET_UPDATE_INTERVAL_MS) {
                        lastWidgetUpdate = nowElapsed
                        SalaryWidgetProvider.updateAll(this@SalaryTickerService, settings, snapshot)
                    }

                    val intervalSeconds = settings.tickSeconds.coerceIn(1, 300)
                    val millisToNextSecond = 1000L - (now.nano / 1_000_000) % 1000L
                    delay(intervalSeconds * 1000L - 1000L + millisToNextSecond)
                }
            }
        }
    }

    private fun startSettlementChecks() {
        settlementJob = scope.launch {
            while (isActive) {
                runCatching {
                    val result = appContainer.settlementManager.settleIfDue()
                    if (result != null) {
                        SettlementNotifier.notifySettled(this@SalaryTickerService, result)
                    }
                }
                delay(SETTLEMENT_CHECK_INTERVAL_MS)
            }
        }
    }

    private suspend fun refreshLedgerCache(now: LocalDateTime) {
        withContext(Dispatchers.IO) {
            val zone = ZoneId.systemDefault()
            val dayStart = now.toLocalDate().atStartOfDay(zone).toInstant().toEpochMilli()
            val dayEnd = now.toLocalDate().plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli()
            savingsCents = appContainer.ledgerDao.savingsCentsNow()
            todayExpenseCents = appContainer.ledgerDao.expenseSumBetween(dayStart, dayEnd)
            settledToday = appContainer.ledgerDao.hasAutoSettlementForPeriod(now.toLocalDate().toString())
            cachedExpenseDate = now.toLocalDate()
        }
    }

    private fun buildNotification(settings: AppSettings, snapshot: SalarySnapshot): Notification {
        val hidden = settings.hideAmounts
        val earned = if (hidden) "¥•••" else "¥" + Money.centsToYuan(snapshot.todayEarnedCents)
        val savings = if (hidden) "¥•••" else "¥" + Money.centsToYuan(snapshot.savingsCents)
        val expense = if (hidden) "¥•••" else "¥" + Money.centsToYuan(snapshot.todayExpenseCents)

        val title = when {
            settings.counterPaused -> "已暂停 · 工资暂停上涨"
            snapshot.status == WorkStatus.REST_DAY -> "今天休息 · 今日已赚 $earned"
            snapshot.status == WorkStatus.BEFORE_WORK -> "还没开工 · 今日已赚 $earned"
            snapshot.status == WorkStatus.FINISHED -> "今日收工 · 一共赚了 $earned"
            else -> "今日已赚 $earned"
        }

        val countdown = when (snapshot.status) {
            WorkStatus.WORKING, WorkStatus.UNPAID_BREAK -> {
                val remaining = java.time.Duration.between(
                    LocalDateTime.now(),
                    LocalDate.now().atTime(snapshot.offWorkAt),
                )
                "距下班 ${TimeText.countdown(remaining)}"
            }
            WorkStatus.BEFORE_WORK -> {
                val remaining = java.time.Duration.between(
                    LocalDateTime.now(),
                    LocalDate.now().atTime(snapshot.workStartAt),
                )
                "距上班 ${TimeText.countdown(remaining)}"
            }
            WorkStatus.FINISHED -> "今天辛苦了"
            WorkStatus.REST_DAY -> "好好休息"
        }

        val payday = when {
            snapshot.settledToday -> "今日已结算 ✅"
            snapshot.daysToPayday <= 0L -> "今天是发薪日 🧧"
            else -> "距发薪日 ${snapshot.daysToPayday} 天"
        }

        val bigText = buildString {
            append("已攒 $savings · 今日支出 $expense\n")
            append("$countdown · $payday\n")
            if (!hidden) {
                append("每秒 +¥${Money.centsToYuanHighPrecision(snapshot.centsPerSecond)}")
            }
        }

        val contentIntent = PendingIntent.getActivity(
            this,
            0,
            MainActivity.intent(this),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val quickAddIntent = PendingIntent.getActivity(
            this,
            1,
            MainActivity.quickAddIntent(this),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val toggleIntent = PendingIntent.getService(
            this,
            2,
            Intent(this, SalaryTickerService::class.java).setAction(ACTION_TOGGLE_PAUSE),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val stopIntent = PendingIntent.getService(
            this,
            3,
            Intent(this, SalaryTickerService::class.java).setAction(ACTION_STOP),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val builder = NotificationCompat.Builder(this, SalaryFlowApp.LIVE_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText("已攒 $savings · 今日支出 $expense · $payday")
            .setSubText(if (hidden) "薪流 · 金额已隐藏" else "薪流 · 每秒都在涨")
            .setStyle(NotificationCompat.BigTextStyle().bigText(bigText))
            .setContentIntent(contentIntent)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setSilent(true)
            .setShowWhen(false)
            .setCategory(NotificationCompat.CATEGORY_STATUS)
            .setVisibility(if (hidden) NotificationCompat.VISIBILITY_SECRET else NotificationCompat.VISIBILITY_PUBLIC)
            .setProgress(1000, (snapshot.dayProgress * 1000).toInt().coerceIn(0, 1000), false)
            .addAction(R.drawable.ic_notification, "记一笔", quickAddIntent)
            .addAction(
                R.drawable.ic_notification,
                if (settings.counterPaused) "继续" else "暂停",
                toggleIntent,
            )
            .addAction(R.drawable.ic_notification, "停止", stopIntent)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            builder.setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
        }
        return builder.build()
    }

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        const val ACTION_TOGGLE_PAUSE = "com.salaryflow.app.action.TOGGLE_PAUSE"
        const val ACTION_STOP = "com.salaryflow.app.action.STOP"

        private const val REFRESH_DB_EVERY_TICKS = 5
        private const val WIDGET_UPDATE_INTERVAL_MS = 20_000L
        private const val SETTLEMENT_CHECK_INTERVAL_MS = 60L * 60L * 1000L

        fun start(context: Context) {
            val intent = Intent(context, SalaryTickerService::class.java)
            ContextCompat.startForegroundService(context, intent)
        }

        fun stop(context: Context) {
            context.startService(Intent(context, SalaryTickerService::class.java).setAction(ACTION_STOP))
        }
    }
}
