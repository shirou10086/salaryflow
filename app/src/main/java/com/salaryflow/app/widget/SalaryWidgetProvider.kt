package com.salaryflow.app.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import com.salaryflow.app.MainActivity
import com.salaryflow.app.R
import com.salaryflow.app.appContainer
import com.salaryflow.app.core.Money
import com.salaryflow.app.core.WorkStatus
import com.salaryflow.app.data.AppSettings
import com.salaryflow.app.service.SalarySnapshot
import com.salaryflow.app.service.SalarySnapshotFactory
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class SalaryWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        val pendingResult = goAsync()
        CoroutineScope(SupervisorJob() + Dispatchers.Default).launch {
            try {
                val settings = context.appContainer.settingsStore.current()
                val snapshot = SalarySnapshotFactory.create(context, settings)
                appWidgetIds.forEach { id ->
                    appWidgetManager.updateAppWidget(id, views(context, settings, snapshot))
                }
            } finally {
                pendingResult.finish()
            }
        }
    }

    companion object {
        fun updateAll(context: Context, settings: AppSettings, snapshot: SalarySnapshot) {
            val manager = AppWidgetManager.getInstance(context) ?: return
            val ids = manager.getAppWidgetIds(ComponentName(context, SalaryWidgetProvider::class.java))
            if (ids.isEmpty()) return
            val views = views(context, settings, snapshot)
            ids.forEach { manager.updateAppWidget(it, views) }
        }

        private fun views(context: Context, settings: AppSettings, snapshot: SalarySnapshot): RemoteViews {
            val views = RemoteViews(context.packageName, R.layout.widget_salary)
            val hidden = settings.hideAmounts
            views.setTextViewText(
                R.id.widget_today,
                if (hidden) "¥•••" else "¥" + Money.centsToYuan(snapshot.todayEarnedCents),
            )
            views.setTextViewText(
                R.id.widget_detail,
                if (hidden) {
                    "今日已赚 · 已攒 ¥•••"
                } else if (snapshot.settledToday) {
                    "已攒 ¥${Money.centsToYuan(snapshot.savingsCents)} · 今日已结算"
                } else {
                    "已攒 ¥${Money.centsToYuan(snapshot.savingsCents)} · 距发薪 ${snapshot.daysToPayday} 天"
                },
            )
            views.setTextViewText(R.id.widget_status, statusText(settings, snapshot))
            val intent = PendingIntent.getActivity(
                context,
                20,
                MainActivity.intent(context),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )
            views.setOnClickPendingIntent(R.id.widget_root, intent)
            return views
        }

        private fun statusText(settings: AppSettings, snapshot: SalarySnapshot): String = when {
            settings.counterPaused -> "已暂停 ⏸"
            snapshot.status == WorkStatus.REST_DAY -> "休息日"
            snapshot.status == WorkStatus.BEFORE_WORK -> "还没开工"
            snapshot.status == WorkStatus.FINISHED -> "已下班"
            snapshot.status == WorkStatus.UNPAID_BREAK -> "午休中"
            else -> "工作中"
        }
    }
}
