package com.salaryflow.app.core

import java.time.DayOfWeek
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.YearMonth
import kotlin.math.max
import kotlin.math.min

val WEEKDAYS: Set<DayOfWeek> = setOf(
    DayOfWeek.MONDAY,
    DayOfWeek.TUESDAY,
    DayOfWeek.WEDNESDAY,
    DayOfWeek.THURSDAY,
    DayOfWeek.FRIDAY,
)

enum class WorkStatus {
    REST_DAY,
    BEFORE_WORK,
    WORKING,
    UNPAID_BREAK,
    FINISHED,
}

/**
 * 计薪规则。默认值参考国内常见的「21.75 天计薪 + 工作日 9:00-18:00 + 午休不计薪」。
 */
data class WorkSchedule(
    val workDays: Set<DayOfWeek> = WEEKDAYS,
    val startTime: LocalTime = LocalTime.of(9, 0),
    val endTime: LocalTime = LocalTime.of(18, 0),
    val lunchStart: LocalTime = LocalTime.of(12, 0),
    val lunchEnd: LocalTime = LocalTime.of(13, 0),
    val lunchPaid: Boolean = false,
    val dinnerEnabled: Boolean = false,
    val dinnerStart: LocalTime = LocalTime.of(18, 0),
    val dinnerEnd: LocalTime = LocalTime.of(18, 30),
    val dinnerPaid: Boolean = false,
) {
    /** 午休必须落在上下班区间内且时长大于 0 才生效。 */
    val lunchEnabled: Boolean
        get() = lunchEnd > lunchStart && lunchStart >= startTime && lunchEnd <= endTime

    /** 晚餐休息仅在开启、位于上下班区间内且时长大于 0 时生效。 */
    val dinnerActive: Boolean
        get() = dinnerEnabled &&
            dinnerEnd > dinnerStart &&
            dinnerStart >= startTime &&
            dinnerEnd <= endTime

    val paidSegments: List<Pair<LocalTime, LocalTime>>
        get() {
            val unpaidBreaks = buildList {
                if (!lunchPaid && lunchEnabled) add(lunchStart to lunchEnd)
                if (!dinnerPaid && dinnerActive) add(dinnerStart to dinnerEnd)
            }
            if (unpaidBreaks.isEmpty()) return listOf(startTime to endTime)

            val mergedBreaks = mutableListOf<Pair<LocalTime, LocalTime>>()
            unpaidBreaks.sortedBy { it.first }.forEach { breakRange ->
                val previous = mergedBreaks.lastOrNull()
                if (previous == null || breakRange.first > previous.second) {
                    mergedBreaks += breakRange
                } else {
                    mergedBreaks[mergedBreaks.lastIndex] =
                        previous.first to maxOf(previous.second, breakRange.second)
                }
            }

            val segments = mutableListOf<Pair<LocalTime, LocalTime>>()
            var cursor = startTime
            mergedBreaks.forEach { (breakStart, breakEnd) ->
                if (breakStart > cursor) segments += cursor to breakStart
                if (breakEnd > cursor) cursor = breakEnd
            }
            if (cursor < endTime) segments += cursor to endTime
            return segments
        }

    val paidSecondsPerDay: Long
        get() = paidSegments.sumOf { (from, to) -> Duration.between(from, to).seconds }

    fun isWorkDay(date: LocalDate): Boolean = date.dayOfWeek in workDays
}

data class SalaryConfig(
    val monthlySalaryCents: Long = 1_000_000L,
    val paidDaysPerMonth: Double = 21.75,
    val payday: Int = 15,
    val savingsRatePercent: Int = 20,
    val schedule: WorkSchedule = WorkSchedule(),
) {
    val savingsRate: Double get() = savingsRatePercent.coerceIn(0, 100) / 100.0
}

data class SettlementPeriod(
    val start: LocalDate,
    val end: LocalDate,
) {
    /** 结算去重用的稳定 key。 */
    val key: String get() = end.toString()
}

data class DayMilestone(
    val time: LocalTime,
)

/**
 * 把「月薪 + 计薪规则」换算成实时金额的纯函数引擎。
 *
 * 单位统一使用「分」的 [Double] 形式：实时跳动需要小数精度，
 * 真正落账时再四舍五入成 [Long]。
 */
class SalaryClock(val config: SalaryConfig) {

    val paidSecondsPerDay: Long get() = config.schedule.paidSecondsPerDay

    val dailyPayCents: Double
        get() = if (config.paidDaysPerMonth <= 0) 0.0 else config.monthlySalaryCents / config.paidDaysPerMonth

    val centsPerSecond: Double
        get() = if (paidSecondsPerDay <= 0) 0.0 else dailyPayCents / paidSecondsPerDay

    fun status(now: LocalDateTime): WorkStatus {
        if (!config.schedule.isWorkDay(now.toLocalDate())) return WorkStatus.REST_DAY
        val time = now.toLocalTime()
        val schedule = config.schedule
        return when {
            time < schedule.startTime -> WorkStatus.BEFORE_WORK
            time >= schedule.endTime -> WorkStatus.FINISHED
            schedule.lunchEnabled && !schedule.lunchPaid &&
                time >= schedule.lunchStart && time < schedule.lunchEnd -> WorkStatus.UNPAID_BREAK
            schedule.dinnerActive && !schedule.dinnerPaid &&
                time >= schedule.dinnerStart && time < schedule.dinnerEnd -> WorkStatus.UNPAID_BREAK
            else -> WorkStatus.WORKING
        }
    }

    /** 返回区间内的计薪秒数，保留毫秒精度。 */
    fun paidSecondsBetween(from: LocalDateTime, to: LocalDateTime): Double {
        if (!to.isAfter(from)) return 0.0
        var total = 0.0
        var date = from.toLocalDate()
        val lastDate = to.toLocalDate()
        while (!date.isAfter(lastDate)) {
            if (config.schedule.isWorkDay(date)) {
                for ((segmentStart, segmentEnd) in config.schedule.paidSegments) {
                    val start = maxOf(from, date.atTime(segmentStart))
                    val end = minOf(to, date.atTime(segmentEnd))
                    if (end.isAfter(start)) {
                        total += Duration.between(start, end).toMillis() / 1000.0
                    }
                }
            }
            date = date.plusDays(1)
        }
        return total
    }

    fun earnedCentsBetween(from: LocalDateTime, to: LocalDateTime): Double =
        paidSecondsBetween(from, to) * centsPerSecond

    fun earnedToday(now: LocalDateTime): Double =
        earnedCentsBetween(now.toLocalDate().atStartOfDay(), now)

    /** 今天已挣金额在计薪时段内的进度，0f - 1f。 */
    fun dayProgress(now: LocalDateTime): Float {
        val schedule = config.schedule
        val dayStart = schedule.startTime.toSecondOfDay().toDouble()
        val dayEnd = schedule.endTime.toSecondOfDay().toDouble()
        if (dayEnd <= dayStart) return 0f
        val current = now.toLocalTime().toSecondOfDay().toDouble()
        return ((current - dayStart) / (dayEnd - dayStart)).toFloat().coerceIn(0f, 1f)
    }

    fun timeUntilOffWork(now: LocalDateTime): Duration {
        if (!config.schedule.isWorkDay(now.toLocalDate())) return Duration.ZERO
        val offWork = now.toLocalDate().atTime(config.schedule.endTime)
        return if (offWork.isAfter(now)) Duration.between(now, offWork) else Duration.ZERO
    }

    fun milestones(): List<DayMilestone> {
        val schedule = config.schedule
        val list = mutableListOf<DayMilestone>()
        if (schedule.lunchEnabled) {
            list += DayMilestone(schedule.lunchStart)
        }
        if (schedule.dinnerActive) {
            list += DayMilestone(schedule.dinnerStart)
        }
        val five = LocalTime.of(17, 0)
        if (five > schedule.startTime && five < schedule.endTime &&
            Duration.between(five, schedule.endTime).toMinutes() >= 30
        ) {
            list += DayMilestone(five)
        }
        list += DayMilestone(schedule.endTime)
        return list.distinct().sortedBy(DayMilestone::time)
    }

    // ---------- 发薪日 ----------

    fun paydayOf(month: YearMonth): LocalDate =
        month.atDay(config.payday.coerceIn(1, month.lengthOfMonth()))

    /** 严格晚于 [date] 的最近发薪日。 */
    fun nextPaydayAfter(date: LocalDate): LocalDate {
        val thisMonth = paydayOf(YearMonth.from(date))
        return if (thisMonth.isAfter(date)) thisMonth else paydayOf(YearMonth.from(date).plusMonths(1))
    }

    /** 不晚于 [date] 的最近发薪日。 */
    fun paydayOnOrBefore(date: LocalDate): LocalDate {
        val thisMonth = paydayOf(YearMonth.from(date))
        return if (!thisMonth.isAfter(date)) thisMonth else paydayOf(YearMonth.from(date).minusMonths(1))
    }

    /** 距离下一个发薪日的天数，发薪日当天为 0。 */
    fun daysToPayday(from: LocalDate): Long {
        val next = nextPaydayAfter(from.minusDays(1))
        return java.time.temporal.ChronoUnit.DAYS.between(from, next)
    }

    /** 正在累积、尚未结算的工资周期。 */
    fun accruingPeriod(now: LocalDateTime): SettlementPeriod {
        val date = now.toLocalDate()
        val end = nextPaydayAfter(date)
        val start = paydayOf(YearMonth.from(end).minusMonths(1)).plusDays(1)
        return SettlementPeriod(start, end)
    }

    /**
     * 需要补结算的周期，按时间升序返回。
     * [lastSettledEnd] 为 null 时只返回最近一期，避免首次使用时把历史月份全部补记。
     */
    fun duePeriods(lastSettledEnd: LocalDate?, now: LocalDateTime, limit: Int = 24): List<SettlementPeriod> {
        val latest = paydayOnOrBefore(now.toLocalDate())
        val periods = mutableListOf<SettlementPeriod>()
        var cursor = latest
        var guard = 0
        while (guard++ < limit + 1) {
            if (lastSettledEnd != null && !cursor.isAfter(lastSettledEnd)) break
            periods += periodEndingAt(cursor)
            cursor = paydayOf(YearMonth.from(cursor).minusMonths(1))
        }
        val ordered = periods.asReversed()
        return if (lastSettledEnd == null) ordered.takeLast(1) else ordered.takeLast(limit)
    }

    fun periodEndingAt(end: LocalDate): SettlementPeriod {
        val start = paydayOf(YearMonth.from(end).minusMonths(1)).plusDays(1)
        return SettlementPeriod(start, end)
    }

    /** 周期内到 [now] 为止已经「挣到」的虚拟金额。 */
    fun accruedCentsIn(period: SettlementPeriod, now: LocalDateTime): Double {
        val from = period.start.atStartOfDay()
        val periodEndExclusive = period.end.plusDays(1).atStartOfDay()
        val to = minOf(now, periodEndExclusive)
        return earnedCentsBetween(from, to)
    }
}
