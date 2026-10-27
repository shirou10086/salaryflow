package com.salaryflow.app.core

import java.time.Duration
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.time.temporal.ChronoUnit
import java.math.BigDecimal
import java.math.RoundingMode
import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToLong

object Money {

    fun centsToYuan(cents: Long): String = centsToYuan(cents.toDouble())

    fun centsToYuan(cents: Double): String =
        String.format(Locale.getDefault(), "%,.2f", cents / 100.0)

    /** 每秒工资，保留 4 位小数更能体现「一直在涨」。 */
    fun centsToYuanHighPrecision(cents: Double): String =
        String.format(Locale.getDefault(), "%,.4f", cents / 100.0)

    fun yuanToCents(input: String): Long? {
        val cleaned = input.trim().replace(",", "").replace("¥", "")
        if (cleaned.isEmpty()) return null
        val value = cleaned.toBigDecimalOrNull() ?: return null
        if (value.signum() < 0) return null
        return runCatching {
            value.movePointRight(2)
                .setScale(0, RoundingMode.HALF_UP)
                .longValueExact()
        }.getOrNull()
    }

    fun ratioToCents(ratio: Double, cents: Long): Long =
        (cents * ratio.coerceIn(0.0, 1.0)).roundToLong()

    fun compactYuan(cents: Long, locale: Locale = Locale.getDefault()): String {
        val yuan = cents / 100.0
        val absolute = abs(yuan)
        if (locale.language.startsWith("zh")) {
            return when {
                absolute >= 100_000_000 -> String.format(locale, "%.2f亿", yuan / 100_000_000)
                absolute >= 10_000 -> String.format(locale, "%.2f万", yuan / 10_000)
                else -> String.format(locale, "%,.2f", yuan)
            }
        }
        return when {
            absolute >= 1_000_000_000 -> String.format(locale, "%.2fB", yuan / 1_000_000_000)
            absolute >= 1_000_000 -> String.format(locale, "%.2fM", yuan / 1_000_000)
            absolute >= 1_000 -> String.format(locale, "%.2fK", yuan / 1_000)
            else -> String.format(locale, "%,.2f", yuan)
        }
    }
}

object TimeText {
    fun day(date: LocalDate, locale: Locale = Locale.getDefault()): String {
        val pattern = if (locale.language.startsWith("zh")) "M月d日" else "MMM d"
        return date.format(DateTimeFormatter.ofPattern(pattern, locale))
    }

    fun fullDay(date: LocalDate, locale: Locale = Locale.getDefault()): String {
        val pattern = if (locale.language.startsWith("zh")) "yyyy年M月d日" else "MMM d, yyyy"
        return date.format(DateTimeFormatter.ofPattern(pattern, locale))
    }

    /** 今天 / 昨天 / 前天；更早的日期返回 null，由调用方展示具体日期。 */
    fun relativeDayLabel(
        date: LocalDate,
        today: LocalDate = LocalDate.now(),
        locale: Locale = Locale.getDefault(),
    ): String? {
        val chinese = locale.language.startsWith("zh")
        return when (ChronoUnit.DAYS.between(date, today)) {
            0L -> if (chinese) "今天" else "Today"
            1L -> if (chinese) "昨天" else "Yesterday"
            2L -> if (chinese) "前天" else "Day before yesterday"
            else -> null
        }
    }

    /** 同一年只显示月日，跨年时补上年份。 */
    fun dayWithYear(
        date: LocalDate,
        today: LocalDate = LocalDate.now(),
        locale: Locale = Locale.getDefault(),
    ): String = if (date.year == today.year) day(date, locale) else fullDay(date, locale)

    /** 账本分组标题：最近三天用今天/昨天/前天，更早用日期。 */
    fun dayLabel(
        date: LocalDate,
        today: LocalDate = LocalDate.now(),
        locale: Locale = Locale.getDefault(),
    ): String = relativeDayLabel(date, today, locale) ?: dayWithYear(date, today, locale)

    /**
     * 流水时间戳：最近三天用「今天 12:30」这类相对写法，更早显示日期。
     */
    fun transactionTimestamp(
        epochMillis: Long,
        today: LocalDate = LocalDate.now(),
        zoneId: ZoneId = ZoneId.systemDefault(),
        locale: Locale = Locale.getDefault(),
    ): String {
        val dateTime = java.time.Instant.ofEpochMilli(epochMillis).atZone(zoneId)
        val date = dateTime.toLocalDate()
        val time = clock(dateTime.toLocalTime())
        val label = relativeDayLabel(date, today, locale) ?: dayWithYear(date, today, locale)
        return "$label $time"
    }

    fun monthYear(month: YearMonth, locale: Locale = Locale.getDefault()): String {
        val pattern = if (locale.language.startsWith("zh")) "yyyy年M月" else "MMMM yyyy"
        return month.format(DateTimeFormatter.ofPattern(pattern, locale))
    }

    fun monthShort(month: YearMonth, locale: Locale = Locale.getDefault()): String {
        val pattern = if (locale.language.startsWith("zh")) "M月" else "MMM"
        return month.format(DateTimeFormatter.ofPattern(pattern, locale))
    }

    fun weekday(dayOfWeek: DayOfWeek, locale: Locale = Locale.getDefault()): String {
        if (locale.language.startsWith("zh")) {
            return arrayOf("周一", "周二", "周三", "周四", "周五", "周六", "周日")[
                dayOfWeek.value - 1
            ]
        }
        return dayOfWeek.getDisplayName(TextStyle.SHORT, locale)
    }

    fun weekdayForPill(dayOfWeek: DayOfWeek, locale: Locale = Locale.getDefault()): String =
        if (locale.language.startsWith("zh")) {
            weekday(dayOfWeek, locale).removePrefix("周")
        } else {
            dayOfWeek.getDisplayName(TextStyle.NARROW, locale)
        }

    fun period(start: LocalDate, end: LocalDate, locale: Locale = Locale.getDefault()): String {
        val pattern = if (locale.language.startsWith("zh")) "M月d日" else "MMM d"
        val formatter = DateTimeFormatter.ofPattern(pattern, locale)
        return "${start.format(formatter)} - ${end.format(formatter)}"
    }

    fun clock(time: java.time.LocalTime): String =
        time.format(DateTimeFormatter.ofPattern("HH:mm", Locale.getDefault()))

    /** 02:13:20 这种倒计时格式。 */
    fun countdown(duration: Duration): String {
        val total = duration.seconds.coerceAtLeast(0)
        val hours = total / 3600
        val minutes = total % 3600 / 60
        val seconds = total % 60
        return String.format(Locale.getDefault(), "%02d:%02d:%02d", hours, minutes, seconds)
    }

    fun hhmmss(millis: Long): String = countdown(Duration.ofMillis(millis.coerceAtLeast(0)))

    fun humanDuration(seconds: Long): String {
        val safe = seconds.coerceAtLeast(0)
        val hours = safe / 3600
        val minutes = safe % 3600 / 60
        val chinese = Locale.getDefault().language.startsWith("zh")
        return when {
            chinese && hours > 0 && minutes > 0 -> "${hours}小时${minutes}分"
            chinese && hours > 0 -> "${hours}小时"
            chinese && minutes > 0 -> "${minutes}分"
            chinese -> "${safe}秒"
            hours > 0 && minutes > 0 -> "${hours}h ${minutes}m"
            hours > 0 -> "${hours}h"
            minutes > 0 -> "${minutes}m"
            else -> "${safe}s"
        }
    }
}

fun Long.toLocalDate(zoneId: ZoneId = ZoneId.systemDefault()): LocalDate =
    java.time.Instant.ofEpochMilli(this).atZone(zoneId).toLocalDate()

fun Long.toYearMonth(zoneId: ZoneId = ZoneId.systemDefault()): YearMonth =
    YearMonth.from(toLocalDate(zoneId))
