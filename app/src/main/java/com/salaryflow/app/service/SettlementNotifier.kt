package com.salaryflow.app.service

import android.Manifest
import android.app.PendingIntent
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.salaryflow.app.MainActivity
import com.salaryflow.app.R
import com.salaryflow.app.SalaryFlowApp
import com.salaryflow.app.core.Money
import com.salaryflow.app.core.TimeText
import com.salaryflow.app.data.SettlementResult

object SettlementNotifier {

    fun notifySettled(context: Context, result: SettlementResult) {
        if (
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            return
        }

        val notificationManager = NotificationManagerCompat.from(context)
        if (!notificationManager.areNotificationsEnabled()) return

        val contentIntent = PendingIntent.getActivity(
            context,
            10,
            MainActivity.celebrationIntent(context),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val amount = "¥${Money.centsToYuan(result.landedCents)}"
        val text = if (result.periodsSettled > 1) {
            context.getString(
                R.string.settlement_notification_multiple,
                amount,
                result.periodsSettled,
            )
        } else {
            context.getString(R.string.settlement_notification_body, amount)
        }
        val locale = context.resources.configuration.locales[0]
        val periodLabel = TimeText.period(result.period.start, result.period.end, locale)

        val notification = NotificationCompat.Builder(context, SalaryFlowApp.SETTLEMENT_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(context.getString(R.string.settlement_notification_title))
            .setContentText(text)
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText(
                        text + "\n" + context.getString(
                            R.string.settlement_notification_period,
                            periodLabel,
                        ),
                    ),
            )
            .setContentIntent(contentIntent)
            .setAutoCancel(true)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .build()

        runCatching {
            notificationManager.notify(SalaryFlowApp.SETTLEMENT_NOTIFICATION_ID, notification)
        }
    }
}
