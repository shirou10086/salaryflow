package com.salaryflow.app.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.YearMonth

class SalaryClockTest {

    private val config = SalaryConfig(
        monthlySalaryCents = 1_000_000L, // ¥10,000
        paidDaysPerMonth = 21.75,
        payday = 15,
        savingsRatePercent = 20,
    )
    private val clock = SalaryClock(config)

    @Test
    fun `daily pay follows paid days per month`() {
        assertEquals(45_977.01, clock.dailyPayCents, 0.01)
    }

    @Test
    fun `daily paid seconds exclude unpaid lunch`() {
        // 9:00-18:00 共 9 小时，扣除 1 小时午休 => 28800 秒
        assertEquals(28_800L, clock.paidSecondsPerDay)
    }

    @Test
    fun `per second pay is daily pay divided by paid seconds`() {
        assertEquals(1.5964, clock.centsPerSecond, 0.0001)
    }

    @Test
    fun `paid seconds between skips lunch and non working days`() {
        val monday = LocalDate.of(2026, 10, 19)
        val from = monday.atTime(11, 30)
        val to = monday.atTime(14, 0)

        // 11:30-12:00 = 1800s, 13:00-14:00 = 3600s
        assertEquals(5_400.0, clock.paidSecondsBetween(from, to), 0.001)

        val saturday = LocalDate.of(2026, 10, 17)
        assertEquals(
            0.0,
            clock.paidSecondsBetween(saturday.atTime(9, 0), saturday.atTime(18, 0)),
            0.001,
        )
    }

    @Test
    fun `earned today accrues only inside paid windows`() {
        val monday = LocalDate.of(2026, 10, 19)
        val morning = clock.earnedToday(monday.atTime(10, 0))
        val beforeWork = clock.earnedToday(monday.atTime(8, 30))
        val afterWork = clock.earnedToday(monday.atTime(23, 0))

        assertEquals(0.0, beforeWork, 0.001)
        assertEquals(clock.centsPerSecond * 3600, morning, 0.01)
        assertEquals(clock.dailyPayCents, afterWork, 0.01)
        assertTrue(afterWork > morning)
    }

    @Test
    fun `status reflects schedule`() {
        val monday = LocalDate.of(2026, 10, 19)
        assertEquals(WorkStatus.BEFORE_WORK, clock.status(monday.atTime(8, 0)))
        assertEquals(WorkStatus.WORKING, clock.status(monday.atTime(10, 0)))
        assertEquals(WorkStatus.UNPAID_BREAK, clock.status(monday.atTime(12, 30)))
        assertEquals(WorkStatus.FINISHED, clock.status(monday.atTime(18, 30)))
        assertEquals(
            WorkStatus.REST_DAY,
            clock.status(LocalDate.of(2026, 10, 18).atTime(10, 0)),
        )
    }

    @Test
    fun `paid lunch keeps the whole day paid`() {
        val paidLunchClock = SalaryClock(
            config.copy(schedule = config.schedule.copy(lunchPaid = true)),
        )
        assertEquals(32_400L, paidLunchClock.paidSecondsPerDay)
        assertEquals(WorkStatus.WORKING, paidLunchClock.status(LocalDate.of(2026, 10, 19).atTime(12, 30)))
    }

    @Test
    fun `payday is clamped to the end of short months`() {
        val clamped = SalaryClock(config.copy(payday = 31))
        assertEquals(LocalDate.of(2026, 2, 28), clamped.paydayOf(YearMonth.of(2026, 2)))
        assertEquals(LocalDate.of(2028, 2, 29), clamped.paydayOf(YearMonth.of(2028, 2)))
    }

    @Test
    fun `payday countdown is zero on payday`() {
        assertEquals(0L, clock.daysToPayday(LocalDate.of(2026, 10, 15)))
        assertEquals(5L, clock.daysToPayday(LocalDate.of(2026, 10, 10)))
        assertEquals(30L, clock.daysToPayday(LocalDate.of(2026, 10, 16)))
    }

    @Test
    fun `accruing period spans previous payday to next payday`() {
        val period = clock.accruingPeriod(LocalDateTime.of(2026, 10, 5, 10, 0))
        assertEquals(LocalDate.of(2026, 9, 16), period.start)
        assertEquals(LocalDate.of(2026, 10, 15), period.end)

        val afterPayday = clock.accruingPeriod(LocalDateTime.of(2026, 10, 15, 20, 0))
        assertEquals(LocalDate.of(2026, 10, 16), afterPayday.start)
        assertEquals(LocalDate.of(2026, 11, 15), afterPayday.end)
    }

    @Test
    fun `due periods are returned in ascending order`() {
        val periods = clock.duePeriods(
            lastSettledEnd = LocalDate.of(2026, 8, 15),
            now = LocalDateTime.of(2026, 10, 16, 9, 0),
        )

        assertEquals(2, periods.size)
        assertEquals(LocalDate.of(2026, 9, 15), periods[0].end)
        assertEquals(LocalDate.of(2026, 10, 15), periods[1].end)
        assertEquals(LocalDate.of(2026, 8, 16), periods[0].start)
    }

    @Test
    fun `first run settles only the latest period`() {
        val periods = clock.duePeriods(
            lastSettledEnd = null,
            now = LocalDateTime.of(2026, 10, 16, 9, 0),
        )
        assertEquals(1, periods.size)
        assertEquals(LocalDate.of(2026, 10, 15), periods.single().end)
    }

    @Test
    fun `nothing is due before the first payday of use`() {
        val periods = clock.duePeriods(
            lastSettledEnd = LocalDate.of(2026, 10, 15),
            now = LocalDateTime.of(2026, 10, 20, 9, 0),
        )
        assertTrue(periods.isEmpty())
    }

    @Test
    fun `period accrual matches virtual earnings`() {
        val period = clock.periodEndingAt(LocalDate.of(2026, 10, 15))
        val accrued = clock.accruedCentsIn(period, LocalDateTime.of(2026, 10, 15, 23, 0))
        val workDays = generateSequence(period.start) { it.plusDays(1) }
            .takeWhile { !it.isAfter(period.end) }
            .count { it.dayOfWeek !in setOf(DayOfWeek.SATURDAY, DayOfWeek.SUNDAY) }

        assertEquals(clock.dailyPayCents * workDays, accrued, 0.01)
        assertFalse(accrued <= 0)
    }
}
