package com.salaryflow.app

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import androidx.core.content.getSystemService
import com.salaryflow.app.data.LedgerDao
import com.salaryflow.app.data.LedgerRepository
import com.salaryflow.app.data.PaydaySettlementManager
import com.salaryflow.app.data.SalaryFlowDatabase
import com.salaryflow.app.data.SettingsStore
import com.salaryflow.app.work.PaydaySettlementScheduler

class AppContainer(context: Context) {
    private val appContext = context.applicationContext

    val database: SalaryFlowDatabase = SalaryFlowDatabase.build(appContext)
    val ledgerDao: LedgerDao = database.ledgerDao()
    val ledgerRepository: LedgerRepository = LedgerRepository(ledgerDao)
    val settingsStore: SettingsStore = SettingsStore(appContext)
    val settlementManager: PaydaySettlementManager = PaydaySettlementManager(ledgerDao, settingsStore)
}

class SalaryFlowApp : Application() {

    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        createNotificationChannels()
        PaydaySettlementScheduler.ensureScheduled(this)
    }

    private fun createNotificationChannels() {
        val manager = getSystemService<NotificationManager>() ?: return

        val liveChannel = NotificationChannel(
            LIVE_CHANNEL_ID,
            getString(R.string.notification_channel_live),
            NotificationManager.IMPORTANCE_LOW,
        ).apply {
            description = getString(R.string.notification_channel_live_desc)
            setShowBadge(false)
            enableVibration(false)
        }

        val settlementChannel = NotificationChannel(
            SETTLEMENT_CHANNEL_ID,
            getString(R.string.notification_channel_settlement),
            NotificationManager.IMPORTANCE_DEFAULT,
        ).apply {
            description = getString(R.string.notification_channel_settlement_desc)
        }

        manager.createNotificationChannel(liveChannel)
        manager.createNotificationChannel(settlementChannel)
    }

    companion object {
        const val LIVE_CHANNEL_ID = "salary_flow_live"
        const val SETTLEMENT_CHANNEL_ID = "salary_flow_settlement"
        const val LIVE_NOTIFICATION_ID = 1001
        const val SETTLEMENT_NOTIFICATION_ID = 1002
    }
}

val Context.appContainer: AppContainer
    get() = (applicationContext as SalaryFlowApp).container
