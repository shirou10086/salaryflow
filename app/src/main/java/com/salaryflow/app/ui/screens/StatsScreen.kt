package com.salaryflow.app.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.salaryflow.app.core.Money
import com.salaryflow.app.data.Categories
import com.salaryflow.app.ui.MonthPoint
import com.salaryflow.app.ui.SalaryFlowViewModel
import com.salaryflow.app.ui.components.CategoryBadge
import com.salaryflow.app.ui.components.SectionCard
import com.salaryflow.app.ui.components.SectionHeader
import com.salaryflow.app.ui.theme.BrandColors
import java.time.YearMonth

private val donutPalette = listOf(
    Color(0xFF26775F),
    Color(0xFFC5573F),
    Color(0xFFB58437),
    Color(0xFF567B8F),
    Color(0xFF7E8870),
    Color(0xFF9A6652),
    Color(0xFF4D8C80),
    Color(0xFF8B7C68),
    Color(0xFF74766F),
)

@Composable
fun StatsScreen(
    viewModel: SalaryFlowViewModel,
    modifier: Modifier = Modifier,
) {
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val month by viewModel.currentMonth.collectAsStateWithLifecycle()
    val expense by viewModel.monthExpenseCents.collectAsStateWithLifecycle()
    val income by viewModel.monthIncomeCents.collectAsStateWithLifecycle()
    val saving by viewModel.monthSavingCents.collectAsStateWithLifecycle()
    val categories by viewModel.monthCategoryTotals.collectAsStateWithLifecycle()
    val recentMonths by viewModel.recentMonths.collectAsStateWithLifecycle()
    val savingsSeries by viewModel.savingsSeries.collectAsStateWithLifecycle()
    val savings by viewModel.savingsCents.collectAsStateWithLifecycle()

    val hidden = settings.hideAmounts

    LazyColumn(
        modifier = modifier.fillMaxWidth(),
        contentPadding = PaddingValues(start = 18.dp, end = 18.dp, top = 10.dp, bottom = 120.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item {
            Column {
                Text("统计", style = MaterialTheme.typography.headlineSmall)
                Text(
                    "${month.year} 年 ${month.monthValue} 月 · 数据来自本机账本",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        item {
            SectionCard {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    StatTile("本月支出", expense, hidden, BrandColors.expense)
                    StatTile("本月收入", income, hidden, BrandColors.savings)
                    StatTile("存入攒钱罐", saving, hidden, BrandColors.savings)
                }
                Spacer(Modifier.height(14.dp))
                Text(
                    buildString {
                        append("收支结余 ")
                        append(if (hidden) "¥•••" else "¥${Money.centsToYuan(income - expense)}")
                        append("，攒钱罐余额 ")
                        append(if (hidden) "¥•••" else "¥${Money.centsToYuan(savings)}")
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        item {
            SectionCard {
                SectionHeader(title = "近 6 个月支出", subtitle = "按月汇总的手动与自动记账")
                Spacer(Modifier.height(16.dp))
                BarChart(
                    points = recentMonths,
                    hidden = hidden,
                )
            }
        }

        item {
            SectionCard {
                SectionHeader(title = "支出分类", subtitle = "看看钱都花在哪了")
                Spacer(Modifier.height(16.dp))
                if (categories.isEmpty()) {
                    Text(
                        "本月还没有支出记录。",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                } else {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        DonutChart(
                            values = categories.map { it.totalCents },
                            modifier = Modifier.size(132.dp),
                        )
                        Spacer(Modifier.width(18.dp))
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            categories.take(5).forEachIndexed { index, total ->
                                val category = Categories.find(total.category)
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    CategoryBadge(
                                        categoryName = category.name,
                                        size = 26,
                                        contentColor = donutPalette[index % donutPalette.size],
                                    )
                                    Spacer(Modifier.width(8.dp))
                                    Text(
                                        category.name,
                                        style = MaterialTheme.typography.labelLarge,
                                        modifier = Modifier.weight(1f),
                                    )
                                    Text(
                                        if (hidden) "¥•••" else "¥${Money.compactYuan(total.totalCents)}",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        item {
            SectionCard {
                SectionHeader(
                    title = "攒钱罐走势",
                    subtitle = "${month.monthValue} 月每日余额",
                )
                Spacer(Modifier.height(16.dp))
                SavingsLineChart(series = savingsSeries)
            }
        }

        item {
            SectionCard {
                SectionHeader(title = "发薪日结算记录", subtitle = "自动寄存的每一笔都在账本里")
                Spacer(Modifier.height(10.dp))
                Text(
                    buildString {
                        append("攒钱比例 ")
                        append("${settings.savingsRatePercent}%")
                        append("，发薪日按固定月薪结算：每次 ")
                        append(if (hidden) "¥•••" else "¥${Money.centsToYuan(Money.ratioToCents(settings.savingsRate, settings.monthlySalaryCents))}")
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun StatTile(
    label: String,
    cents: Long,
    hidden: Boolean,
    color: Color,
) {
    Column {
        Text(
            label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(4.dp))
        Text(
            if (hidden) "¥•••" else "¥${Money.compactYuan(cents)}",
            style = MaterialTheme.typography.titleLarge,
            color = color,
        )
    }
}

@Composable
private fun BarChart(points: List<MonthPoint>, hidden: Boolean) {
    if (points.isEmpty()) {
        Spacer(Modifier.height(120.dp))
        return
    }
    val maxValue = remember(points) { points.maxOfOrNull { it.expenseCents }?.coerceAtLeast(1L) ?: 1L }
    val barColor = BrandColors.expense
    val gridColor = MaterialTheme.colorScheme.outlineVariant
    val labelStyle = MaterialTheme.typography.labelSmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)

    Column {
        Canvas(modifier = Modifier.fillMaxWidth().height(160.dp)) {
            val bottom = size.height - 26f
            val chartHeight = bottom
            val slot = size.width / points.size
            val barWidth = slot * 0.42f

            repeat(4) { index ->
                val y = chartHeight * index / 3f
                drawLine(
                    color = gridColor,
                    start = Offset(0f, y),
                    end = Offset(size.width, y),
                    strokeWidth = 2f,
                )
            }

            points.forEachIndexed { index, point ->
                val ratio = point.expenseCents.toFloat() / maxValue
                val barHeight = (chartHeight * ratio).coerceAtLeast(if (point.expenseCents > 0) 6f else 0f)
                val left = slot * index + (slot - barWidth) / 2f
                drawRoundRect(
                    brush = Brush.verticalGradient(listOf(barColor, barColor.copy(alpha = 0.55f))),
                    topLeft = Offset(left, bottom - barHeight),
                    size = Size(barWidth, barHeight),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(10f, 10f),
                )
            }
        }
        Row(modifier = Modifier.fillMaxWidth()) {
            points.forEach { point ->
                Text(
                    "${point.month.monthValue}月",
                    style = labelStyle,
                    modifier = Modifier.weight(1f),
                )
            }
        }
        Spacer(Modifier.height(4.dp))
        Text(
            if (hidden) {
                "最近一个月支出 ¥•••"
            } else {
                "最近一个月支出 ¥${Money.centsToYuan(points.last().expenseCents)}"
            },
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun DonutChart(values: List<Long>, modifier: Modifier = Modifier) {
    val total = values.sum().coerceAtLeast(1L)
    val colors = donutPalette
    Canvas(modifier = modifier) {
        val stroke = size.minDimension * 0.22f
        var startAngle = -90f
        values.forEachIndexed { index, value ->
            val sweep = 360f * value / total
            drawArc(
                color = colors[index % colors.size],
                startAngle = startAngle,
                sweepAngle = sweep - 2f,
                useCenter = false,
                style = Stroke(width = stroke, cap = StrokeCap.Round),
                size = Size(size.width - stroke, size.height - stroke),
                topLeft = Offset(stroke / 2f, stroke / 2f),
            )
            startAngle += sweep
        }
    }
}

@Composable
private fun SavingsLineChart(series: List<Long>) {
    if (series.size < 2) {
        Spacer(Modifier.height(120.dp))
        return
    }
    val maxValue = (series.maxOrNull() ?: 0L).coerceAtLeast(1L).toFloat()
    val minValue = (series.minOrNull() ?: 0L).toFloat()
    val range = (maxValue - minValue).coerceAtLeast(1f)
    val lineColor = BrandColors.savings

    Column {
        Canvas(modifier = Modifier.fillMaxWidth().height(140.dp)) {
            val path = Path()
            val fill = Path()
            series.forEachIndexed { index, value ->
                val x = size.width * index / (series.size - 1).toFloat()
                val y = size.height - (value - minValue) / range * size.height * 0.86f - size.height * 0.07f
                if (index == 0) {
                    path.moveTo(x, y)
                    fill.moveTo(x, size.height)
                    fill.lineTo(x, y)
                } else {
                    path.lineTo(x, y)
                    fill.lineTo(x, y)
                }
            }
            fill.lineTo(size.width, size.height)
            fill.close()
            drawPath(
                path = fill,
                brush = Brush.verticalGradient(
                    listOf(lineColor.copy(alpha = 0.35f), Color.Transparent),
                ),
            )
            drawPath(path = path, color = lineColor, style = Stroke(width = 5f, cap = StrokeCap.Round))
            drawCircle(
                color = lineColor,
                radius = 7f,
                center = Offset(size.width, size.height - (series.last() - minValue) / range * size.height * 0.86f - size.height * 0.07f),
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text("月初", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(
                "月末 ¥${Money.compactYuan(series.last())}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
