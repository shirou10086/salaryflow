package com.salaryflow.app.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.salaryflow.app.BuildConfig
import com.salaryflow.app.core.Money
import com.salaryflow.app.core.TimeText
import com.salaryflow.app.data.AppSettings
import com.salaryflow.app.ui.SalaryFlowViewModel
import com.salaryflow.app.ui.components.PillSelect
import com.salaryflow.app.ui.components.SectionCard
import com.salaryflow.app.ui.components.SectionHeader
import java.time.DayOfWeek
import java.time.LocalTime

@Composable
fun SettingsScreen(
    viewModel: SalaryFlowViewModel,
    modifier: Modifier = Modifier,
) {
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { },
    )

    var showSalaryDialog by remember { mutableStateOf(false) }
    var showPaidDaysDialog by remember { mutableStateOf(false) }
    var showPaydayDialog by remember { mutableStateOf(false) }
    var showGoalNameDialog by remember { mutableStateOf(false) }
    var showGoalAmountDialog by remember { mutableStateOf(false) }
    var showClearDialog by remember { mutableStateOf(false) }
    var editingTime by remember { mutableStateOf<TimeField?>(null) }

    LazyColumn(
        modifier = modifier.fillMaxWidth(),
        contentPadding = PaddingValues(start = 18.dp, end = 18.dp, top = 10.dp, bottom = 120.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item {
            Column {
                Text("设置", style = MaterialTheme.typography.headlineSmall)
                Text(
                    "算薪规则、通知栏与结算方式",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        item {
            SectionCard {
                SectionHeader(title = "薪资与工时", subtitle = "决定每秒涨多少钱")
                Spacer(Modifier.height(6.dp))
                SettingRow(
                    title = "月薪",
                    value = "¥${Money.centsToYuan(settings.monthlySalaryCents)}",
                    onClick = { showSalaryDialog = true },
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                SettingRow(
                    title = "每月计薪天数",
                    subtitle = "国内常见 21.75 天",
                    value = "${settings.paidDaysPerMonth}",
                    onClick = { showPaidDaysDialog = true },
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                SettingRow(
                    title = "上班时间",
                    value = TimeText.clock(AppSettings.minuteToTime(settings.workStartMinute)),
                    onClick = { editingTime = TimeField.WORK_START },
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                SettingRow(
                    title = "下班时间",
                    value = TimeText.clock(AppSettings.minuteToTime(settings.workEndMinute)),
                    onClick = { editingTime = TimeField.WORK_END },
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                SettingRow(
                    title = "午休",
                    subtitle = "默认 12:00 - 13:00",
                    value = "${TimeText.clock(AppSettings.minuteToTime(settings.lunchStartMinute))} - " +
                        TimeText.clock(AppSettings.minuteToTime(settings.lunchEndMinute)),
                    onClick = { editingTime = TimeField.LUNCH_START },
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                SwitchRow(
                    title = "午休也计薪",
                    subtitle = "开启后午休时间工资继续上涨",
                    checked = settings.lunchPaid,
                    onChange = { value -> viewModel.updateSettings { it.copy(lunchPaid = value) } },
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                Text(
                    "工作日",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(top = 12.dp, bottom = 6.dp),
                )
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    DayOfWeek.entries.forEach { day ->
                        val mask = 1 shl (day.value - 1)
                        PillSelect(
                            text = TimeText.weekNames[day.value - 1].removePrefix("周"),
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
                SectionHeader(title = "攒钱与结算", subtitle = "发薪日自动寄存")
                Spacer(Modifier.height(10.dp))
                Text(
                    "攒钱比例",
                    style = MaterialTheme.typography.titleMedium,
                )
                Spacer(Modifier.height(6.dp))
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(0, 10, 20, 30, 50, 100).forEach { rate ->
                        PillSelect(
                            text = "$rate%",
                            selected = settings.savingsRatePercent == rate,
                            onClick = { viewModel.updateSettings { it.copy(savingsRatePercent = rate) } },
                        )
                    }
                }
                Spacer(Modifier.height(12.dp))
                SettingRow(
                    title = "发薪日",
                    subtitle = "每月这一天把虚拟工资存进罐子",
                    value = "每月 ${settings.payday} 号",
                    onClick = { showPaydayDialog = true },
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                SwitchRow(
                    title = "发薪日自动结算",
                    subtitle = "关掉后需要手动点「提前入账」",
                    checked = settings.autoSettleEnabled,
                    onChange = { value -> viewModel.updateSettings { it.copy(autoSettleEnabled = value) } },
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                SwitchRow(
                    title = "同时记录工资收入",
                    subtitle = "账本里会多一条「工资」收入",
                    checked = settings.autoIncomeEnabled,
                    onChange = { value -> viewModel.updateSettings { it.copy(autoIncomeEnabled = value) } },
                )
                Spacer(Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedButton(
                        onClick = viewModel::settleCurrentPeriodEarly,
                        modifier = Modifier.weight(1f),
                    ) { Text("提前入账") }
                    OutlinedButton(
                        onClick = viewModel::requestDemoCelebration,
                        modifier = Modifier.weight(1f),
                    ) { Text("试播动画") }
                }
            }
        }

        item {
            SectionCard {
                SectionHeader(title = "攒钱目标", subtitle = "给罐子一个小目标")
                Spacer(Modifier.height(6.dp))
                SettingRow(
                    title = "目标名称",
                    value = settings.goalName,
                    onClick = { showGoalNameDialog = true },
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                SettingRow(
                    title = "目标金额",
                    value = "¥${Money.centsToYuan(settings.goalAmountCents)}",
                    onClick = { showGoalAmountDialog = true },
                )
            }
        }

        item {
            SectionCard {
                SectionHeader(title = "通知栏", subtitle = "随时看到工资在涨")
                Spacer(Modifier.height(6.dp))
                SwitchRow(
                    title = "显示实时工资通知",
                    subtitle = "常驻通知栏，需保持服务开启",
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
                            }
                            viewModel.startTickerService()
                        }
                    },
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                SwitchRow(
                    title = "隐藏金额",
                    subtitle = "通知栏与界面显示 ¥••••",
                    checked = settings.hideAmounts,
                    onChange = { value -> viewModel.updateSettings { it.copy(hideAmounts = value) } },
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                SwitchRow(
                    title = "开机自动开启",
                    subtitle = "重启手机后恢复通知栏实时工资",
                    checked = settings.bootStartEnabled,
                    onChange = { value -> viewModel.updateSettings { it.copy(bootStartEnabled = value) } },
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                Text(
                    "刷新频率",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(top = 12.dp, bottom = 6.dp),
                )
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(1 to "1 秒", 5 to "5 秒", 10 to "10 秒", 60 to "1 分钟").forEach { (seconds, label) ->
                        PillSelect(
                            text = label,
                            selected = settings.tickSeconds == seconds,
                            onClick = { viewModel.updateSettings { it.copy(tickSeconds = seconds) } },
                        )
                    }
                }
                Spacer(Modifier.height(8.dp))
                Text(
                    "越快的刷新越耗电，1 秒适合想看「每秒都在涨」的时候。",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        item {
            SectionCard {
                SectionHeader(title = "外观", subtitle = "跟随系统或手动指定")
                Spacer(Modifier.height(8.dp))
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("system" to "跟随系统", "light" to "浅色", "dark" to "深色").forEach { (key, label) ->
                        PillSelect(
                            text = label,
                            selected = settings.themeMode == key,
                            onClick = { viewModel.updateSettings { it.copy(themeMode = key) } },
                        )
                    }
                }
            }
        }

        item {
            SectionCard {
                SectionHeader(title = "数据", subtitle = "全部保存在本机 SQLite")
                Spacer(Modifier.height(10.dp))
                OutlinedButton(
                    onClick = { showClearDialog = true },
                    modifier = Modifier.fillMaxWidth(),
                ) { Text("清空所有流水与结算记录") }
                Spacer(Modifier.height(8.dp))
                Text(
                    "清空后不可恢复，请谨慎操作。",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        item {
            SectionCard {
                SectionHeader(title = "关于", subtitle = "薪流 SalaryFlow")
                Spacer(Modifier.height(8.dp))
                Text(
                    "版本 ${BuildConfig.VERSION_NAME} · MIT License",
                    style = MaterialTheme.typography.bodyMedium,
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    "灵感来自 Jack-Cherish/zhuanlema 的实时工资计算，以及鲨鱼记账的流水记账体验；" +
                        "不联网、不上传、不做金融建议，只是让打工人看见自己每一秒的劳动。",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(10.dp))
                OutlinedButton(
                    onClick = {
                        val intent = android.content.Intent(
                            android.content.Intent.ACTION_VIEW,
                            android.net.Uri.parse("https://github.com/Jack-Cherish/zhuanlema"),
                        )
                        runCatching { context.startActivity(intent) }
                    },
                    modifier = Modifier.fillMaxWidth(),
                ) { Text("查看灵感来源项目") }
            }
        }
    }

    if (showSalaryDialog) {
        NumberInputDialog(
            title = "月薪（元）",
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
            title = "每月计薪天数",
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
            title = "发薪日（1-31）",
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
            title = "目标名称",
            initial = settings.goalName,
            onDismiss = { showGoalNameDialog = false },
            onConfirm = { value ->
                viewModel.updateSettings { it.copy(goalName = value.ifBlank { "第一笔存款" }) }
                showGoalNameDialog = false
            },
        )
    }

    if (showGoalAmountDialog) {
        NumberInputDialog(
            title = "目标金额（元）",
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
            title = field.title,
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
                    }
                }
                editingTime = null
            },
            onSecondStep = {
                if (field == TimeField.LUNCH_START) editingTime = TimeField.LUNCH_END
            },
        )
    }

    if (showClearDialog) {
        AlertDialog(
            onDismissRequest = { showClearDialog = false },
            title = { Text("清空所有数据？") },
            text = { Text("会删除全部流水、自动结算记录与攒钱罐余额，无法恢复。") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.clearAllData()
                        showClearDialog = false
                    },
                ) { Text("确认清空") }
            },
            dismissButton = {
                TextButton(onClick = { showClearDialog = false }) { Text("取消") }
            },
        )
    }
}

private enum class TimeField(val title: String) {
    WORK_START("上班时间"),
    WORK_END("下班时间"),
    LUNCH_START("午休开始"),
    LUNCH_END("午休结束");

    fun current(settings: AppSettings): LocalTime = when (this) {
        WORK_START -> AppSettings.minuteToTime(settings.workStartMinute)
        WORK_END -> AppSettings.minuteToTime(settings.workEndMinute)
        LUNCH_START -> AppSettings.minuteToTime(settings.lunchStartMinute)
        LUNCH_END -> AppSettings.minuteToTime(settings.lunchEndMinute)
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
            TextButton(onClick = { onConfirm(value) }) { Text("保存") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("取消") }
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
        confirmButton = { TextButton(onClick = { onConfirm(value) }) { Text("保存") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TimeInputDialog(
    title: String,
    initial: LocalTime,
    onDismiss: () -> Unit,
    onConfirm: (LocalTime) -> Unit,
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
                if (title == "午休开始") {
                    Text(
                        "保存后继续设置午休结束时间。",
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
            ) { Text("保存") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } },
    )
}
