package com.salaryflow.app.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.salaryflow.app.core.SalaryConfig
import com.salaryflow.app.core.WorkSchedule
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.time.DayOfWeek
import java.time.LocalTime

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "salary_flow_settings")

data class AppSettings(
    val monthlySalaryCents: Long = 1_000_000L,
    val paidDaysPerMonth: Double = 21.75,
    val payday: Int = 15,
    val workStartMinute: Int = 9 * 60,
    val workEndMinute: Int = 18 * 60,
    val lunchStartMinute: Int = 12 * 60,
    val lunchEndMinute: Int = 13 * 60,
    val lunchPaid: Boolean = false,
    /** bit0 = 周一 … bit6 = 周日。 */
    val workDaysMask: Int = 0b0011111,
    val savingsRatePercent: Int = 20,
    val hideAmounts: Boolean = false,
    /** 通知栏刷新间隔，秒。 */
    val tickSeconds: Int = 1,
    val serviceEnabled: Boolean = false,
    val counterPaused: Boolean = false,
    val autoSettleEnabled: Boolean = true,
    val autoIncomeEnabled: Boolean = true,
    val bootStartEnabled: Boolean = true,
    val goalName: String = "第一笔存款",
    val goalAmountCents: Long = 1_000_000L,
    val lastSettledPeriodEnd: String? = null,
    val pendingCelebrationKey: String? = null,
    val lastSettlementAmountCents: Long = 0L,
    val themeMode: String = "system",
    val onboarded: Boolean = false,
) {
    val savingsRate: Double get() = savingsRatePercent.coerceIn(0, 100) / 100.0

    fun toSalaryConfig(): SalaryConfig = SalaryConfig(
        monthlySalaryCents = monthlySalaryCents,
        paidDaysPerMonth = paidDaysPerMonth,
        payday = payday,
        savingsRatePercent = savingsRatePercent,
        schedule = WorkSchedule(
            workDays = maskToWorkDays(workDaysMask),
            startTime = minuteToTime(workStartMinute),
            endTime = minuteToTime(workEndMinute),
            lunchStart = minuteToTime(lunchStartMinute),
            lunchEnd = minuteToTime(lunchEndMinute),
            lunchPaid = lunchPaid,
        ),
    )

    companion object {
        fun maskToWorkDays(mask: Int): Set<DayOfWeek> =
            DayOfWeek.entries.filter { day -> mask and (1 shl (day.value - 1)) != 0 }.toSet()

        fun workDaysToMask(days: Set<DayOfWeek>): Int =
            days.fold(0) { acc, day -> acc or (1 shl (day.value - 1)) }

        fun minuteToTime(minute: Int): LocalTime =
            LocalTime.of((minute / 60).coerceIn(0, 23), (minute % 60).coerceIn(0, 59))

        fun timeToMinute(time: LocalTime): Int = time.hour * 60 + time.minute
    }
}

class SettingsStore(private val context: Context) {

    val settings: Flow<AppSettings> = context.dataStore.data.map { it.toAppSettings() }

    suspend fun current(): AppSettings = settings.first()

    suspend fun update(transform: (AppSettings) -> AppSettings) {
        context.dataStore.edit { preferences ->
            val next = transform(preferences.toAppSettings())
            preferences.writeFrom(next)
        }
    }

    suspend fun setLastSettledPeriodEnd(periodEnd: String?) = update { it.copy(lastSettledPeriodEnd = periodEnd) }

    suspend fun requestCelebration(key: String) = update { it.copy(pendingCelebrationKey = key) }

    suspend fun clearCelebration() = update { it.copy(pendingCelebrationKey = null) }

    suspend fun setServiceEnabled(enabled: Boolean) = update {
        it.copy(serviceEnabled = enabled, counterPaused = if (enabled) it.counterPaused else false)
    }
}

private object Keys {
    val monthlySalaryCents = longPreferencesKey("monthly_salary_cents")
    val paidDaysPerMonth = doublePreferencesKey("paid_days_per_month")
    val payday = intPreferencesKey("payday")
    val workStartMinute = intPreferencesKey("work_start_minute")
    val workEndMinute = intPreferencesKey("work_end_minute")
    val lunchStartMinute = intPreferencesKey("lunch_start_minute")
    val lunchEndMinute = intPreferencesKey("lunch_end_minute")
    val lunchPaid = booleanPreferencesKey("lunch_paid")
    val workDaysMask = intPreferencesKey("work_days_mask")
    val savingsRatePercent = intPreferencesKey("savings_rate_percent")
    val hideAmounts = booleanPreferencesKey("hide_amounts")
    val tickSeconds = intPreferencesKey("tick_seconds")
    val serviceEnabled = booleanPreferencesKey("service_enabled")
    val counterPaused = booleanPreferencesKey("counter_paused")
    val autoSettleEnabled = booleanPreferencesKey("auto_settle_enabled")
    val autoIncomeEnabled = booleanPreferencesKey("auto_income_enabled")
    val bootStartEnabled = booleanPreferencesKey("boot_start_enabled")
    val goalName = stringPreferencesKey("goal_name")
    val goalAmountCents = longPreferencesKey("goal_amount_cents")
    val lastSettledPeriodEnd = stringPreferencesKey("last_settled_period_end")
    val pendingCelebrationKey = stringPreferencesKey("pending_celebration_key")
    val lastSettlementAmountCents = longPreferencesKey("last_settlement_amount_cents")
    val themeMode = stringPreferencesKey("theme_mode")
    val onboarded = booleanPreferencesKey("onboarded")
}

private fun Preferences.toAppSettings(): AppSettings {
    val defaults = AppSettings()
    return AppSettings(
        monthlySalaryCents = this[Keys.monthlySalaryCents] ?: defaults.monthlySalaryCents,
        paidDaysPerMonth = this[Keys.paidDaysPerMonth] ?: defaults.paidDaysPerMonth,
        payday = this[Keys.payday] ?: defaults.payday,
        workStartMinute = this[Keys.workStartMinute] ?: defaults.workStartMinute,
        workEndMinute = this[Keys.workEndMinute] ?: defaults.workEndMinute,
        lunchStartMinute = this[Keys.lunchStartMinute] ?: defaults.lunchStartMinute,
        lunchEndMinute = this[Keys.lunchEndMinute] ?: defaults.lunchEndMinute,
        lunchPaid = this[Keys.lunchPaid] ?: defaults.lunchPaid,
        workDaysMask = this[Keys.workDaysMask] ?: defaults.workDaysMask,
        savingsRatePercent = this[Keys.savingsRatePercent] ?: defaults.savingsRatePercent,
        hideAmounts = this[Keys.hideAmounts] ?: defaults.hideAmounts,
        tickSeconds = this[Keys.tickSeconds] ?: defaults.tickSeconds,
        serviceEnabled = this[Keys.serviceEnabled] ?: defaults.serviceEnabled,
        counterPaused = this[Keys.counterPaused] ?: defaults.counterPaused,
        autoSettleEnabled = this[Keys.autoSettleEnabled] ?: defaults.autoSettleEnabled,
        autoIncomeEnabled = this[Keys.autoIncomeEnabled] ?: defaults.autoIncomeEnabled,
        bootStartEnabled = this[Keys.bootStartEnabled] ?: defaults.bootStartEnabled,
        goalName = this[Keys.goalName] ?: defaults.goalName,
        goalAmountCents = this[Keys.goalAmountCents] ?: defaults.goalAmountCents,
        lastSettledPeriodEnd = this[Keys.lastSettledPeriodEnd],
        pendingCelebrationKey = this[Keys.pendingCelebrationKey],
        lastSettlementAmountCents = this[Keys.lastSettlementAmountCents] ?: 0L,
        themeMode = this[Keys.themeMode] ?: defaults.themeMode,
        onboarded = this[Keys.onboarded] ?: defaults.onboarded,
    )
}

private fun MutablePreferences.writeFrom(settings: AppSettings) {
    this[Keys.monthlySalaryCents] = settings.monthlySalaryCents
    this[Keys.paidDaysPerMonth] = settings.paidDaysPerMonth
    this[Keys.payday] = settings.payday
    this[Keys.workStartMinute] = settings.workStartMinute
    this[Keys.workEndMinute] = settings.workEndMinute
    this[Keys.lunchStartMinute] = settings.lunchStartMinute
    this[Keys.lunchEndMinute] = settings.lunchEndMinute
    this[Keys.lunchPaid] = settings.lunchPaid
    this[Keys.workDaysMask] = settings.workDaysMask
    this[Keys.savingsRatePercent] = settings.savingsRatePercent
    this[Keys.hideAmounts] = settings.hideAmounts
    this[Keys.tickSeconds] = settings.tickSeconds
    this[Keys.serviceEnabled] = settings.serviceEnabled
    this[Keys.counterPaused] = settings.counterPaused
    this[Keys.autoSettleEnabled] = settings.autoSettleEnabled
    this[Keys.autoIncomeEnabled] = settings.autoIncomeEnabled
    this[Keys.bootStartEnabled] = settings.bootStartEnabled
    this[Keys.goalName] = settings.goalName
    this[Keys.goalAmountCents] = settings.goalAmountCents
    settings.lastSettledPeriodEnd?.let { this[Keys.lastSettledPeriodEnd] = it } ?: remove(Keys.lastSettledPeriodEnd)
    settings.pendingCelebrationKey?.let { this[Keys.pendingCelebrationKey] = it } ?: remove(Keys.pendingCelebrationKey)
    this[Keys.lastSettlementAmountCents] = settings.lastSettlementAmountCents
    this[Keys.themeMode] = settings.themeMode
    this[Keys.onboarded] = settings.onboarded
}
