package com.salaryflow.app.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.NotificationsOff
import androidx.compose.material.icons.automirrored.rounded.TrendingDown
import androidx.compose.material.icons.automirrored.rounded.TrendingUp
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.salaryflow.app.R
import com.salaryflow.app.core.Money
import com.salaryflow.app.core.SalaryClock
import com.salaryflow.app.core.TimeText
import com.salaryflow.app.core.WorkStatus
import com.salaryflow.app.data.TxType
import com.salaryflow.app.ui.SalaryFlowViewModel
import com.salaryflow.app.ui.components.AnimatedMoneyText
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
    onCalibrateBalance: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val savingsCents by viewModel.savingsCents.collectAsStateWithLifecycle()
    val todayExpenseCents by viewModel.todayExpenseCents.collectAsStateWithLifecycle()
    val monthExpenseCents by viewModel.monthExpenseCents.collectAsStateWithLifecycle()
    val monthIncomeCents by viewModel.monthIncomeCents.collectAsStateWithLifecycle()
    val transactions by viewModel.transactions.collectAsStateWithLifecycle()
    val now by viewModel.nowTicker.collectAsStateWithLifecycle(initialValue = LocalDateTime.now())

    val clock = remember(settings) { SalaryClock(settings.toSalaryConfig()) }
    val earnedToday = clock.earnedToday(now)
    val status = clock.status(now)
    val period = clock.accruingPeriod(now)
    val daysToPayday = clock.daysToPayday(now.toLocalDate())
    val paydayToday = daysToPayday == 0L
    val dayProgress = if (status == WorkStatus.REST_DAY) 0f else clock.dayProgress(now)
    val settledToday = transactions.any { transaction ->
        transaction.periodEnd == now.toLocalDate().toString() &&
            transaction.auto &&
            (
                transaction.settlementKey?.startsWith("saving:") == true ||
                    transaction.settlementKey?.startsWith("income:") == true
                )
    }
    val displayPeriod = if (paydayToday) clock.periodEndingAt(now.toLocalDate()) else period
    val displayAccrued = if (settledToday) {
        settings.monthlySalaryCents.toDouble()
    } else {
        clock.accruedCentsIn(displayPeriod, now)
    }
    val goalProgress = if (settings.goalAmountCents <= 0) {
        0f
    } else {
        (savingsCents.toFloat() / settings.goalAmountCents).coerceIn(0f, 1f)
    }

    LazyColumn(
        modifier = modifier.fillMaxWidth(),
        contentPadding = PaddingValues(start = 18.dp, end = 18.dp, top = 10.dp, bottom = 104.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item {
            HomeTopBar(
                dateText = TimeText.day(now.toLocalDate()) + " · " + TimeText.weekday(now.dayOfWeek),
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
                paused = false,
                progress = dayProgress,
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
            QuickEntryRow(
                onExpense = { onQuickAdd(TxType.EXPENSE) },
                onIncome = { onQuickAdd(TxType.INCOME) },
            )
        }

        item {
            MonthlyOverviewCard(
                monthLabel = TimeText.monthYear(YearMonth.from(now)),
                savingsCents = savingsCents,
                todayExpenseCents = todayExpenseCents,
                monthExpenseCents = monthExpenseCents,
                monthIncomeCents = monthIncomeCents,
                goalName = settings.goalName,
                goalAmountCents = settings.goalAmountCents,
                goalProgress = goalProgress,
                hidden = settings.hideAmounts,
                onCalibrate = onCalibrateBalance,
            )
        }

        item {
            IncomeArrivalCard(
                periodLabel = TimeText.period(displayPeriod.start, displayPeriod.end),
                cycleStartLabel = TimeText.day(displayPeriod.start),
                paydayLabel = TimeText.day(displayPeriod.end),
                expectedCents = settings.monthlySalaryCents,
                accruedCents = displayAccrued,
                daysToPayday = daysToPayday,
                landed = settledToday,
                hidden = settings.hideAmounts,
                onDemoCelebration = viewModel::requestDemoCelebration,
            )
        }

        item {
            SectionHeader(
                title = stringResource(R.string.recent_transactions),
                subtitle = stringResource(R.string.recent_transactions_subtitle),
                trailing = {
                    Text(
                        stringResource(R.string.view_ledger),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .clickable(onClick = onOpenLedger)
                            .padding(horizontal = 10.dp, vertical = 7.dp),
                    )
                },
            )
        }

        if (transactions.isEmpty()) {
            item {
                SectionCard {
                    Text(
                        stringResource(R.string.recent_transactions_empty),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        } else {
            item {
                SectionCard {
                    transactions.take(4).forEachIndexed { index, transaction ->
                        TransactionRow(transaction, settings.hideAmounts)
                        if (index != transactions.take(4).lastIndex) {
                            com.salaryflow.app.ui.components.Divider(
                                Modifier.padding(start = 54.dp),
                            )
                        }
                    }
                }
            }
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
            Text(stringResource(R.string.app_name), style = MaterialTheme.typography.headlineSmall)
            Text(
                dateText,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            TopBarIconButton(
                icon = if (hidden) Icons.Rounded.VisibilityOff else Icons.Rounded.Visibility,
                contentDescription = stringResource(
                    if (hidden) R.string.show_amounts else R.string.hide_amounts,
                ),
                active = false,
                onClick = onToggleHidden,
            )
            TopBarIconButton(
                icon = if (serviceEnabled) Icons.Rounded.NotificationsActive else Icons.Rounded.NotificationsOff,
                contentDescription = stringResource(
                    if (serviceEnabled) {
                        R.string.disable_salary_notification
                    } else {
                        R.string.enable_salary_notification
                    },
                ),
                active = serviceEnabled,
                onClick = onToggleService,
            )
        }
    }
}

@Composable
private fun TopBarIconButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    contentDescription: String,
    active: Boolean,
    onClick: () -> Unit,
) {
    Surface(
        modifier = Modifier.size(48.dp),
        shape = CircleShape,
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        IconButton(onClick = onClick, modifier = Modifier.size(48.dp)) {
            Icon(
                icon,
                contentDescription = contentDescription,
                tint = if (active) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(21.dp),
            )
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
            .clip(RoundedCornerShape(22.dp))
            .background(Brush.linearGradient(listOf(BrandColors.heroStart, BrandColors.heroEnd)))
            .drawBehind {
                drawCircle(
                    color = Color.White.copy(alpha = 0.055f),
                    radius = size.minDimension * 0.68f,
                    center = Offset(size.width * 1.04f, size.height * 0.02f),
                )
                drawCircle(
                    color = BrandColors.heroAccent.copy(alpha = 0.06f),
                    radius = size.minDimension * 0.3f,
                    center = Offset(size.width * 0.03f, size.height * 0.97f),
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
                    containerColor = Color.White.copy(alpha = 0.11f),
                    contentColor = Color.White,
                )
                Text(
                    when {
                        settledToday -> stringResource(R.string.home_settled_this_cycle)
                        daysToPayday <= 0L -> stringResource(R.string.home_payday_today)
                        else -> stringResource(R.string.home_days_to_payday, daysToPayday)
                    },
                    style = MaterialTheme.typography.labelMedium,
                    color = BrandColors.heroAccent,
                )
            }
            Spacer(Modifier.height(22.dp))
            Text(
                statusLabel(status),
                style = MaterialTheme.typography.labelMedium,
                color = Color.White.copy(alpha = 0.72f),
            )
            Spacer(Modifier.height(2.dp))
            AnimatedMoneyText(
                cents = earnedToday,
                hidden = hidden,
                style = MaterialTheme.typography.displayLarge,
                color = Color.White,
            )
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    if (hidden) {
                        stringResource(
                            R.string.home_per_second,
                            stringResource(R.string.amount_hidden_long).removePrefix("¥"),
                        )
                    } else {
                        stringResource(
                            R.string.home_per_second,
                            Money.centsToYuanHighPrecision(centsPerSecond),
                        )
                    },
                    style = MaterialTheme.typography.labelMedium,
                    color = BrandColors.heroAccent,
                )
                Text(
                    stringResource(
                        R.string.home_daily_pay,
                        if (hidden) {
                            stringResource(R.string.amount_hidden_long).removePrefix("¥")
                        } else {
                            Money.centsToYuan(dailyPayCents)
                        },
                    ),
                    style = MaterialTheme.typography.labelMedium,
                    color = Color.White.copy(alpha = 0.68f),
                )
            }

            Spacer(Modifier.height(22.dp))
            DayProgressBar(
                progress = progress,
                milestones = milestones,
                now = now,
                startMinutes = schedule.startTime.hour * 60 + schedule.startTime.minute,
                endMinutes = schedule.endTime.hour * 60 + schedule.endTime.minute,
            )

            Spacer(Modifier.height(18.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom,
            ) {
                Column {
                    Text(
                        stringResource(
                            if (status == WorkStatus.FINISHED || status == WorkStatus.REST_DAY) {
                                R.string.home_today
                            } else {
                                R.string.home_until_off_work
                            },
                        ),
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White.copy(alpha = 0.62f),
                    )
                    Text(
                        if (status == WorkStatus.REST_DAY) {
                            stringResource(R.string.home_day_off)
                        } else if (status == WorkStatus.FINISHED) {
                            stringResource(R.string.home_off_work_done)
                        } else {
                            offWorkCountdown
                        },
                        style = MaterialTheme.typography.titleLarge,
                        color = Color.White,
                    )
                }
                if (status != WorkStatus.REST_DAY) {
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            stringResource(R.string.home_day_progress),
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White.copy(alpha = 0.62f),
                        )
                        Text(
                            "${(progress * 100).toInt()}%",
                            style = MaterialTheme.typography.titleLarge,
                            color = Color.White,
                        )
                    }
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
                .height(6.dp)
                .clip(RoundedCornerShape(50))
                .background(Color.White.copy(alpha = 0.16f)),
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
                    .height(10.dp)
                    .background(
                        if (currentMinutes >= milestoneMinutes) Color.White else Color.White.copy(alpha = 0.38f),
                    ),
            )
        }
    }
    Spacer(Modifier.height(7.dp))
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        milestones.forEach { milestone ->
            Text(
                TimeText.clock(milestone.time),
                style = MaterialTheme.typography.labelSmall,
                color = Color.White.copy(alpha = 0.68f),
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
private fun QuickEntryRow(
    onExpense: () -> Unit,
    onIncome: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        QuickEntryButton(
            modifier = Modifier.weight(1f),
            icon = Icons.AutoMirrored.Rounded.TrendingDown,
            title = stringResource(R.string.quick_add_expense),
            subtitle = "−",
            accent = BrandColors.expense,
            onClick = onExpense,
        )
        QuickEntryButton(
            modifier = Modifier.weight(1f),
            icon = Icons.AutoMirrored.Rounded.TrendingUp,
            title = stringResource(R.string.quick_add_income),
            subtitle = "+",
            accent = BrandColors.savings,
            onClick = onIncome,
        )
    }
}

@Composable
private fun QuickEntryButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    accent: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier
            .height(84.dp)
            .clip(RoundedCornerShape(20.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(20.dp),
        color = accent,
        contentColor = Color.White,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(RoundedCornerShape(13.dp))
                    .background(Color.White.copy(alpha = 0.14f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
            }
            Spacer(Modifier.width(11.dp))
            Column {
                Text(
                    title,
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White,
                )
                Text(
                    subtitle,
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White.copy(alpha = 0.72f),
                )
            }
        }
    }
}

@Composable
private fun MonthlyOverviewCard(
    monthLabel: String,
    savingsCents: Long,
    todayExpenseCents: Long,
    monthExpenseCents: Long,
    monthIncomeCents: Long,
    goalName: String,
    goalAmountCents: Long,
    goalProgress: Float,
    hidden: Boolean,
    onCalibrate: () -> Unit,
) {
    val monthBalanceCents = monthIncomeCents - monthExpenseCents
    val displayGoalName = goalName.ifBlank { stringResource(R.string.default_goal_name) }

    SectionCard {
        SectionHeader(
            title = stringResource(R.string.monthly_overview),
            subtitle = monthLabel,
            trailing = {
                TextButton(onClick = onCalibrate) {
                    Text(stringResource(R.string.update_balance))
                }
            },
        )

        Spacer(Modifier.height(14.dp))
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(BrandColors.savings.copy(alpha = 0.075f))
                .padding(horizontal = 16.dp, vertical = 15.dp),
        ) {
            Text(
                stringResource(R.string.current_savings),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(4.dp))
            AnimatedMoneyText(
                cents = savingsCents.toDouble(),
                hidden = hidden,
                style = MaterialTheme.typography.displayMedium,
                color = BrandColors.savings,
                animated = false,
            )
            Spacer(Modifier.height(3.dp))
            Text(
                stringResource(R.string.current_savings_subtitle),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        Spacer(Modifier.height(16.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            SummaryMetric(
                modifier = Modifier.weight(1f),
                label = stringResource(R.string.monthly_income),
                value = if (hidden) {
                    stringResource(R.string.amount_hidden_short)
                } else {
                    "+¥${Money.compactYuan(monthIncomeCents)}"
                },
                color = BrandColors.savings,
            )
            SummaryMetric(
                modifier = Modifier.weight(1f),
                label = stringResource(R.string.monthly_expense),
                value = if (hidden) {
                    stringResource(R.string.amount_hidden_short)
                } else {
                    "−¥${Money.compactYuan(monthExpenseCents)}"
                },
                sub = stringResource(
                    R.string.today_expense_value,
                    if (hidden) {
                        stringResource(R.string.amount_hidden_short)
                    } else {
                        "¥${Money.centsToYuan(todayExpenseCents)}"
                    },
                ),
                color = BrandColors.expense,
            )
            SummaryMetric(
                modifier = Modifier.weight(1f),
                label = stringResource(R.string.monthly_balance),
                value = if (hidden) {
                    stringResource(R.string.amount_hidden_short)
                } else {
                    (if (monthBalanceCents >= 0) "+" else "−") +
                        "¥${Money.compactYuan(kotlin.math.abs(monthBalanceCents))}"
                },
                color = if (monthBalanceCents >= 0) {
                    BrandColors.savings
                } else {
                    BrandColors.expense
                },
            )
        }

        Spacer(Modifier.height(18.dp))
        LinearProgressIndicator(
            progress = { goalProgress },
            modifier = Modifier
                .fillMaxWidth()
                .height(5.dp)
                .clip(RoundedCornerShape(50)),
            color = BrandColors.savings,
            trackColor = MaterialTheme.colorScheme.surfaceVariant,
        )
        Spacer(Modifier.height(7.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                displayGoalName,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                if (hidden) {
                    stringResource(R.string.goal_progress_hidden)
                } else {
                    stringResource(
                        R.string.goal_progress,
                        (goalProgress * 100).toInt(),
                        Money.compactYuan(savingsCents),
                        Money.compactYuan(goalAmountCents),
                    )
                },
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun SummaryMetric(
    label: String,
    value: String,
    color: Color,
    modifier: Modifier = Modifier,
    sub: String? = null,
    alignEnd: Boolean = false,
) {
    Column(
        modifier = modifier,
        horizontalAlignment = if (alignEnd) Alignment.End else Alignment.Start,
    ) {
        Text(
            label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(4.dp))
        Text(value, style = MaterialTheme.typography.titleLarge, color = color, maxLines = 1)
        if (sub != null) {
            Spacer(Modifier.height(3.dp))
            Text(
                sub,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
            )
        }
    }
}

@Composable
private fun IncomeArrivalCard(
    periodLabel: String,
    cycleStartLabel: String,
    paydayLabel: String,
    expectedCents: Long,
    accruedCents: Double,
    daysToPayday: Long,
    landed: Boolean,
    hidden: Boolean,
    onDemoCelebration: () -> Unit,
) {
    val targetProgress = if (landed) {
        1f
    } else if (expectedCents <= 0L) {
        0f
    } else {
        (accruedCents / expectedCents).toFloat().coerceIn(0f, 1f)
    }
    val animatedProgress by animateFloatAsState(
        targetValue = targetProgress,
        animationSpec = tween(durationMillis = 1_100),
        label = "income-rail",
    )

    AnimatedContent(
        targetState = landed,
        transitionSpec = {
            (fadeIn(tween(420)) + scaleIn(initialScale = 0.985f)) togetherWith
                (fadeOut(tween(220)) + scaleOut(targetScale = 0.995f))
        },
        label = "income-arrival",
    ) { isLanded ->
        IncomeArrivalSurface(
            landed = isLanded,
            periodLabel = periodLabel,
            cycleStartLabel = cycleStartLabel,
            paydayLabel = paydayLabel,
            expectedCents = expectedCents,
            accruedCents = accruedCents,
            daysToPayday = daysToPayday,
            hidden = hidden,
            railProgress = animatedProgress,
            onDemoCelebration = onDemoCelebration,
        )
    }
}

@Composable
private fun IncomeArrivalSurface(
    landed: Boolean,
    periodLabel: String,
    cycleStartLabel: String,
    paydayLabel: String,
    expectedCents: Long,
    accruedCents: Double,
    daysToPayday: Long,
    hidden: Boolean,
    railProgress: Float,
    onDemoCelebration: () -> Unit,
) {
    val shape = RoundedCornerShape(20.dp)
    val accent = if (landed) BrandColors.savings else BrandColors.income
    val container = if (landed) {
        BrandColors.savings.copy(alpha = 0.055f)
    } else {
        MaterialTheme.colorScheme.surface
    }

    if (landed) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = shape,
            colors = CardDefaults.cardColors(containerColor = container),
            border = BorderStroke(1.dp, accent.copy(alpha = 0.6f)),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        ) {
            IncomeArrivalBody(
                landed = true,
                periodLabel = periodLabel,
                cycleStartLabel = cycleStartLabel,
                paydayLabel = paydayLabel,
                expectedCents = expectedCents,
                accruedCents = accruedCents,
                daysToPayday = daysToPayday,
                hidden = hidden,
                railProgress = railProgress,
                onDemoCelebration = onDemoCelebration,
            )
        }
    } else {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(shape)
                .background(container)
                .dashedBorder(
                    color = accent.copy(alpha = 0.68f),
                    width = 1.dp,
                    cornerRadius = 20.dp,
                    dashLength = 8.dp,
                    gapLength = 6.dp,
                ),
        ) {
            IncomeArrivalBody(
                landed = false,
                periodLabel = periodLabel,
                cycleStartLabel = cycleStartLabel,
                paydayLabel = paydayLabel,
                expectedCents = expectedCents,
                accruedCents = accruedCents,
                daysToPayday = daysToPayday,
                hidden = hidden,
                railProgress = railProgress,
                onDemoCelebration = onDemoCelebration,
            )
        }
    }
}

@Composable
private fun IncomeArrivalBody(
    landed: Boolean,
    periodLabel: String,
    cycleStartLabel: String,
    paydayLabel: String,
    expectedCents: Long,
    accruedCents: Double,
    daysToPayday: Long,
    hidden: Boolean,
    railProgress: Float,
    onDemoCelebration: () -> Unit,
) {
    val accent = if (landed) BrandColors.savings else BrandColors.income

    Column(Modifier.padding(18.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top,
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    stringResource(
                        if (landed) {
                            R.string.income_arrival_landed
                        } else {
                            R.string.income_arrival_pending
                        },
                    ),
                    style = MaterialTheme.typography.titleLarge,
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    stringResource(
                        if (landed) {
                            R.string.income_arrival_landed_subtitle
                        } else {
                            R.string.income_arrival_pending_subtitle
                        },
                    ),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            StatusChip(
                text = when {
                    landed -> stringResource(R.string.income_arrival_landed_chip)
                    daysToPayday <= 0L -> stringResource(R.string.income_arrival_today_chip)
                    else -> stringResource(R.string.income_arrival_days_chip, daysToPayday)
                },
                containerColor = if (landed) {
                    MaterialTheme.colorScheme.primaryContainer
                } else {
                    MaterialTheme.colorScheme.tertiaryContainer
                },
                contentColor = if (landed) {
                    MaterialTheme.colorScheme.onPrimaryContainer
                } else {
                    MaterialTheme.colorScheme.onTertiaryContainer
                },
            )
        }

        Spacer(Modifier.height(18.dp))
        Text(
            stringResource(
                if (landed) {
                    R.string.income_arrival_this_cycle
                } else {
                    R.string.income_arrival_expected
                },
            ),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        AnimatedMoneyText(
            cents = expectedCents.toDouble(),
            hidden = hidden,
            style = MaterialTheme.typography.displayMedium,
            color = if (landed) BrandColors.savings else MaterialTheme.colorScheme.onSurface,
        )
        Spacer(Modifier.height(3.dp))
        Text(
            if (landed) {
                stringResource(R.string.income_arrival_period_landed, periodLabel)
            } else {
                stringResource(R.string.income_arrival_period_pending, periodLabel, paydayLabel)
            },
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Spacer(Modifier.height(20.dp))
        IncomeRail(landed = landed, progress = railProgress, accent = accent)
        Spacer(Modifier.height(9.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                stringResource(R.string.income_arrival_cycle_start),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                cycleStartLabel,
                style = MaterialTheme.typography.labelSmall,
                color = accent,
                fontWeight = FontWeight.SemiBold,
            )
        }

        Spacer(Modifier.height(18.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            IncomeMetric(
                label = stringResource(
                    if (landed) {
                        R.string.income_arrival_this_cycle
                    } else {
                        R.string.income_arrival_accrued
                    },
                ),
                value = if (hidden) {
                    stringResource(R.string.amount_hidden_long)
                } else {
                    "¥${Money.centsToYuan(accruedCents)}"
                },
                color = accent,
            )
            IncomeMetric(
                label = stringResource(R.string.income_arrival_amount),
                value = if (hidden) {
                    stringResource(R.string.amount_hidden_long)
                } else {
                    "¥${Money.centsToYuan(expectedCents)}"
                },
                color = BrandColors.savings,
                alignEnd = true,
            )
        }

        Spacer(Modifier.height(14.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (landed) {
                Icon(
                    Icons.Rounded.CheckCircle,
                    contentDescription = null,
                    tint = BrandColors.savings,
                    modifier = Modifier.size(18.dp),
                )
                Spacer(Modifier.width(7.dp))
                Text(
                    stringResource(R.string.income_arrival_fully_saved),
                    style = MaterialTheme.typography.labelMedium,
                    color = BrandColors.savings,
                    modifier = Modifier.weight(1f),
                )
            } else {
                Text(
                    stringResource(R.string.income_arrival_auto_solid),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.weight(1f))
            }
            Text(
                stringResource(R.string.income_arrival_preview),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .clickable(onClick = onDemoCelebration)
                    .padding(horizontal = 9.dp, vertical = 8.dp),
            )
        }
    }
}

@Composable
private fun IncomeRail(
    landed: Boolean,
    progress: Float,
    accent: Color,
) {
    val inactiveColor = MaterialTheme.colorScheme.outlineVariant
    val surfaceColor = MaterialTheme.colorScheme.surface

    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(22.dp),
    ) {
        val edge = 6.dp.toPx()
        val startX = edge
        val endX = size.width - edge
        val centerY = size.height / 2f
        val activeEndX = startX + (endX - startX) * progress.coerceIn(0f, 1f)
        val dash = PathEffect.dashPathEffect(
            intervals = floatArrayOf(6.dp.toPx(), 5.dp.toPx()),
            phase = 0f,
        )

        drawLine(
            color = inactiveColor,
            start = Offset(startX, centerY),
            end = Offset(endX, centerY),
            strokeWidth = 2.dp.toPx(),
            pathEffect = dash,
            cap = StrokeCap.Round,
        )
        if (progress > 0f) {
            drawLine(
                color = accent,
                start = Offset(startX, centerY),
                end = Offset(activeEndX, centerY),
                strokeWidth = 4.dp.toPx(),
                cap = StrokeCap.Round,
            )
        }
        drawCircle(color = accent, radius = 5.dp.toPx(), center = Offset(startX, centerY))
        drawCircle(
            color = if (landed) accent else surfaceColor,
            radius = 6.dp.toPx(),
            center = Offset(endX, centerY),
        )
        drawCircle(
            color = if (landed) accent else inactiveColor,
            radius = 5.dp.toPx(),
            center = Offset(endX, centerY),
        )
    }
}

@Composable
private fun IncomeMetric(
    label: String,
    value: String,
    color: Color,
    alignEnd: Boolean = false,
) {
    Column(horizontalAlignment = if (alignEnd) Alignment.End else Alignment.Start) {
        Text(
            label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(3.dp))
        Text(value, style = MaterialTheme.typography.titleLarge, color = color)
    }
}

private fun Modifier.dashedBorder(
    color: Color,
    width: Dp,
    cornerRadius: Dp,
    dashLength: Dp,
    gapLength: Dp,
): Modifier = drawWithContent {
    drawContent()
    val strokeWidth = width.toPx()
    val inset = strokeWidth / 2f
    val border = Path().apply {
        addRoundRect(
            RoundRect(
                rect = Rect(
                    left = inset,
                    top = inset,
                    right = size.width - inset,
                    bottom = size.height - inset,
                ),
                cornerRadius = CornerRadius(cornerRadius.toPx()),
            ),
        )
    }
    drawPath(
        path = border,
        color = color,
        style = Stroke(
            width = strokeWidth,
            pathEffect = PathEffect.dashPathEffect(
                intervals = floatArrayOf(dashLength.toPx(), gapLength.toPx()),
            ),
        ),
    )
}

@Composable
private fun statusText(status: WorkStatus, paused: Boolean): String = when {
    paused -> stringResource(R.string.status_paused)
    status == WorkStatus.WORKING -> stringResource(R.string.status_working)
    status == WorkStatus.UNPAID_BREAK -> stringResource(R.string.status_break)
    status == WorkStatus.BEFORE_WORK -> stringResource(R.string.status_before_work)
    status == WorkStatus.FINISHED -> stringResource(R.string.status_finished)
    else -> stringResource(R.string.status_rest_day)
}

@Composable
private fun statusLabel(status: WorkStatus): String = when (status) {
    WorkStatus.WORKING -> stringResource(R.string.home_today_earned)
    WorkStatus.UNPAID_BREAK -> stringResource(R.string.home_unpaid_break)
    WorkStatus.BEFORE_WORK -> stringResource(R.string.home_today_not_started)
    WorkStatus.FINISHED -> stringResource(R.string.home_today_finished)
    WorkStatus.REST_DAY -> stringResource(R.string.home_day_off)
}
