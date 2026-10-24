package com.salaryflow.app.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.salaryflow.app.appContainer
import com.salaryflow.app.work.BalanceReminderScheduler
import com.salaryflow.app.work.PaydaySettlementScheduler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED &&
            intent.action != Intent.ACTION_MY_PACKAGE_REPLACED
        ) {
            return
        }
        PaydaySettlementScheduler.ensureScheduled(context)
        BalanceReminderScheduler.ensureScheduled(context)

        val pendingResult = goAsync()
        CoroutineScope(SupervisorJob() + Dispatchers.Default).launch {
            try {
                val settings = context.appContainer.settingsStore.current()
                if (settings.serviceEnabled && settings.bootStartEnabled) {
                    runCatching { SalaryTickerService.start(context) }
                }
            } finally {
                pendingResult.finish()
            }
        }
    }
}
