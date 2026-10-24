package com.salaryflow.app.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.Payments
import androidx.compose.material.icons.rounded.Savings
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.salaryflow.app.core.Money
import com.salaryflow.app.ui.components.PillSelect
import com.salaryflow.app.ui.theme.BrandColors

data class OnboardingResult(
    val monthlySalaryCents: Long,
    val paidDaysPerMonth: Double,
    val payday: Int,
    val workStartMinute: Int,
    val workEndMinute: Int,
    val actualSavingsCents: Long,
    val enableLiveNotification: Boolean,
    val enableBalanceReminder: Boolean,
)

private data class SchedulePreset(
    val label: String,
    val workStartMinute: Int,
    val workEndMinute: Int,
)

private val schedulePresets = listOf(
    SchedulePreset("09:00 - 18:00", 9 * 60, 18 * 60),
    SchedulePreset("10:00 - 19:00", 10 * 60, 19 * 60),
    SchedulePreset("08:30 - 17:30", 8 * 60 + 30, 17 * 60 + 30),
)

/**
 * 本地初始化引导。
 *
 * 这里不创建云账号，所有信息只写入设备；“注册”完成后再进入主页。
 */
@Composable
fun OnboardingScreen(
    onComplete: (OnboardingResult) -> Unit,
    modifier: Modifier = Modifier,
) {
    var step by remember { mutableStateOf(0) }
    var salaryText by remember { mutableStateOf("") }
    var paidDays by remember { mutableStateOf(21.75) }
    var paydayText by remember { mutableStateOf("15") }
    var schedule by remember { mutableStateOf(schedulePresets.first()) }
    var actualBalanceText by remember { mutableStateOf("") }
    var enableLiveNotification by remember { mutableStateOf(true) }
    var enableBalanceReminder by remember { mutableStateOf(true) }

    val salaryCents = Money.yuanToCents(salaryText)
    val actualBalanceCents = actualBalanceText
        .takeIf { it.isNotBlank() }
        ?.let(Money::yuanToCents)
    val payday = paydayText.toIntOrNull()
    val salaryValid = salaryCents != null && salaryCents > 0
    val paydayValid = payday != null && payday in 1..31
    val balanceValid = actualBalanceText.isBlank() || actualBalanceCents != null
    val canContinue = salaryValid && paydayValid && balanceValid

    BackHandler(enabled = step > 0) { step-- }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
    ) {
        AnimatedContent(
            targetState = step,
            transitionSpec = {
                val forward = targetState > initialState
                (
                    slideInHorizontally(tween(360)) { width -> if (forward) width / 4 else -width / 4 } +
                        fadeIn(tween(280))
                    ) togetherWith (
                    slideOutHorizontally(tween(320)) { width -> if (forward) -width / 5 else width / 5 } +
                        fadeOut(tween(220))
                    )
            },
            label = "onboarding-step",
            modifier = Modifier.fillMaxSize(),
        ) { currentStep ->
            when (currentStep) {
                0 -> WelcomeStep(onContinue = { step = 1 })
                1 -> SalaryStep(
                    step = currentStep,
                    salaryText = salaryText,
                    onSalaryChange = { salaryText = it },
                    paidDays = paidDays,
                    onPaidDaysChange = { paidDays = it },
                    paydayText = paydayText,
                    onPaydayChange = { paydayText = it },
                    schedule = schedule,
                    onScheduleChange = { schedule = it },
                    canContinue = canContinue,
                    onBack = { step = 0 },
                    onContinue = { step = 2 },
                )
                else -> BalanceStep(
                    step = currentStep,
                    actualBalanceText = actualBalanceText,
                    onActualBalanceChange = { actualBalanceText = it },
                    enableLiveNotification = enableLiveNotification,
                    onLiveNotificationChange = { enableLiveNotification = it },
                    enableBalanceReminder = enableBalanceReminder,
                    onBalanceReminderChange = { enableBalanceReminder = it },
                    canFinish = canContinue,
                    onBack = { step = 1 },
                    onFinish = {
                        if (salaryCents != null && payday != null) {
                            onComplete(
                                OnboardingResult(
                                    monthlySalaryCents = salaryCents,
                                    paidDaysPerMonth = paidDays,
                                    payday = payday,
                                    workStartMinute = schedule.workStartMinute,
                                    workEndMinute = schedule.workEndMinute,
                                    actualSavingsCents = actualBalanceCents ?: 0L,
                                    enableLiveNotification = enableLiveNotification,
                                    enableBalanceReminder = enableBalanceReminder,
                                ),
                            )
                        }
                    },
                )
            }
        }
    }
}

@Composable
private fun WelcomeStep(onContinue: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(Color(0xFF082A23), Color(0xFF155C4A), Color(0xFF0C3D34)),
                ),
            )
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = 24.dp, vertical = 22.dp),
    ) {
        Canvas(Modifier.fillMaxSize()) {
            drawCircle(
                color = Color.White.copy(alpha = 0.045f),
                radius = size.minDimension * 0.62f,
                center = Offset(size.width * 1.05f, size.height * 0.06f),
            )
            drawCircle(
                color = BrandColors.heroAccent.copy(alpha = 0.07f),
                radius = size.minDimension * 0.42f,
                center = Offset(size.width * 0.02f, size.height * 0.96f),
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                BrandMark()
                Column {
                    Text(
                        "薪流",
                        style = MaterialTheme.typography.titleLarge,
                        color = Color.White,
                    )
                    Text(
                        "SalaryFlow",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White.copy(alpha = 0.62f),
                    )
                }
            }

            Spacer(Modifier.height(72.dp))
            Surface(
                shape = RoundedCornerShape(999.dp),
                color = BrandColors.heroAccent.copy(alpha = 0.14f),
                border = BorderStroke(1.dp, BrandColors.heroAccent.copy(alpha = 0.3f)),
            ) {
                Text(
                    "纯本地 · 不需要云账号",
                    style = MaterialTheme.typography.labelMedium,
                    color = BrandColors.heroAccent,
                    modifier = Modifier.padding(horizontal = 13.dp, vertical = 7.dp),
                )
            }
            Spacer(Modifier.height(22.dp))
            Text(
                "让每一秒工资\n都有迹可循",
                style = MaterialTheme.typography.displayLarge.copy(
                    fontSize = 45.sp,
                    lineHeight = 51.sp,
                ),
                color = Color.White,
            )
            Spacer(Modifier.height(16.dp))
            Text(
                "通知栏实时显示今天已经赚到多少，\n收入、支出和实际存款都留在同一本账里。",
                style = MaterialTheme.typography.bodyLarge,
                color = Color.White.copy(alpha = 0.76f),
            )

            Spacer(Modifier.height(42.dp))
            WelcomeFeature(
                icon = Icons.Rounded.Payments,
                title = "实时工资",
                subtitle = "工作时段每秒增长，发薪日收入落到账本",
            )
            WelcomeFeature(
                icon = Icons.Rounded.Savings,
                title = "看得见的存款",
                subtitle = "收入与支出自动推算，每月用真实余额校准",
            )
            WelcomeFeature(
                icon = Icons.Rounded.Lock,
                title = "只保存在本机",
                subtitle = "没有网络权限、没有埋点，也不上传金额",
            )

            Spacer(Modifier.height(56.dp))
            Button(
                onClick = onContinue,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(18.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = BrandColors.heroAccent,
                    contentColor = Color(0xFF07352B),
                ),
            ) {
                Text("开始设置", fontWeight = FontWeight.Bold)
                Spacer(Modifier.size(8.dp))
                Icon(Icons.AutoMirrored.Rounded.ArrowForward, contentDescription = null)
            }
            Spacer(Modifier.height(10.dp))
            Text(
                "约 1 分钟完成，之后可随时在设置里修改。",
                style = MaterialTheme.typography.labelSmall,
                color = Color.White.copy(alpha = 0.55f),
                modifier = Modifier.align(Alignment.CenterHorizontally),
            )
        }
    }
}

@Composable
private fun WelcomeFeature(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Surface(
            shape = RoundedCornerShape(14.dp),
            color = Color.White.copy(alpha = 0.09f),
            modifier = Modifier.size(46.dp),
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    icon,
                    contentDescription = null,
                    tint = BrandColors.heroAccent,
                    modifier = Modifier.size(23.dp),
                )
            }
        }
        Spacer(Modifier.size(13.dp))
        Column {
            Text(title, style = MaterialTheme.typography.titleMedium, color = Color.White)
            Text(
                subtitle,
                style = MaterialTheme.typography.labelSmall,
                color = Color.White.copy(alpha = 0.61f),
            )
        }
    }
}

@Composable
private fun SalaryStep(
    step: Int,
    salaryText: String,
    onSalaryChange: (String) -> Unit,
    paidDays: Double,
    onPaidDaysChange: (Double) -> Unit,
    paydayText: String,
    onPaydayChange: (String) -> Unit,
    schedule: SchedulePreset,
    onScheduleChange: (SchedulePreset) -> Unit,
    canContinue: Boolean,
    onBack: () -> Unit,
    onContinue: () -> Unit,
) {
    OnboardingPage(
        step = step,
        title = "你的工资怎么算？",
        subtitle = "这些数据决定通知栏每秒上涨的速度，之后可以再改。",
        onBack = onBack,
    ) {
        OnboardingCard {
            Text("税后月薪", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(10.dp))
            OutlinedTextField(
                value = salaryText,
                onValueChange = { input ->
                    onSalaryChange(input.filter { it.isDigit() || it == '.' || it == ',' }.take(12))
                },
                prefix = { Text("¥ ") },
                placeholder = { Text("例如 10000") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                textStyle = MaterialTheme.typography.displayMedium,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(9.dp))
            Text(
                "用于计算日薪与每秒收入，不会联网校验。",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        Spacer(Modifier.height(14.dp))
        OnboardingCard {
            Text("每月计薪天数", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(10.dp))
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                listOf(21.75 to "21.75 天", 22.0 to "22 天", 26.0 to "26 天").forEach { (days, label) ->
                    PillSelect(
                        text = label,
                        selected = paidDays == days,
                        onClick = { onPaidDaysChange(days) },
                    )
                }
            }
        }

        Spacer(Modifier.height(14.dp))
        OnboardingCard {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f)) {
                    Text("每月发薪日", style = MaterialTheme.typography.titleMedium)
                    Text(
                        "工资在这一天正式计入收入和存款",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                OutlinedTextField(
                    value = paydayText,
                    onValueChange = { input -> onPaydayChange(input.filter(Char::isDigit).take(2)) },
                    suffix = { Text("号") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .padding(start = 12.dp)
                        .size(width = 104.dp, height = 64.dp),
                )
            }
        }

        Spacer(Modifier.height(14.dp))
        OnboardingCard {
            Text("常规工作日", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(4.dp))
            Text(
                "默认周一到周五；午休和调休可在设置里调整。",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(11.dp))
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                schedulePresets.forEach { preset ->
                    PillSelect(
                        text = preset.label,
                        selected = schedule == preset,
                        onClick = { onScheduleChange(preset) },
                    )
                }
            }
        }

        Spacer(Modifier.height(24.dp))
        PrimaryOnboardingButton(
            text = "继续",
            enabled = canContinue,
            onClick = onContinue,
        )
    }
}

@Composable
private fun BalanceStep(
    step: Int,
    actualBalanceText: String,
    onActualBalanceChange: (String) -> Unit,
    enableLiveNotification: Boolean,
    onLiveNotificationChange: (Boolean) -> Unit,
    enableBalanceReminder: Boolean,
    onBalanceReminderChange: (Boolean) -> Unit,
    canFinish: Boolean,
    onBack: () -> Unit,
    onFinish: () -> Unit,
) {
    OnboardingPage(
        step = step,
        title = "先对一次真实余额",
        subtitle = "第三方个人账户没有可用的余额读取接口，所以由你自己掌握最终数字。",
        onBack = onBack,
    ) {
        OnboardingCard {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    shape = CircleShape,
                    color = BrandColors.savings.copy(alpha = 0.1f),
                    modifier = Modifier.size(42.dp),
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            Icons.Rounded.Savings,
                            contentDescription = null,
                            tint = BrandColors.savings,
                            modifier = Modifier.size(22.dp),
                        )
                    }
                }
                Spacer(Modifier.size(12.dp))
                Column {
                    Text("当前实际存款", style = MaterialTheme.typography.titleMedium)
                    Text(
                        "可以留空，第一次使用时从 0 开始",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            Spacer(Modifier.height(16.dp))
            OutlinedTextField(
                value = actualBalanceText,
                onValueChange = { input ->
                    onActualBalanceChange(input.filter { it.isDigit() || it == '.' }.take(14))
                },
                prefix = { Text("¥ ") },
                placeholder = { Text("例如 35000") },
                label = { Text("可选") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                textStyle = MaterialTheme.typography.displayMedium,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(10.dp))
            Text(
                "收入和支出会自动增减这个数字；漏记时每月校准一次即可。",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        Spacer(Modifier.height(14.dp))
        OnboardingCard {
            OnboardingSwitchRow(
                icon = Icons.Rounded.NotificationsActive,
                title = "开启通知栏实时工资",
                subtitle = "随时查看今日已赚、存款和支出",
                checked = enableLiveNotification,
                onChange = onLiveNotificationChange,
            )
            Spacer(Modifier.height(14.dp))
            OnboardingSwitchRow(
                icon = Icons.Rounded.Savings,
                title = "每月提醒校准存款",
                subtitle = "每月 1 日提醒一次，防止记账误差累积",
                checked = enableBalanceReminder,
                onChange = onBalanceReminderChange,
            )
        }

        Spacer(Modifier.height(14.dp))
        Surface(
            shape = RoundedCornerShape(18.dp),
            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.52f),
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.Top,
            ) {
                Icon(
                    Icons.Rounded.Lock,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp),
                )
                Spacer(Modifier.size(10.dp))
                Text(
                    "不绑定支付宝、微信或网银，不读取通知，也不请求无障碍权限。" +
                        "所有金额只写入本机应用数据库。",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        Spacer(Modifier.height(24.dp))
        PrimaryOnboardingButton(
            text = "进入薪流",
            enabled = canFinish,
            onClick = onFinish,
        )
    }
}

@Composable
private fun OnboardingPage(
    step: Int,
    title: String,
    subtitle: String,
    onBack: () -> Unit,
    content: @Composable () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 12.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "返回")
            }
            Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                repeat(3) { index ->
                    Box(
                        modifier = Modifier
                            .size(
                                width = if (index == step) 28.dp else 8.dp,
                                height = 8.dp,
                            )
                            .background(
                                color = if (index <= step) {
                                    BrandColors.savings
                                } else {
                                    MaterialTheme.colorScheme.outlineVariant
                                },
                                shape = RoundedCornerShape(99.dp),
                            ),
                    )
                }
            }
            Spacer(Modifier.size(48.dp))
        }

        Spacer(Modifier.height(22.dp))
        Text(title, style = MaterialTheme.typography.headlineSmall)
        Spacer(Modifier.height(6.dp))
        Text(
            subtitle,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(24.dp))
        content()
        Spacer(Modifier.height(28.dp))
    }
}

@Composable
private fun OnboardingCard(content: @Composable () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Column(Modifier.padding(18.dp)) { content() }
    }
}

@Composable
private fun OnboardingSwitchRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    checked: Boolean,
    onChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Surface(
            shape = RoundedCornerShape(13.dp),
            color = MaterialTheme.colorScheme.primaryContainer,
            modifier = Modifier.size(42.dp),
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(21.dp),
                )
            }
        }
        Spacer(Modifier.size(12.dp))
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
                checkedTrackColor = BrandColors.savings,
            ),
        )
    }
}

@Composable
private fun PrimaryOnboardingButton(
    text: String,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp),
        shape = RoundedCornerShape(18.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = BrandColors.savings,
            contentColor = Color.White,
        ),
    ) {
        Text(text, fontWeight = FontWeight.Bold)
        Spacer(Modifier.size(8.dp))
        Icon(Icons.AutoMirrored.Rounded.ArrowForward, contentDescription = null)
    }
}

@Composable
private fun BrandMark() {
    Canvas(Modifier.size(48.dp)) {
        drawCircle(
            color = BrandColors.heroAccent.copy(alpha = 0.12f),
            radius = size.minDimension / 2f,
        )
        val jar = Path().apply {
            moveTo(size.width * 0.28f, size.height * 0.35f)
            lineTo(size.width * 0.72f, size.height * 0.35f)
            lineTo(size.width * 0.67f, size.height * 0.78f)
            lineTo(size.width * 0.33f, size.height * 0.78f)
            close()
        }
        drawPath(jar, color = BrandColors.heroAccent.copy(alpha = 0.16f))
        drawPath(
            jar,
            color = BrandColors.heroAccent,
            style = Stroke(width = 3f, cap = StrokeCap.Round),
        )
        drawRoundRect(
            color = BrandColors.heroAccent,
            topLeft = Offset(size.width * 0.26f, size.height * 0.27f),
            size = Size(size.width * 0.48f, size.height * 0.09f),
            cornerRadius = CornerRadius(size.height * 0.045f),
        )
        drawCircle(
            color = Color(0xFFF0C567),
            radius = size.minDimension * 0.055f,
            center = Offset(size.width * 0.5f, size.height * 0.57f),
        )
    }
}
