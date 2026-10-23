package com.salaryflow.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.NotificationsOff
import androidx.compose.material.icons.rounded.Savings
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.salaryflow.app.core.Money
import com.salaryflow.app.core.SalaryClock
import com.salaryflow.app.core.TimeText
import com.salaryflow.app.core.WorkStatus
import com.salaryflow.app.data.TxType
import com.salaryflow.app.ui.SalaryFlowViewModel
import com.salaryflow.app.ui.components.AnimatedMoneyText
import com.salaryflow.app.ui.components.EmojiBadge
import com.salaryflow.app.ui.components.SectionCard
import com.salaryflow.app.ui.components.SectionHeader
import com.salaryflow.app.ui.components.StatusChip
import com.salaryflow.app.ui.components.TransactionRow
import com.salaryflow.app.ui.theme.BrandColors
import java.time.LocalDateTime
import java.time.YearMonth

@Composable
fun HomeScreen(
    viewModel: SalaryFlowViewModel,
    onOpenLedger: () -> Unit,
    onQuickAdd: (TxType) -> Unit,
    onToggleService: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val savingsCents by viewModel.savingsCents.collectAsStateWithLifecycle()
    val todayExpenseCents by viewModel.todayExpenseCents.collectAsStateWithLifecycle()
    val transactions by viewModel.transactions.collectAsStateWithLifecycle()
    val now by viewModel.nowTicker.collectAsStateWithLifecycle(initialValue = LocalDateTime.now())

    val clock = remember(settings) { SalaryClock(settings.toSalaryConfig()) }
    val earnedToday = clock.earnedToday(now)
    val status = clock.status(now)
    val period = clock.accruingPeriod(now)
    val periodAccrued = clock.accruedCentsIn(period, now)
    val daysToPayday = clock.daysToPayday(now.toLocalDate())
    val settledToday = transactions.any { transaction ->
        transaction.periodEnd == now.toLocalDate().toString() &&
            transaction.auto &&
            (
                transaction.settlementKey?.startsWith("saving:") == true ||
                    transaction.settlementKey?.startsWith("income:") == true
                )
    }
    val autoSettlement = Money.ratioToCents(settings.savingsRate, settings.monthlySalaryCents)
    val monthExpense by viewModel.monthExpenseCents.collectAsStateWithLifecycle()
    val goalProgress = if (settings.goalAmountCents <= 0) {
        0f
    } else {
        (savingsCents.toFloat() / settings.goalAmountCents).coerceIn(0f, 1f)
    }

    LazyColumn(
        modifier = modifier.fillMaxWidth(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(
            start = 16.dp,
            end = 16.dp,
            top = 8.dp,
            bottom = 96.dp,
        ),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item {
            HomeTopBar(
                dateText = "${now.monthValue}月${now.dayOfMonth}日 · ${TimeText.weekNames[now.dayOfWeek.value - 1]}",
                serviceEnabled = settings.serviceEnabled,
                hidden = settings.hideAmounts,
                onToggleService = onToggleService,
                onToggleHidden = { viewModel.updateSettings { it.copy(hideAmounts = !it.hideAmounts) } },
            )
        }

        item {
            HeroSalaryCard(
                earnedToday = earnedToday,
                hidden = settings.hideAmounts,
                status = status,
                paused = settings.counterPaused,
                progress = clock.dayProgress(now),
                milestones = clock.milestones(),
                now = now,
                centsPerSecond = clock.centsPerSecond,
                dailyPayCents = clock.dailyPayCents,
                offWorkCountdown = TimeText.countdown(clock.timeUntilOffWork(now)),
                daysToPayday = daysToPayday,
                settledToday = settledToday,
                schedule = clock.config.schedule,
            )
        }

        item {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                SectionCard(
                    modifier = Modifier.weight(1f),
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        EmojiBadge("🫙", size = 34, background = MaterialTheme.colorScheme.primaryContainer)
                        Spacer(Modifier.width(10.dp))
                        Text("已攒钱", style = MaterialTheme.typography.titleMedium)
                    }
                    Spacer(Modifier.height(10.dp))
                    AnimatedMoneyText(
                        cents = savingsCents.toDouble(),
                        hidden = settings.hideAmounts,
                        style = MaterialTheme.typography.displayMedium.copy(fontSize = 26.sp),
                        color = BrandColors.savings,
                    )
                    Spacer(Modifier.height(8.dp))
                    LinearProgressIndicator(
                        progress = { goalProgress },
                        modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(50)),
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        if (settings.hideAmounts) {
                            "目标：${settings.goalName}"
                        } else {
                            "目标：${settings.goalName} · ${(goalProgress * 100).toInt()}%"
                        },
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                SectionCard(
                    modifier = Modifier.weight(1f),
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        EmojiBadge("💸", size = 34, background = MaterialTheme.colorScheme.secondaryContainer)
                        Spacer(Modifier.width(10.dp))
                        Text("今日支出", style = MaterialTheme.typography.titleMedium)
                    }
                    Spacer(Modifier.height(10.dp))
                    AnimatedMoneyText(
                        cents = todayExpenseCents.toDouble(),
                        hidden = settings.hideAmounts,
                        style = MaterialTheme.typography.displayMedium.copy(fontSize = 26.sp),
                        color = BrandColors.expense,
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "本月 ¥${Money.centsToYuan(monthExpense)}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }

        item {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                QuickActionCard(
                    modifier = Modifier.weight(1f),
                    emoji = "✍️",
                    title = "记一笔",
                    subtitle = "支出 / 收入 / 攒钱",
                    onClick = { onQuickAdd(TxType.EXPENSE) },
                )
                QuickActionCard(
                    modifier = Modifier.weight(1f),
                    emoji = "🎉",
                    title = "发薪仪式",
                    subtitle = "试播落袋动画",
                    onClick = viewModel::requestDemoCelebration,
                )
            }
        }

        item {
            PeriodCard(
                periodLabel = period.label,
                accruedCents = periodAccrued,
                salaryCents = settings.monthlySalaryCents,
                settlementCents = autoSettlement,
                savingsRatePercent = settings.savingsRatePercent,
                daysToPayday = daysToPayday,
                settledToday = settledToday,
                hidden = settings.hideAmounts,
                onSettleEarly = viewModel::settleCurrentPeriodEarly,
            )
        }

        item {
            SectionHeader(
                title = "最近流水",
                subtitle = "自动记账与手动记账都在这里",
                trailing = {
                    Text(
                        "查看全部",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .background(MaterialTheme.colorScheme.primaryContainer)
                            .clickable(onClick = onOpenLedger)
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    )
                },
            )
        }

        if (transactions.isEmpty()) {
            item {
                SectionCard {
                    Text(
                        "还没有流水，点上面的「记一笔」开始。",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        } else {
            items(transactions.take(4), key = { it.id }) { transaction ->
                SectionCard(modifier = Modifier.fillMaxWidth()) {
                    TransactionRow(transaction, settings.hideAmounts)
                }
            }
        }

        item {
            Text(
                "所有数据只保存在本机，不联网、不上传。",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun HomeTopBar(
    dateText: String,
    serviceEnabled: Boolean,
    hidden: Boolean,
    onToggleService: () -> Unit,
    onToggleHidden: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Column {
            Text("薪流", style = MaterialTheme.typography.headlineSmall)
            Text(
                dateText,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Row {
            IconButton(onClick = onToggleHidden) {
                Icon(
                    if (hidden) Icons.Rounded.VisibilityOff else Icons.Rounded.Visibility,
                    contentDescription = if (hidden) "显示金额" else "隐藏金额",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            IconButton(onClick = onToggleService) {
                Icon(
                    if (serviceEnabled) Icons.Rounded.NotificationsActive else Icons.Rounded.NotificationsOff,
                    contentDescription = if (serviceEnabled) "关闭通知栏实时工资" else "开启通知栏实时工资",
                    tint = if (serviceEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun HeroSalaryCard(
    earnedToday: Double,
    hidden: Boolean,
    status: WorkStatus,
    paused: Boolean,
    progress: Float,
    milestones: List<com.salaryflow.app.core.DayMilestone>,
    now: LocalDateTime,
    centsPerSecond: Double,
    dailyPayCents: Double,
    offWorkCountdown: String,
    daysToPayday: Long,
    settledToday: Boolean,
    schedule: com.salaryflow.app.core.WorkSchedule,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(28.dp))
            .background(Brush.linearGradient(listOf(BrandColors.heroStart, BrandColors.heroEnd)))
            .drawBehind {
                drawCircle(
                    color = Color.White.copy(alpha = 0.10f),
                    radius = size.minDimension * 0.55f,
                    center = Offset(size.width * 1.02f, size.height * 0.05f),
                )
                drawCircle(
                    color = Color.White.copy(alpha = 0.06f),
                    radius = size.minDimension * 0.35f,
                    center = Offset(size.width * 0.05f, size.height * 0.95f),
                )
            }
            .padding(20.dp),
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                StatusChip(
                    text = statusText(status, paused),
                    containerColor = Color.White.copy(alpha = 0.18f),
                    contentColor = Color.White,
                )
                Text(
                    when {
                        settledToday -> "今日已结算 ✅"
                        daysToPayday <= 0 -> "今天发薪 🧧"
                        else -> "距发薪 $daysToPayday 天"
                    },
                    style = MaterialTheme.typography.labelMedium,
                    color = BrandColors.heroAccent,
                )
            }
            Spacer(Modifier.height(18.dp))
            Text(
                statusLabel(status),
                style = MaterialTheme.typography.labelMedium,
                color = Color.White.copy(alpha = 0.82f),
            )
            Spacer(Modifier.height(4.dp))
            AnimatedMoneyText(
                cents = earnedToday,
                hidden = hidden,
                style = MaterialTheme.typography.displayLarge,
                color = Color.White,
            )
            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    if (hidden) {
                        "每秒 +¥••••"
                    } else {
                        "每秒 +¥${Money.centsToYuanHighPrecision(centsPerSecond)}"
                    },
                    style = MaterialTheme.typography.labelMedium,
                    color = BrandColors.heroAccent,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    "   ·   日薪 " + if (hidden) "¥••••" else "¥${Money.centsToYuan(dailyPayCents)}",
                    style = MaterialTheme.typography.labelMedium,
                    color = Color.White.copy(alpha = 0.75f),
                )
            }

            Spacer(Modifier.height(20.dp))
            DayProgressBar(
                progress = progress,
                milestones = milestones,
                now = now,
                startMinutes = schedule.startTime.hour * 60 + schedule.startTime.minute,
                endMinutes = schedule.endTime.hour * 60 + schedule.endTime.minute,
            )

            Spacer(Modifier.height(16.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Column {
                    Text(
                        if (status == WorkStatus.FINISHED || status == WorkStatus.REST_DAY) "今天" else "距离下班",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White.copy(alpha = 0.72f),
                    )
                    Text(
                        if (status == WorkStatus.FINISHED || status == WorkStatus.REST_DAY) "已收工" else offWorkCountdown,
                        style = MaterialTheme.typography.titleLarge.copy(fontFamily = FontFamily.Monospace),
                        color = Color.White,
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        "计薪进度",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White.copy(alpha = 0.72f),
                    )
                    Text(
                        "${(progress * 100).toInt()}%",
                        style = MaterialTheme.typography.titleLarge.copy(fontFamily = FontFamily.Monospace),
                        color = Color.White,
                    )
                }
            }
        }
    }
}

@Composable
private fun DayProgressBar(
    progress: Float,
    milestones: List<com.salaryflow.app.core.DayMilestone>,
    now: LocalDateTime,
    startMinutes: Int,
    endMinutes: Int,
) {
    BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
        val currentMinutes = now.hour * 60 + now.minute
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(RoundedCornerShape(50))
                .background(Color.White.copy(alpha = 0.22f)),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(progress)
                    .fillMaxHeight()
                    .background(BrandColors.heroAccent),
            )
        }
        milestones.forEach { milestone ->
            val milestoneMinutes = milestone.time.hour * 60 + milestone.time.minute
            val fraction = milestoneFraction(milestoneMinutes, startMinutes, endMinutes)
            Box(
                modifier = Modifier
                    .offset(x = maxWidth * fraction - 1.dp, y = (-2).dp)
                    .width(2.dp)
                    .height(12.dp)
                    .background(
                        if (currentMinutes >= milestoneMinutes) Color.White else Color.White.copy(alpha = 0.45f),
                    ),
            )
        }
    }
    Spacer(Modifier.height(6.dp))
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        milestones.forEach { milestone ->
            Text(
                "${milestone.emoji} ${TimeText.clock(milestone.time)}",
                style = MaterialTheme.typography.labelSmall,
                color = Color.White.copy(alpha = 0.78f),
            )
        }
    }
}

private fun milestoneFraction(
    minutes: Int,
    startMinutes: Int,
    endMinutes: Int,
): Float {
    if (endMinutes <= startMinutes) return 0f
    return ((minutes - startMinutes).toFloat() / (endMinutes - startMinutes)).coerceIn(0f, 1f)
}

@Composable
private fun QuickActionCard(
    emoji: String,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    SectionCard(modifier = modifier, onClick = onClick) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            EmojiBadge(emoji, size = 38)
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleMedium)
                Text(
                    subtitle,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Icon(
                Icons.Rounded.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun PeriodCard(
    periodLabel: String,
    accruedCents: Double,
    salaryCents: Long,
    settlementCents: Long,
    savingsRatePercent: Int,
    daysToPayday: Long,
    settledToday: Boolean,
    hidden: Boolean,
    onSettleEarly: () -> Unit,
) {
    SectionCard {
        SectionHeader(
            title = "本期工资",
            subtitle = periodLabel,
            trailing = {
                StatusChip(
                    text = when {
                        settledToday -> "今日已结算"
                        daysToPayday <= 0 -> "今天结算"
                        else -> "$daysToPayday 天后结算"
                    },
                    containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                    contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
                )
            },
        )
        Spacer(Modifier.height(14.dp))
        Row(verticalAlignment = Alignment.Bottom) {
            Text(
                if (hidden) "¥••••" else "¥${Money.centsToYuan(accruedCents)}",
                style = MaterialTheme.typography.displayMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Spacer(Modifier.width(8.dp))
            Text(
                if (hidden) "/ ¥••••" else "/ ¥${Money.centsToYuan(salaryCents)}",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 6.dp),
            )
        }
        Spacer(Modifier.height(10.dp))
        LinearProgressIndicator(
            progress = {
                if (salaryCents <= 0) 0f else (accruedCents / salaryCents).toFloat().coerceIn(0f, 1f)
            },
            modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(50)),
        )
        Spacer(Modifier.height(12.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                Icons.Rounded.Savings,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
            )
            Spacer(Modifier.width(8.dp))
            Text(
                buildString {
                    append("发薪日自动存 ")
                    append(if (hidden) "¥••••" else "¥${Money.centsToYuan(settlementCents)}")
                    append("（工资的 $savingsRatePercent%）")
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f),
            )
        }
        Spacer(Modifier.height(6.dp))
        Text(
            "点下面可以先把本期已累积的部分提前存进罐子。",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(10.dp))
        androidx.compose.material3.OutlinedButton(
            onClick = onSettleEarly,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("提前入账本期")
        }
    }
}

private fun statusText(status: WorkStatus, paused: Boolean): String = when {
    paused -> "已暂停 ⏸"
    status == WorkStatus.WORKING -> "搬砖中 💪"
    status == WorkStatus.UNPAID_BREAK -> "午休中 🍚"
    status == WorkStatus.BEFORE_WORK -> "还没开工 ☕"
    status == WorkStatus.FINISHED -> "已下班 🎉"
    else -> "休息日 🌴"
}

private fun statusLabel(status: WorkStatus): String = when (status) {
    WorkStatus.WORKING -> "今天已挣"
    WorkStatus.UNPAID_BREAK -> "今天已挣（午休不计薪）"
    WorkStatus.BEFORE_WORK -> "今天还没开工"
    WorkStatus.FINISHED -> "今日收工，一共挣了"
    WorkStatus.REST_DAY -> "今天休息，不营业"
}
