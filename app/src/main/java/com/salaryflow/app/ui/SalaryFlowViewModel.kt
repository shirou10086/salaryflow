package com.salaryflow.app.ui

import android.app.Application
import android.content.Intent
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.salaryflow.app.BuildConfig
import com.salaryflow.app.appContainer
import com.salaryflow.app.core.SalaryClock
import com.salaryflow.app.core.toYearMonth
import com.salaryflow.app.data.AppSettings
import com.salaryflow.app.data.CategoryTotal
import com.salaryflow.app.data.TxType
import com.salaryflow.app.data.LedgerRepository
import com.salaryflow.app.data.SettingsStore
import com.salaryflow.app.data.TransactionEntity
import com.salaryflow.app.data.balanceDeltaCents
import com.salaryflow.app.service.SalaryTickerService
import com.salaryflow.app.ui.screens.OnboardingResult
import com.salaryflow.app.work.BalanceReminderScheduler
import com.salaryflow.app.work.PaydaySettlementScheduler
import com.salaryflow.app.update.UpdateCheckResult
import com.salaryflow.app.update.UpdateManager
import com.salaryflow.app.update.UpdateRelease
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.File
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.YearMonth

data class MonthStats(
    val month: YearMonth = YearMonth.now(),
    val expenseCents: Long = 0,
    val incomeCents: Long = 0,
    val savingCents: Long = 0,
    val dailyExpenses: List<Long> = emptyList(),
    val categoryTotals: List<CategoryTotal> = emptyList(),
)

data class MonthPoint(
    val month: YearMonth,
    val expenseCents: Long,
    val incomeCents: Long,
    val savingCents: Long,
)

sealed interface UpdateUiState {
    data object Idle : UpdateUiState
    data object Checking : UpdateUiState
    data class Available(val release: UpdateRelease) : UpdateUiState
    data class Downloading(val release: UpdateRelease, val progress: Int) : UpdateUiState
    data class ReadyToInstall(val release: UpdateRelease, val file: File) : UpdateUiState
    data object UpToDate : UpdateUiState
    data class Failed(val reason: UpdateFailure, val release: UpdateRelease? = null) : UpdateUiState
}

enum class UpdateFailure {
    CHECK,
    DOWNLOAD,
    INSTALL,
}

@OptIn(ExperimentalCoroutinesApi::class)
class SalaryFlowViewModel(application: Application) : AndroidViewModel(application) {

    private val container = application.appContainer
    private val settingsStore: SettingsStore = container.settingsStore
    private val ledger: LedgerRepository = container.ledgerRepository
    private val updateManager: UpdateManager = container.updateManager

    private val _updateState = MutableStateFlow<UpdateUiState>(UpdateUiState.Idle)
    val updateState: StateFlow<UpdateUiState> = _updateState.asStateFlow()
    private var autoUpdateCheckRequested = false
    private var updateCheckJob: Job? = null
    private var updateDownloadJob: Job? = null

    val settings: StateFlow<AppSettings> = settingsStore.settings
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AppSettings())

    val settingsLoaded: StateFlow<Boolean> = settingsStore.settings
        .map { true }
        .stateIn(viewModelScope, SharingStarted.Eagerly, false)

    val savingsCents: StateFlow<Long> = ledger.observeSavingsCents()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0L)

    private val selectedMonth = MutableStateFlow(YearMonth.now())
    val currentMonth: StateFlow<YearMonth> = selectedMonth

    val transactions: StateFlow<List<TransactionEntity>> = ledger.observeAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /** 最近 6 个月的收支，用于统计图。 */
    val recentMonths: StateFlow<List<MonthPoint>> = transactions
        .map { list ->
            val thisMonth = YearMonth.now()
            (5 downTo 0).map { offset ->
                val month = thisMonth.minusMonths(offset.toLong())
                val inMonth = list.filter { it.occurredAt.toYearMonth() == month }
                MonthPoint(
                    month = month,
                    expenseCents = inMonth.filter { it.type == TxType.EXPENSE }.sumOf { it.amountCents },
                    incomeCents = inMonth.filter { it.type == TxType.INCOME }.sumOf { it.amountCents },
                    savingCents = inMonth.filter { it.type == TxType.SAVING_IN }.sumOf { it.amountCents },
                )
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /** 当前选中月份内，攒钱罐余额的逐日变化。 */
    val savingsSeries: StateFlow<List<Long>> = selectedMonth
        .flatMapLatest { month ->
            transactions.map { list ->
                val days = month.lengthOfMonth()
                var running = list
                    .filter { it.occurredAt.toYearMonth() < month }
                    .sumOf { it.balanceDeltaCents() }
                (1..days).map { day ->
                    val zone = java.time.ZoneId.systemDefault()
                    val dayStart = month.atDay(day).atStartOfDay(zone).toInstant().toEpochMilli()
                    val dayEnd = month.atDay(day).plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli()
                    running += list
                        .filter { it.occurredAt >= dayStart && it.occurredAt < dayEnd }
                        .sumOf { it.balanceDeltaCents() }
                    running
                }
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val monthTransactions: StateFlow<List<TransactionEntity>> = selectedMonth
        .flatMapLatest { ledger.observeMonth(it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val monthExpenseCents: StateFlow<Long> = selectedMonth
        .flatMapLatest { ledger.observeExpenseSum(it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0L)

    val monthIncomeCents: StateFlow<Long> = selectedMonth
        .flatMapLatest { ledger.observeIncomeSum(it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0L)

    val monthSavingCents: StateFlow<Long> = selectedMonth
        .flatMapLatest { ledger.observeSavingInSum(it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0L)

    val monthCategoryTotals: StateFlow<List<CategoryTotal>> = selectedMonth
        .flatMapLatest { ledger.observeExpenseByCategory(it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val todayExpenseCents: StateFlow<Long> = flow {
        while (true) {
            emit(LocalDate.now())
            delay(30_000)
        }
    }.flatMapLatest { date -> ledger.observeDaySum(TxType.EXPENSE, date) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0L)

    /** 1 秒一次的心跳，仅首页订阅。 */
    val nowTicker: Flow<LocalDateTime> = flow {
        while (true) {
            emit(LocalDateTime.now())
            delay(1_000)
        }
    }

    fun selectMonth(month: YearMonth) {
        selectedMonth.value = month
    }

    fun shiftMonth(delta: Long) {
        selectedMonth.value = selectedMonth.value.plusMonths(delta)
    }

    fun salaryClock() = SalaryClock(settings.value.toSalaryConfig())

    fun updateSettings(transform: (AppSettings) -> AppSettings) {
        viewModelScope.launch { settingsStore.update(transform) }
    }

    fun checkForUpdatesAutomatically() {
        if (!BuildConfig.SELF_UPDATE_ENABLED) return
        if (autoUpdateCheckRequested) return
        autoUpdateCheckRequested = true
        requestUpdateCheck(manual = false)
    }

    fun checkForUpdates() {
        if (!BuildConfig.SELF_UPDATE_ENABLED) return
        requestUpdateCheck(manual = true)
    }

    private fun requestUpdateCheck(manual: Boolean) {
        if (!BuildConfig.SELF_UPDATE_ENABLED) return
        if (updateCheckJob?.isActive == true || updateDownloadJob?.isActive == true) return
        updateCheckJob = viewModelScope.launch {
            val settings = settingsStore.current()
            if (!manual) {
                if (!settings.autoUpdateEnabled) return@launch
                val lastCheck = settings.lastUpdateCheckAt
                if (lastCheck != null && System.currentTimeMillis() - lastCheck < UPDATE_CHECK_INTERVAL_MS) {
                    return@launch
                }
            }
            _updateState.value = UpdateUiState.Checking
            try {
                val result = updateManager.checkForUpdate(BuildConfig.VERSION_NAME)
                settingsStore.update { it.copy(lastUpdateCheckAt = System.currentTimeMillis()) }
                _updateState.value = when (result) {
                    UpdateCheckResult.UpToDate -> if (manual) {
                        UpdateUiState.UpToDate
                    } else {
                        UpdateUiState.Idle
                    }

                    is UpdateCheckResult.Available -> UpdateUiState.Available(result.release)
                }
            } catch (_: Throwable) {
                _updateState.value = if (manual) {
                    UpdateUiState.Failed(UpdateFailure.CHECK)
                } else {
                    UpdateUiState.Idle
                }
            }
        }
    }

    fun downloadUpdate() {
        if (!BuildConfig.SELF_UPDATE_ENABLED) return
        val release = (_updateState.value as? UpdateUiState.Available)?.release ?: return
        if (updateDownloadJob?.isActive == true) return
        updateDownloadJob = viewModelScope.launch {
            _updateState.value = UpdateUiState.Downloading(release, progress = 0)
            try {
                val file = updateManager.download(release) { progress ->
                    _updateState.value = UpdateUiState.Downloading(release, progress)
                }
                _updateState.value = UpdateUiState.ReadyToInstall(release, file)
            } catch (_: Throwable) {
                _updateState.value = UpdateUiState.Failed(UpdateFailure.DOWNLOAD, release)
            }
        }
    }

    fun needsInstallPermission(): Boolean = updateManager.needsInstallPermission()

    fun installPermissionIntent(): Intent = updateManager.installPermissionIntent()

    fun installReadyUpdate() {
        if (!BuildConfig.SELF_UPDATE_ENABLED) return
        val ready = _updateState.value as? UpdateUiState.ReadyToInstall ?: return
        when (updateManager.install(ready.file)) {
            com.salaryflow.app.update.InstallResult.Started -> {
                _updateState.value = UpdateUiState.Idle
            }

            com.salaryflow.app.update.InstallResult.PermissionRequired -> Unit
            com.salaryflow.app.update.InstallResult.Failed -> {
                _updateState.value = UpdateUiState.Failed(UpdateFailure.INSTALL, ready.release)
            }
        }
    }

    fun retryUpdate() {
        val failed = _updateState.value as? UpdateUiState.Failed ?: return
        if (failed.release == null) {
            checkForUpdates()
        } else {
            _updateState.value = UpdateUiState.Available(failed.release)
            downloadUpdate()
        }
    }

    fun dismissUpdateState() {
        if (_updateState.value !is UpdateUiState.Downloading) {
            _updateState.value = UpdateUiState.Idle
        }
    }

    fun setAutoUpdateEnabled(enabled: Boolean) {
        if (!BuildConfig.SELF_UPDATE_ENABLED) return
        viewModelScope.launch {
            settingsStore.update { it.copy(autoUpdateEnabled = enabled) }
            if (enabled) {
                autoUpdateCheckRequested = false
                checkForUpdatesAutomatically()
            }
        }
    }

    fun updatePayday(payday: Int) {
        viewModelScope.launch {
            settingsStore.update { it.copy(payday = payday) }
            PaydaySettlementScheduler.ensureScheduled(getApplication())
            container.settlementManager.settleIfDue()
        }
    }

    fun addExpense(amountCents: Long, category: String, note: String) {
        viewModelScope.launch { ledger.addExpense(amountCents, category, note) }
    }

    fun addIncome(amountCents: Long, category: String, note: String) {
        viewModelScope.launch { ledger.addIncome(amountCents, category, note) }
    }

    fun addSaving(amountCents: Long, note: String, withdraw: Boolean = false) {
        viewModelScope.launch {
            ledger.addSaving(
                amountCents = amountCents,
                note = note,
                type = if (withdraw) TxType.SAVING_OUT else TxType.SAVING_IN,
            )
        }
    }

    fun deleteTransaction(transaction: TransactionEntity) {
        viewModelScope.launch { ledger.delete(transaction) }
    }

    fun startTickerService() {
        val context = getApplication<Application>()
        viewModelScope.launch {
            settingsStore.update { it.copy(serviceEnabled = true) }
            runCatching { SalaryTickerService.start(context) }
        }
    }

    fun stopTickerService() {
        val context = getApplication<Application>()
        SalaryTickerService.stop(context)
        updateSettings { it.copy(serviceEnabled = false) }
    }

    fun togglePaused() {
        updateSettings { it.copy(counterPaused = !it.counterPaused) }
    }

    fun completeOnboarding(result: OnboardingResult) {
        val context = getApplication<Application>()
        viewModelScope.launch {
            settingsStore.update {
                it.copy(
                    monthlySalaryCents = result.monthlySalaryCents,
                    paidDaysPerMonth = result.paidDaysPerMonth,
                    payday = result.payday,
                    workStartMinute = result.workStartMinute,
                    workEndMinute = result.workEndMinute,
                    savingsRatePercent = 0,
                    counterPaused = false,
                    balanceReminderEnabled = result.enableBalanceReminder,
                    lastBalanceReminderMonth = YearMonth.now().toString(),
                    lastSettledPeriodEnd = null,
                )
            }
            ledger.calibrateBalance(result.actualSavingsCents)
            settingsStore.update {
                it.copy(
                    onboarded = true,
                    lastBalanceCalibrationAt = System.currentTimeMillis(),
                )
            }
            PaydaySettlementScheduler.ensureScheduled(context)
            BalanceReminderScheduler.ensureScheduled(context)
            container.settlementManager.settleIfDue()
        }
    }

    fun calibrateBalance(actualCents: Long) {
        viewModelScope.launch {
            ledger.calibrateBalance(actualCents)
            settingsStore.update { it.copy(lastBalanceCalibrationAt = System.currentTimeMillis()) }
        }
    }

    fun setBalanceReminderEnabled(enabled: Boolean) {
        val context = getApplication<Application>()
        viewModelScope.launch {
            settingsStore.update { it.copy(balanceReminderEnabled = enabled) }
            BalanceReminderScheduler.ensureScheduled(context)
        }
    }

    fun requestDemoCelebration() {
        viewModelScope.launch { settingsStore.requestCelebration("demo:${System.currentTimeMillis()}") }
    }

    fun dismissCelebration() {
        viewModelScope.launch { settingsStore.clearCelebration() }
    }

    fun clearAllData() {
        viewModelScope.launch {
            ledger.clearAll()
            settingsStore.update {
                it.copy(
                    lastSettledPeriodEnd = null,
                    pendingCelebrationKey = null,
                    lastSettlementAmountCents = 0L,
                    lastBalanceCalibrationAt = null,
                    lastBalanceReminderMonth = null,
                )
            }
        }
    }

    /** 页面可见时补一次发薪日结算。 */
    fun settleIfDue() {
        viewModelScope.launch { container.settlementManager.settleIfDue() }
    }

    /** 供预览/测试使用：当前时刻的今日已赚。 */
    fun earnedToday(now: LocalDateTime = LocalDateTime.now()): Double =
        salaryClock().earnedToday(now)

    companion object {
        private const val UPDATE_CHECK_INTERVAL_MS = 24L * 60L * 60L * 1000L
    }
}
