package com.salaryflow.app.core

import java.time.Duration
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.math.BigDecimal
import java.math.RoundingMode
import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToLong

object Money {

    fun centsToYuan(cents: Long): String = centsToYuan(cents.toDouble())

    fun centsToYuan(cents: Double): String =
        String.format(Locale.CHINA, "%,.2f", cents / 100.0)

    /** 每秒工资，保留 4 位小数更能体现「一直在涨」。 */
    fun centsToYuanHighPrecision(cents: Double): String =
        String.format(Locale.CHINA, "%,.4f", cents / 100.0)

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

    fun compactYuan(cents: Long): String {
        val yuan = cents / 100.0
        val absolute = abs(yuan)
        return when {
            absolute >= 100_000_000 -> String.format(Locale.CHINA, "%.2f亿", yuan / 100_000_000)
            absolute >= 10_000 -> String.format(Locale.CHINA, "%.2f万", yuan / 10_000)
            else -> String.format(Locale.CHINA, "%,.2f", yuan)
        }
    }
}

object TimeText {
    private val dayFormatter = DateTimeFormatter.ofPattern("M月d日")
    private val fullFormatter = DateTimeFormatter.ofPattern("yyyy年M月d日")
    private val timeFormatter = DateTimeFormatter.ofPattern("HH:mm")

    val weekNames = arrayOf("周一", "周二", "周三", "周四", "周五", "周六", "周日")

    fun day(date: LocalDate): String = date.format(dayFormatter)
    fun fullDay(date: LocalDate): String = date.format(fullFormatter)
    fun clock(time: java.time.LocalTime): String = time.format(timeFormatter)

    /** 02:13:20 这种倒计时格式。 */
    fun countdown(duration: Duration): String {
        val total = duration.seconds.coerceAtLeast(0)
        val hours = total / 3600
        val minutes = total % 3600 / 60
        val seconds = total % 60
        return String.format(Locale.CHINA, "%02d:%02d:%02d", hours, minutes, seconds)
    }

    fun hhmmss(millis: Long): String = countdown(Duration.ofMillis(millis.coerceAtLeast(0)))

    fun humanDuration(seconds: Long): String {
        val safe = seconds.coerceAtLeast(0)
        val hours = safe / 3600
        val minutes = safe % 3600 / 60
        return when {
            hours > 0 && minutes > 0 -> "${hours}小时${minutes}分"
            hours > 0 -> "${hours}小时"
            minutes > 0 -> "${minutes}分"
            else -> "${safe}秒"
        }
    }
}

fun Long.toLocalDate(zoneId: ZoneId = ZoneId.systemDefault()): LocalDate =
    java.time.Instant.ofEpochMilli(this).atZone(zoneId).toLocalDate()

fun Long.toYearMonth(zoneId: ZoneId = ZoneId.systemDefault()): YearMonth =
    YearMonth.from(toLocalDate(zoneId))
