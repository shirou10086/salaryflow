package com.salaryflow.app.ui.screens

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.salaryflow.app.BuildConfig
import com.salaryflow.app.R
import com.salaryflow.app.core.Money
import com.salaryflow.app.core.TimeText
import com.salaryflow.app.data.AppSettings
import com.salaryflow.app.ui.SalaryFlowViewModel
import com.salaryflow.app.ui.UpdateUiState
import com.salaryflow.app.ui.components.PillSelect
import com.salaryflow.app.ui.components.SectionCard
import com.salaryflow.app.ui.components.SectionHeader
import java.time.DayOfWeek
import java.time.LocalTime

@Composable
fun SettingsScreen(
    viewModel: SalaryFlowViewModel,
    onCalibrateBalance: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val savingsCents by viewModel.savingsCents.collectAsStateWithLifecycle()
    val updateState by viewModel.updateState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { granted -> if (granted) viewModel.startTickerService() },
    )

    var showSalaryDialog by remember { mutableStateOf(false) }
    var showPaidDaysDialog by remember { mutableStateOf(false) }
    var showPaydayDialog by remember { mutableStateOf(false) }
    var showGoalNameDialog by remember { mutableStateOf(false) }
    var showGoalAmountDialog by remember { mutableStateOf(false) }
    var showClearDialog by remember { mutableStateOf(false) }
    var editingTime by remember { mutableStateOf<TimeField?>(null) }
    val defaultGoalName = stringResource(R.string.default_goal_name)
    val goalName = settings.goalName.ifBlank { defaultGoalName }

    LazyColumn(
        modifier = modifier.fillMaxWidth(),
        contentPadding = PaddingValues(start = 18.dp, end = 18.dp, top = 10.dp, bottom = 120.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item {
            Column {
                Text(stringResource(R.string.settings_title), style = MaterialTheme.typography.headlineSmall)
                Text(
                    stringResource(R.string.settings_subtitle),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        item {
            SectionCard {
                SectionHeader(
                    title = stringResource(R.string.settings_salary_section),
                    subtitle = stringResource(R.string.settings_salary_section_subtitle),
                )
                Spacer(Modifier.height(6.dp))
                SettingRow(
                    title = stringResource(R.string.settings_monthly_salary),
                    value = "¥${Money.centsToYuan(settings.monthlySalaryCents)}",
                    onClick = { showSalaryDialog = true },
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                SettingRow(
                    title = stringResource(R.string.settings_paid_days),
                    subtitle = stringResource(R.string.settings_paid_days_hint),
                    value = "${settings.paidDaysPerMonth}",
                    onClick = { showPaidDaysDialog = true },
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                SettingRow(
                    title = stringResource(R.string.settings_work_start),
                    value = TimeText.clock(AppSettings.minuteToTime(settings.workStartMinute)),
                    onClick = { editingTime = TimeField.WORK_START },
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                SettingRow(
                    title = stringResource(R.string.settings_work_end),
                    value = TimeText.clock(AppSettings.minuteToTime(settings.workEndMinute)),
                    onClick = { editingTime = TimeField.WORK_END },
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                SettingRow(
                    title = stringResource(R.string.settings_lunch),
                    subtitle = stringResource(R.string.settings_lunch_hint),
                    value = "${TimeText.clock(AppSettings.minuteToTime(settings.lunchStartMinute))} - " +
                        TimeText.clock(AppSettings.minuteToTime(settings.lunchEndMinute)),
                    onClick = { editingTime = TimeField.LUNCH_START },
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                SwitchRow(
                    title = stringResource(R.string.settings_lunch_paid),
                    subtitle = stringResource(R.string.settings_lunch_paid_hint),
                    checked = settings.lunchPaid,
                    onChange = { value -> viewModel.updateSettings { it.copy(lunchPaid = value) } },
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                SwitchRow(
                    title = stringResource(R.string.settings_dinner),
                    subtitle = stringResource(R.string.settings_dinner_hint),
                    checked = settings.dinnerEnabled,
                    onChange = { value -> viewModel.updateSettings { it.copy(dinnerEnabled = value) } },
                )
                if (settings.dinnerEnabled) {
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    SettingRow(
                        title = stringResource(R.string.settings_dinner_time),
                        subtitle = stringResource(R.string.settings_dinner_scope_hint),
                        value = "${TimeText.clock(AppSettings.minuteToTime(settings.dinnerStartMinute))} - " +
                            TimeText.clock(AppSettings.minuteToTime(settings.dinnerEndMinute)),
                        onClick = { editingTime = TimeField.DINNER_START },
                    )
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    SwitchRow(
                        title = stringResource(R.string.settings_dinner_paid),
                        subtitle = stringResource(R.string.settings_dinner_paid_hint),
                        checked = settings.dinnerPaid,
                        onChange = { value ->
                            viewModel.updateSettings { it.copy(dinnerPaid = value) }
                        },
                    )
                }
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                Text(
                    stringResource(R.string.settings_work_days),
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(top = 12.dp, bottom = 6.dp),
                )
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    DayOfWeek.entries.forEach { day ->
                        val mask = 1 shl (day.value - 1)
                        PillSelect(
                            text = TimeText.weekdayForPill(day),
                            selected = settings.workDaysMask and mask != 0,
                            onClick = {
                                viewModel.updateSettings {
                                    it.copy(workDaysMask = it.workDaysMask xor mask)
                                }
                            },
                        )
                    }
                }
            }
        }

        item {
            SectionCard {
                SectionHeader(
                    title = stringResource(R.string.settings_payday_section),
                    subtitle = stringResource(R.string.settings_payday_section_subtitle),
                )
                Spacer(Modifier.height(8.dp))
                SettingRow(
                    title = stringResource(R.string.settings_payday),
                    subtitle = stringResource(R.string.settings_payday_hint),
                    value = stringResource(R.string.settings_payday_value, settings.payday),
                    onClick = { showPaydayDialog = true },
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                SwitchRow(
                    title = stringResource(R.string.settings_auto_settle),
                    subtitle = stringResource(R.string.settings_auto_settle_hint),
                    checked = settings.autoSettleEnabled,
                    onChange = { value -> viewModel.updateSettings { it.copy(autoSettleEnabled = value) } },
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                SwitchRow(
                    title = stringResource(R.string.settings_auto_income),
                    subtitle = stringResource(R.string.settings_auto_income_hint),
                    checked = settings.autoIncomeEnabled,
                    onChange = { value -> viewModel.updateSettings { it.copy(autoIncomeEnabled = value) } },
                )
                Spacer(Modifier.height(12.dp))
                OutlinedButton(
                    onClick = viewModel::requestDemoCelebration,
                    modifier = Modifier.fillMaxWidth(),
                ) { Text(stringResource(R.string.settings_preview_celebration)) }
            }
        }

        item {
            SectionCard {
                SectionHeader(
                    title = stringResource(R.string.settings_savings_section),
                    subtitle = stringResource(R.string.settings_savings_section_subtitle),
                )
                Spacer(Modifier.height(10.dp))
                SettingRow(
                    title = stringResource(R.string.current_savings),
                    subtitle = stringResource(R.string.current_savings_subtitle),
                    value = if (settings.hideAmounts) {
                        "¥••••"
                    } else {
                        "¥${Money.centsToYuan(savingsCents)}"
                    },
                    onClick = onCalibrateBalance,
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                SwitchRow(
                    title = stringResource(R.string.settings_balance_reminder),
                    subtitle = stringResource(R.string.settings_balance_reminder_hint),
                    checked = settings.balanceReminderEnabled,
                    onChange = viewModel::setBalanceReminderEnabled,
                )
                Spacer(Modifier.height(12.dp))
                OutlinedButton(
                    onClick = onCalibrateBalance,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(stringResource(R.string.settings_update_savings))
                }
            }
        }

        item {
            SectionCard {
                SectionHeader(
                    title = stringResource(R.string.settings_goal_section),
                    subtitle = stringResource(R.string.settings_goal_section_subtitle),
                )
                Spacer(Modifier.height(6.dp))
                SettingRow(
                    title = stringResource(R.string.settings_goal_name),
                    value = goalName,
                    onClick = { showGoalNameDialog = true },
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                SettingRow(
                    title = stringResource(R.string.settings_goal_amount),
                    value = "¥${Money.centsToYuan(settings.goalAmountCents)}",
                    onClick = { showGoalAmountDialog = true },
                )
            }
        }

        item {
            SectionCard {
                SectionHeader(
                    title = stringResource(R.string.settings_notification_section),
                    subtitle = stringResource(R.string.settings_notification_section_subtitle),
                )
                Spacer(Modifier.height(6.dp))
                SwitchRow(
                    title = stringResource(R.string.settings_show_notification),
                    subtitle = stringResource(R.string.settings_show_notification_hint),
                    checked = settings.serviceEnabled,
                    onChange = { value ->
                        if (!value) {
                            viewModel.stopTickerService()
                        } else {
                            val needsPermission = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                                ContextCompat.checkSelfPermission(
                                    context,
                                    Manifest.permission.POST_NOTIFICATIONS,
                                ) != PackageManager.PERMISSION_GRANTED
                            if (needsPermission) {
                                permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                            } else {
                                viewModel.startTickerService()
                            }
                        }
                    },
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                SwitchRow(
                    title = stringResource(R.string.settings_hide_amounts),
                    subtitle = stringResource(R.string.settings_hide_amounts_hint),
                    checked = settings.hideAmounts,
                    onChange = { value -> viewModel.updateSettings { it.copy(hideAmounts = value) } },
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                SwitchRow(
                    title = stringResource(R.string.settings_hide_on_lockscreen),
                    subtitle = stringResource(R.string.settings_hide_on_lockscreen_hint),
                    checked = settings.hideOnLockscreen,
                    onChange = { value ->
                        viewModel.updateSettings { it.copy(hideOnLockscreen = value) }
                    },
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                SwitchRow(
                    title = stringResource(R.string.settings_boot_start),
                    subtitle = stringResource(R.string.settings_boot_start_hint),
                    checked = settings.bootStartEnabled,
                    onChange = { value -> viewModel.updateSettings { it.copy(bootStartEnabled = value) } },
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                Text(
                    stringResource(R.string.settings_refresh_rate),
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(top = 12.dp, bottom = 6.dp),
                )
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(
                        1 to R.string.settings_refresh_one_second,
                        5 to R.string.settings_refresh_five_seconds,
                        10 to R.string.settings_refresh_ten_seconds,
                        60 to R.string.settings_refresh_one_minute,
                    ).forEach { (seconds, labelRes) ->
                        PillSelect(
                            text = stringResource(labelRes),
                            selected = settings.tickSeconds == seconds,
                            onClick = { viewModel.updateSettings { it.copy(tickSeconds = seconds) } },
                        )
                    }
                }
                Spacer(Modifier.height(8.dp))
                Text(
                    stringResource(R.string.settings_refresh_hint),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        item {
            SectionCard {
                SectionHeader(
                    title = stringResource(R.string.settings_appearance),
                    subtitle = stringResource(R.string.settings_appearance_subtitle),
                )
                Spacer(Modifier.height(8.dp))
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(
                        "system" to R.string.settings_theme_system,
                        "light" to R.string.settings_theme_light,
                        "dark" to R.string.settings_theme_dark,
                    ).forEach { (key, labelRes) ->
                        PillSelect(
                            text = stringResource(labelRes),
                            selected = settings.themeMode == key,
                            onClick = { viewModel.updateSettings { it.copy(themeMode = key) } },
                        )
                    }
                }
            }
        }

        if (BuildConfig.SELF_UPDATE_ENABLED) {
            item {
                SectionCard {
                    SectionHeader(
                        title = stringResource(R.string.settings_update_section),
                        subtitle = stringResource(R.string.settings_update_section_subtitle),
                    )
                    Spacer(Modifier.height(6.dp))
                    SwitchRow(
                        title = stringResource(R.string.settings_auto_update),
                        subtitle = stringResource(R.string.settings_auto_update_hint),
                        checked = settings.autoUpdateEnabled,
                        onChange = viewModel::setAutoUpdateEnabled,
                    )
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    OutlinedButton(
                        onClick = viewModel::checkForUpdates,
                        enabled = updateState != UpdateUiState.Checking &&
                            updateState !is UpdateUiState.Downloading,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(
                            stringResource(
                                if (updateState == UpdateUiState.Checking) {
                                    R.string.settings_checking_update
                                } else {
                                    R.string.settings_check_update
                                },
                            ),
                        )
                    }
                    Spacer(Modifier.height(8.dp))
                    Text(
                        stringResource(R.string.settings_update_install_hint),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }

        item {
            SectionCard {
                SectionHeader(
                    title = stringResource(R.string.settings_data),
                    subtitle = stringResource(R.string.settings_data_subtitle),
                )
                Spacer(Modifier.height(10.dp))
                OutlinedButton(
                    onClick = { showClearDialog = true },
                    modifier = Modifier.fillMaxWidth(),
                ) { Text(stringResource(R.string.settings_clear_data)) }
                Spacer(Modifier.height(8.dp))
                Text(
                    stringResource(R.string.settings_clear_data_hint),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        item {
            SectionCard {
                SectionHeader(
                    title = stringResource(R.string.settings_about),
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    stringResource(R.string.settings_version, BuildConfig.VERSION_NAME),
                    style = MaterialTheme.typography.bodyMedium,
                )
                HorizontalDivider(
                    color = MaterialTheme.colorScheme.outlineVariant,
                    modifier = Modifier.padding(top = 12.dp),
                )
                SettingRow(
                    title = stringResource(R.string.settings_language),
                    subtitle = stringResource(R.string.settings_language_hint),
                    value = stringResource(R.string.settings_language_value),
                    onClick = {
                        val intent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            Intent(
                                Settings.ACTION_APP_LOCALE_SETTINGS,
                                Uri.parse("package:${context.packageName}"),
                            )
                        } else {
                            Intent(Settings.ACTION_LOCALE_SETTINGS)
                        }
                        runCatching { context.startActivity(intent) }
                    },
                )
            }
        }
    }

    if (showSalaryDialog) {
        NumberInputDialog(
            title = stringResource(R.string.settings_salary_dialog_title),
            initial = (settings.monthlySalaryCents / 100.0).toString(),
            onDismiss = { showSalaryDialog = false },
            onConfirm = { value ->
                Money.yuanToCents(value)?.let { cents ->
                    viewModel.updateSettings { it.copy(monthlySalaryCents = cents) }
                }
                showSalaryDialog = false
            },
        )
    }

    if (showPaidDaysDialog) {
        NumberInputDialog(
            title = stringResource(R.string.settings_paid_days_dialog_title),
            initial = settings.paidDaysPerMonth.toString(),
            onDismiss = { showPaidDaysDialog = false },
            onConfirm = { value ->
                value.toDoubleOrNull()?.takeIf { it > 0 }?.let { days ->
                    viewModel.updateSettings { it.copy(paidDaysPerMonth = days) }
                }
                showPaidDaysDialog = false
            },
        )
    }

    if (showPaydayDialog) {
        NumberInputDialog(
            title = stringResource(R.string.settings_payday_dialog_title),
            initial = settings.payday.toString(),
            onDismiss = { showPaydayDialog = false },
            onConfirm = { value ->
                value.toIntOrNull()?.takeIf { it in 1..31 }?.let { day ->
                    viewModel.updatePayday(day)
                }
                showPaydayDialog = false
            },
        )
    }

    if (showGoalNameDialog) {
        TextInputDialog(
            title = stringResource(R.string.settings_goal_name_dialog_title),
            initial = goalName,
            onDismiss = { showGoalNameDialog = false },
            onConfirm = { value ->
                viewModel.updateSettings { it.copy(goalName = value.trim()) }
                showGoalNameDialog = false
            },
        )
    }

    if (showGoalAmountDialog) {
        NumberInputDialog(
            title = stringResource(R.string.settings_goal_amount_dialog_title),
            initial = (settings.goalAmountCents / 100.0).toString(),
            onDismiss = { showGoalAmountDialog = false },
            onConfirm = { value ->
                Money.yuanToCents(value)?.let { cents ->
                    viewModel.updateSettings { it.copy(goalAmountCents = cents) }
                }
                showGoalAmountDialog = false
            },
        )
    }

    editingTime?.let { field ->
        TimeInputDialog(
            title = stringResource(field.titleRes),
            initial = field.current(settings),
            onDismiss = { editingTime = null },
            onConfirm = { time ->
                val minute = AppSettings.timeToMinute(time)
                viewModel.updateSettings { current ->
                    when (field) {
                        TimeField.WORK_START -> current.copy(workStartMinute = minute)
                        TimeField.WORK_END -> current.copy(workEndMinute = minute)
                        TimeField.LUNCH_START -> current.copy(lunchStartMinute = minute)
                        TimeField.LUNCH_END -> current.copy(lunchEndMinute = minute)
                        TimeField.DINNER_START -> current.copy(dinnerStartMinute = minute)
                        TimeField.DINNER_END -> current.copy(dinnerEndMinute = minute)
                    }
                }
                editingTime = null
            },
            nextStepHint = when (field) {
                TimeField.LUNCH_START -> stringResource(R.string.time_lunch_next_hint)
                TimeField.DINNER_START -> stringResource(R.string.time_dinner_next_hint)
                else -> null
            },
            onSecondStep = {
                editingTime = when (field) {
                    TimeField.LUNCH_START -> TimeField.LUNCH_END
                    TimeField.DINNER_START -> TimeField.DINNER_END
                    else -> null
                }
            },
        )
    }

    if (showClearDialog) {
        AlertDialog(
            onDismissRequest = { showClearDialog = false },
            title = { Text(stringResource(R.string.settings_clear_dialog_title)) },
            text = { Text(stringResource(R.string.settings_clear_dialog_text)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.clearAllData()
                        showClearDialog = false
                    },
                ) { Text(stringResource(R.string.settings_clear_confirm)) }
            },
            dismissButton = {
                TextButton(onClick = { showClearDialog = false }) {
                    Text(stringResource(R.string.action_cancel))
                }
            },
        )
    }
}

private enum class TimeField(val titleRes: Int) {
    WORK_START(R.string.time_work_start),
    WORK_END(R.string.time_work_end),
    LUNCH_START(R.string.time_lunch_start),
    LUNCH_END(R.string.time_lunch_end),
    DINNER_START(R.string.time_dinner_start),
    DINNER_END(R.string.time_dinner_end);

    fun current(settings: AppSettings): LocalTime = when (this) {
        WORK_START -> AppSettings.minuteToTime(settings.workStartMinute)
        WORK_END -> AppSettings.minuteToTime(settings.workEndMinute)
        LUNCH_START -> AppSettings.minuteToTime(settings.lunchStartMinute)
        LUNCH_END -> AppSettings.minuteToTime(settings.lunchEndMinute)
        DINNER_START -> AppSettings.minuteToTime(settings.dinnerStartMinute)
        DINNER_END -> AppSettings.minuteToTime(settings.dinnerEndMinute)
    }
}

@Composable
private fun SettingRow(
    title: String,
    value: String,
    subtitle: String? = null,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            if (subtitle != null) {
                Text(
                    subtitle,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        Text(
            value,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier
                .padding(start = 12.dp)
                .clickableRow(onClick),
        )
    }
}

@Composable
private fun SwitchRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Text(
                subtitle,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Switch(
            checked = checked,
            onCheckedChange = onChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                checkedTrackColor = MaterialTheme.colorScheme.primary,
                checkedBorderColor = MaterialTheme.colorScheme.primary,
                uncheckedThumbColor = MaterialTheme.colorScheme.outline,
                uncheckedTrackColor = MaterialTheme.colorScheme.surfaceVariant,
                uncheckedBorderColor = MaterialTheme.colorScheme.outline,
            ),
        )
    }
}

private fun Modifier.clickableRow(onClick: () -> Unit): Modifier =
    this.clickable(onClick = onClick)

@Composable
private fun NumberInputDialog(
    title: String,
    initial: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit,
) {
    var value by remember { mutableStateOf(initial) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            OutlinedTextField(
                value = value,
                onValueChange = { value = it.filter { char -> char.isDigit() || char == '.' } },
                singleLine = true,
                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                    keyboardType = KeyboardType.Decimal,
                ),
                modifier = Modifier.fillMaxWidth(),
            )
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(value) }) {
                Text(stringResource(R.string.action_save))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) }
        },
    )
}

@Composable
private fun TextInputDialog(
    title: String,
    initial: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit,
) {
    var value by remember { mutableStateOf(initial) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            OutlinedTextField(
                value = value,
                onValueChange = { value = it },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(value) }) {
                Text(stringResource(R.string.action_save))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) }
        },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TimeInputDialog(
    title: String,
    initial: LocalTime,
    onDismiss: () -> Unit,
    onConfirm: (LocalTime) -> Unit,
    nextStepHint: String?,
    onSecondStep: () -> Unit = {},
) {
    val state = rememberTimePickerState(
        initialHour = initial.hour,
        initialMinute = initial.minute,
        is24Hour = true,
    )
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column {
                TimePicker(state = state)
                if (nextStepHint != null) {
                    Text(
                        nextStepHint,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onConfirm(LocalTime.of(state.hour, state.minute))
                    onSecondStep()
                },
            ) { Text(stringResource(R.string.action_save)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) }
        },
    )
}
