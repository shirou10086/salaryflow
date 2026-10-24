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
            val hiddenAmount = context.getString(R.string.amount_hidden_short)
            views.setTextViewText(
                R.id.widget_today,
                if (hidden) {
                    hiddenAmount
                } else {
                    "¥" + Money.centsToYuan(snapshot.todayEarnedCents)
                },
            )
            views.setTextViewText(
                R.id.widget_detail,
                if (hidden) {
                    context.getString(R.string.widget_detail_hidden, hiddenAmount)
                } else if (snapshot.settledToday) {
                    context.getString(
                        R.string.widget_detail_settled,
                        "¥${Money.centsToYuan(snapshot.savingsCents)}",
                    )
                } else {
                    context.getString(
                        R.string.widget_detail_payday,
                        "¥${Money.centsToYuan(snapshot.savingsCents)}",
                        context.getString(
                            R.string.home_days_to_payday,
                            snapshot.daysToPayday,
                        ),
                    )
                },
            )
            views.setTextViewText(
                R.id.widget_status,
                statusText(context, settings, snapshot),
            )
            val intent = PendingIntent.getActivity(
                context,
                20,
                MainActivity.intent(context),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )
            views.setOnClickPendingIntent(R.id.widget_root, intent)
            return views
        }

        private fun statusText(
            context: Context,
            settings: AppSettings,
            snapshot: SalarySnapshot,
        ): String = when {
            snapshot.status == WorkStatus.REST_DAY -> context.getString(R.string.widget_status_rest)
            snapshot.status == WorkStatus.BEFORE_WORK -> context.getString(R.string.widget_status_before_work)
            snapshot.status == WorkStatus.FINISHED -> context.getString(R.string.widget_status_finished)
            snapshot.status == WorkStatus.UNPAID_BREAK -> context.getString(R.string.widget_status_break)
            else -> context.getString(R.string.widget_status_working)
        }
    }
}
