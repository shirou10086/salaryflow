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

        val text = buildString {
            append("¥${Money.centsToYuan(result.savingCents)} 已存进攒钱罐")
            if (result.periodsSettled > 1) append("（补记 ${result.periodsSettled} 期）")
        }

        val notification = NotificationCompat.Builder(context, SalaryFlowApp.SETTLEMENT_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("工资已经到账")
            .setContentText(text)
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText("$text\n周期：${result.period.label}"),
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
